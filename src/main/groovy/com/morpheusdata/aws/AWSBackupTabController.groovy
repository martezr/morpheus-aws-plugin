package com.morpheusdata.aws

import com.morpheusdata.model.Permission
import com.morpheusdata.views.JsonResponse
import com.morpheusdata.views.HTMLResponse
import com.morpheusdata.views.ViewModel
import com.morpheusdata.web.PluginController
import com.morpheusdata.web.Route
import com.morpheusdata.core.Plugin
import com.morpheusdata.core.MorpheusContext
import software.amazon.awssdk.services.backup.BackupClient
import software.amazon.awssdk.services.backup.model.Lifecycle
import software.amazon.awssdk.services.backup.model.StartBackupJobRequest
import software.amazon.awssdk.services.backup.model.StartRestoreJobRequest
import software.amazon.awssdk.services.backup.model.ListRecoveryPointsByResourceRequest
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

class AWSBackupTabController implements PluginController {

	MorpheusContext morpheusContext
	Plugin plugin

	public AWSBackupTabController(Plugin plugin, MorpheusContext morpheusContext) {
		this.plugin = plugin
		this.morpheusContext = morpheusContext
	}

	@Override
	public String getCode() {
		return 'awsBackupTabController'
	}

	@Override
	String getName() {
		return 'AWS Backup Tab Controller'
	}

	@Override
	MorpheusContext getMorpheus() {
		return morpheusContext
	}

	List<Route> getRoutes() {
		[
			Route.build("/awsBackup/backupVaults", "listBackupVaults", [Permission.build("awsIntegrationPlugin", "full")]),
			Route.build("/awsBackup/createBackup", "createBackup", [Permission.build("awsIntegrationPlugin", "full")]),
			Route.build("/awsBackup/listRecoveryPoints", "listRecoveryPoints", [Permission.build("awsIntegrationPlugin", "full")]),
			Route.build("/awsBackup/restoreBackup", "restoreBackup", [Permission.build("awsIntegrationPlugin", "full")])
		]
	}

	def listBackupVaults(ViewModel <Map> model){
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
		def vaults = backupClient.listBackupVaults()
		def dataOut = []
		vaults.backupVaultList().each{
			dataOut << it.backupVaultName()
		}
        model.object.vaults = dataOut
		return JsonResponse.of(model.object)
	}

	def listRecoveryPoints(ViewModel <Map> model){
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
		ListRecoveryPointsByResourceRequest request = ListRecoveryPointsByResourceRequest.builder().resourceArn('arn:aws:ec2:us-east-1:684882843674:instance/i-0cf6daa6b431542a8').build();
		def backupPayload = backupClient.listRecoveryPointsByResource(request)

		def recoveryPoints = backupPayload.recoveryPoints()

		def dataOut = []
		recoveryPoints.each{
			def pointDate = it.creationDate()
			def output = it.recoveryPointArn() + " - " + pointDate.toString()
			dataOut << output
		}
        model.object.recoveryPoints = dataOut
		return JsonResponse.of(model.object)
	}

	def createBackup(ViewModel <Map> model){
		Enumeration en=model.request.getParameterNames();
		while(en.hasMoreElements())
		{
			Object objOri=en.nextElement();
			String param=(String)objOri;
			String value=model.request.getParameter(param);
			println("Parameter Name is '"+param+"' and Parameter Value is '"+value+"'");
		}
		println "Request user: ${model.user}"
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

		//Lifecycle.Builder lifecyclePeriod = Lifecycle.builder().build()
		StartBackupJobRequest request = StartBackupJobRequest.builder().backupVaultName("Default").resourceArn('arn:aws:ec2:us-east-1:684882843674:instance/i-0cf6daa6b431542a8').iamRoleArn('arn:aws:iam::684882843674:role/service-role/AWSBackupDefaultServiceRole').build();
		def backupPayload = backupClient.startBackupJob(request)
        model.object.backupResponse = backupPayload.toString()
		return JsonResponse.of(model.object)
	}

	def restoreBackup(ViewModel <Map> model){
		Enumeration en=model.request.getParameterNames();
		while(en.hasMoreElements())
		{
			Object objOri=en.nextElement();
			String param=(String)objOri;
			String value=model.request.getParameter(param);
			println("Parameter Name is '"+param+"' and Parameter Value is '"+value+"'");
		}
		println "Request user: ${model.user}"
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

		//Lifecycle.Builder lifecyclePeriod = Lifecycle.builder().build()
		StartRestoreJobRequest request = StartRestoreJobRequest.builder().	recoveryPointArn('arn:aws:ec2:us-east-1::image/ami-0058b5ea1c655ffee').iamRoleArn('arn:aws:iam::684882843674:role/service-role/AWSBackupDefaultServiceRole').build();
		def restorePayload = backupClient.startRestoreJob(request)
        model.object.restoreResponse = restorePayload.toString()
		return JsonResponse.of(model.object)
	}
}