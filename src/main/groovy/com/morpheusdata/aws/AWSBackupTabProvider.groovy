package com.morpheusdata.aws

import com.morpheusdata.core.AbstractInstanceTabProvider
import com.morpheusdata.core.MorpheusContext
import com.morpheusdata.core.Plugin
import com.morpheusdata.model.Account
import com.morpheusdata.model.Instance
import com.morpheusdata.model.TaskConfig
import com.morpheusdata.model.ContentSecurityPolicy
import com.morpheusdata.model.User
import com.morpheusdata.views.HTMLResponse
import com.morpheusdata.views.ViewModel
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import io.reactivex.Single
import software.amazon.awssdk.services.backup.*
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;

class AWSBackupTabProvider extends AbstractInstanceTabProvider {
	Plugin plugin
	MorpheusContext morpheus

	String code = 'aws-backup-tab'
	String name = 'AWS Backups'

	AWSBackupTabProvider(Plugin plugin, MorpheusContext context) {
		this.plugin = plugin
		this.morpheus = context
	}

	@Override
	HTMLResponse renderTemplate(Instance instance) {

		// Instantiate an object for storing data
		// passed to the html template
		ViewModel<Instance> model = new ViewModel<>()
		
		// Retrieve additional details about the instance
        // https://developer.morpheusdata.com/api/com/morpheusdata/model/TaskConfig.InstanceConfig.html
		TaskConfig instanceDetails = morpheus.buildInstanceConfig(instance, [:], null, [], [:]).blockingGet()
		println "External ID: ${instanceDetails.instance.containers[0].server.externalId}"
		// Define an object for storing the data retrieved
		// from the DataDog REST API
		def HashMap<String, String> awsBackupPayload = new HashMap<String, String>();

        // Retrieve plugin settings
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
		def jobs = backupClient.listBackupJobs()
		println "Backup jobs: ${jobs.backupJobs()[0].stateAsString()}"

		Long successfulJobs = 0
		Long failedJobs = 0
		Long runningJobs = 0
		Long totalJobs = 0

		def dataOut = []
		jobs.backupJobs().eachWithIndex{it,index->
			if(index < 5){
				def reportMap = [:]
				reportMap['id'] = it.backupJobId()
				reportMap['start'] = it.creationDate()
				reportMap['end'] = it.completionDate()
				reportMap['percentage'] = it.percentDone()
				reportMap['state'] = it.stateAsString()
				reportMap['vaultName'] = it.backupVaultName()
				reportMap['size'] = it.backupSizeInBytes()
				dataOut << reportMap
			}
			switch(it.stateAsString()) {            
				case "COMPLETED": 
					totalJobs++
					successfulJobs++ 
					break; 
				case "FAILED ": 
					totalJobs++
					failedJobs++ 
					break; 
				case "RUNNING": 
					totalJobs++
					runningJobs++ 
					break; 
				case "PENDING": 
					totalJobs++
					runningJobs++
					break; 
				default: 
					totalJobs++
					break; 
			}
		}
		println dataOut
		awsBackupPayload.put("totalJobs", totalJobs)
		awsBackupPayload.put("runningJobs", runningJobs)
		awsBackupPayload.put("failedJobs", failedJobs)
		awsBackupPayload.put("successfulJobs", successfulJobs)
		awsBackupPayload.put("jobs", dataOut)

	    def webnonce = morpheus.getWebRequest().getNonceToken()
		awsBackupPayload.put("webnonce",webnonce)

		// Set the value of the model object to the HashMap object
		model.object = awsBackupPayload
		getRenderer().renderTemplate("hbs/instanceTab", model)
	}


	// This method contains the logic for when the tab
	// should be displayed in the UI
	@Override
	Boolean show(Instance instance, User user, Account account) {
		// Set the tab to not be shown be default
		return true
	}
	/**
	 * Allows various sources used in the template to be loaded
	 * @return
	 */
	@Override
	ContentSecurityPolicy getContentSecurityPolicy() {
		def csp = new ContentSecurityPolicy()
		csp
	}
}