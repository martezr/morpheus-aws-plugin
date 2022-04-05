package com.morpheusdata.aws

import com.morpheusdata.core.AbstractReportProvider
import com.morpheusdata.core.MorpheusContext
import com.morpheusdata.core.Plugin
import com.morpheusdata.model.OptionType
import com.morpheusdata.model.ReportResult
import com.morpheusdata.model.ReportType
import com.morpheusdata.model.ReportResultRow
import com.morpheusdata.model.ContentSecurityPolicy
import com.morpheusdata.views.HTMLResponse
import com.morpheusdata.views.ViewModel
import com.morpheusdata.response.ServiceResponse
import groovy.util.logging.Slf4j
import io.reactivex.Observable;
import groovy.json.*
import software.amazon.awssdk.services.backup.BackupClient
import software.amazon.awssdk.services.backup.model.Lifecycle
import software.amazon.awssdk.services.backup.model.StartBackupJobRequest
import software.amazon.awssdk.services.backup.model.StartRestoreJobRequest
import software.amazon.awssdk.services.backup.model.ListCopyJobsResponse
import software.amazon.awssdk.services.backup.model.ListCopyJobsRequest
import software.amazon.awssdk.services.backup.model.ListRecoveryPointsByResourceRequest
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;

@Slf4j
class AWSBackupReportProvider extends AbstractReportProvider {
	Plugin plugin
	MorpheusContext morpheusContext

	AWSBackupReportProvider(Plugin plugin, MorpheusContext context) {
		this.plugin = plugin
		this.morpheusContext = context
	}

	@Override
	MorpheusContext getMorpheus() {
		morpheusContext
	}

	@Override
	Plugin getPlugin() {
		plugin
	}

	@Override
	String getCode() {
		'aws-backup-report'
	}

	@Override
	String getName() {
		'AWS Backup'
	}

	 ServiceResponse validateOptions(Map opts) {
		 return ServiceResponse.success()
	 }


	@Override
	HTMLResponse renderTemplate(ReportResult reportResult, Map<String, List<ReportResultRow>> reportRowsBySection) {
		ViewModel<String> model = new ViewModel<String>()
		//def HashMap<String, String> reportPayload = new HashMap<String, String>();
		def webnonce = morpheus.getWebRequest().getNonceToken()
		//reportPayload.put("webnonce",webnonce)
		//reportPayload.put("reportdata",reportRowsBySection)
		model.object = reportRowsBySection
		//model.object = reportPayload
		println "Report data: ${model.object}"
		getRenderer().renderTemplate("hbs/awsBackupReport", model)
	}


	@Override
	ContentSecurityPolicy getContentSecurityPolicy() {
		def csp = new ContentSecurityPolicy()
		csp
	}


	void process(ReportResult reportResult) {
		// Update the status of the report (generating) - https://developer.morpheusdata.com/api/com/morpheusdata/model/ReportResult.Status.html
		morpheus.report.updateReportResultStatus(reportResult,ReportResult.Status.generating).blockingGet();
		Long displayOrder = 0

		def settings = morpheus.getSettings(plugin)
		def settingsOutput = ""
		settings.subscribe(
			{ outData -> 
                 settingsOutput = outData
        	},
        	{ error ->
                 println error.printStackTrace()
        	}
		)

		// Parse the plugin settings payload. The settings will be available as
		// settingsJson.$optionTypeFieldName i.e. - settingsJson.ddApiKey to retrieve the DataDog API key setting
		JsonSlurper slurper = new JsonSlurper()
		def settingsJson = slurper.parseText(settingsOutput)

		SdkHttpClient httpClient = ApacheHttpClient.builder().build();
		Region region = Region.US_EAST_1;
		AwsBasicCredentials awsCreds = AwsBasicCredentials.create(
			settingsJson.accessKey,
			settingsJson.secretKey);
		BackupClient backupClient = BackupClient.builder().region(region).httpClient(httpClient).credentialsProvider(StaticCredentialsProvider.create(awsCreds)).build();
		
		// Backup Vaults
		def vaults = backupClient.listBackupVaults()
		println "Vaults: ${vaults}"

		// Backup Plans
		def plans = backupClient.listBackupPlans()
		println "Plan: ${plans}"

		// Backup Jobs
		def backupJobs = backupClient.listBackupJobs()
		def backupJobCount = backupJobs.backupJobs().size()
		println "Backup Jobs: ${backupJobs}"

		// Copy Jobs
		//def copyJobs = backupClient.listCopyJobs()
		//def copyJobCount = copyJobs.copyJobs().size()
		def copyJobCount = 4
		println "Copy Jobs: ${copyJobCount}"

		// Restore Jobs
		def restoreJobs = backupClient.listRestoreJobs()
		def restoreJobCount = restoreJobs.restoreJobs().size()
		println "Restore Jobs: ${restoreJobs}"

		def totalJobs = backupJobCount + copyJobCount + restoreJobCount

		Map<String,Object> data = [totalJobs: totalJobs, backupVaults: vaults.backupVaultList().size(), backupPlans: plans.backupPlansList().size(), copyJobs: copyJobCount, restoreJobs: restoreJobCount, backupJobs: backupJobCount]
		println "Data: ${data}"

		ReportResultRow resultRowRecord = new ReportResultRow(section: ReportResultRow.SECTION_HEADER, displayOrder: displayOrder++, dataMap: data)
        morpheus.report.appendResultRows(reportResult,[resultRowRecord]).blockingGet()
		morpheus.report.updateReportResultStatus(reportResult,ReportResult.Status.ready).blockingGet();
	}

	// https://developer.morpheusdata.com/api/com/morpheusdata/core/ReportProvider.html#method.summary
	// The description associated with the custom report
	 @Override
	 String getDescription() {
		 return "AWS Backup"
	 }

	// The category of the custom report
	 @Override
	 String getCategory() {
		 return 'inventory'
	 }

	 @Override
	 Boolean getOwnerOnly() {
		 return false
	 }

	 @Override
	 Boolean getMasterOnly() {
		 return true
	 }

	 @Override
	 Boolean getSupportsAllZoneTypes() {
		 return true
	 }

	// https://developer.morpheusdata.com/api/com/morpheusdata/model/OptionType.html
	 @Override
	 List<OptionType> getOptionTypes() {}
 }