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
import groovy.sql.GroovyRowResult
import groovy.sql.Sql
import groovy.util.logging.Slf4j
import io.reactivex.Observable;
import java.util.Date
import java.util.concurrent.TimeUnit
import groovy.json.*
import java.sql.Connection


@Slf4j
class AWSVolumeReportProvider extends AbstractReportProvider {
	Plugin plugin
	MorpheusContext morpheusContext

	AWSVolumeReportProvider(Plugin plugin, MorpheusContext context) {
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
		'aws-volume-report'
	}

	@Override
	String getName() {
		"AWS Volume Report"
	}

	 ServiceResponse validateOptions(Map opts) {
		 return ServiceResponse.success()
	 }

	@Override
	HTMLResponse renderTemplate(ReportResult reportResult, Map<String, List<ReportResultRow>> reportRowsBySection) {
		ViewModel<String> model = new ViewModel<String>()
		model.object = reportRowsBySection
		getRenderer().renderTemplate("hbs/awsVolumeReport", model)
	}

	@Override
	ContentSecurityPolicy getContentSecurityPolicy() {
		def csp = new ContentSecurityPolicy()
		csp
	}

	void process(ReportResult reportResult) {
		morpheus.report.updateReportResultStatus(reportResult,ReportResult.Status.generating).blockingGet();
		Long displayOrder = 0
		List<GroovyRowResult> results = []
		Connection dbConnection
        String attachmentStatus = "${reportResult.configMap?.attachmentStatus}"
		try {
			dbConnection = morpheus.report.getReadOnlyDatabaseConnection().blockingGet()
			results = new Sql(dbConnection).rows("SELECT id,name,type_id,status,max_storage,external_id from storage_volume WHERE type_id IN (SELECT id from storage_volume_type WHERE code LIKE 'amazon-%') AND ref_type='ComputeZone' AND status LIKE '${attachmentStatus}' order by name asc;")
		} finally {
			morpheus.report.releaseDatabaseConnection(dbConnection)
		}
		Observable<GroovyRowResult> observable = Observable.fromIterable(results) as Observable<GroovyRowResult>
		observable.map{ resultRow ->
            def sizeGb = resultRow.max_storage / (1024 * 1024 * 1024)
			Map<String,Object> data = [name: resultRow.name, id: resultRow.id, status: resultRow.status, type: resultRow.type_id, size: sizeGb + " GB"]
			ReportResultRow resultRowRecord = new ReportResultRow(section: ReportResultRow.SECTION_MAIN, displayOrder: displayOrder++, dataMap: data)
			log.info("resultRowRecord: ${resultRowRecord.dump()}")
			return resultRowRecord
		}.buffer(50).doOnComplete {
			morpheus.report.updateReportResultStatus(reportResult,ReportResult.Status.ready).blockingGet();
		}.doOnError { Throwable t ->
			morpheus.report.updateReportResultStatus(reportResult,ReportResult.Status.failed).blockingGet();
		}.subscribe {resultRows ->
			morpheus.report.appendResultRows(reportResult,resultRows).blockingGet()
		}
	}

	 @Override
	 String getDescription() {
		 return "Tech brief sample report"
	 }

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

	 @Override
	 List<OptionType> getOptionTypes() {
		  [new OptionType(code: 'aws-volume-attachment-status', name: 'AttachmentStatus',inputType: 'SELECT', optionSource: 'attachmentStatusOptions', fieldName: 'attachmentStatus', fieldContext: 'config', fieldLabel: 'Attachment Status', displayOrder: 0, required: true)]
	 }
}