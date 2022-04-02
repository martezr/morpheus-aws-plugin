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

	/**
	 * Defines two Routes with the builder method
	 * @return
	 */
	List<Route> getRoutes() {
		[
			Route.build("/awsBackup/example", "example", [Permission.build("awsIntegrationPlugin", "full")]),
			Route.build("/awsBackup/json", "json", [Permission.build("awsIntegrationPlugin", "full")]),
			Route.build("/awsBackup/backupVaults", "listBackupVaults", [Permission.build("awsIntegrationPlugin", "full")]),
			Route.build("/awsBackup/createBackup", "createBackup", [Permission.build("awsIntegrationPlugin", "full")]),
			Route.build("/awsBackup/listRecoveryPoints", "listRecoveryPoints", [Permission.build("awsIntegrationPlugin", "full")])
		]
	}

	/**
	 * As defined in {@link #getRoutes}, Method will be invoked when /reverseTask/example is requested
	 * @param model
	 * @return a simple html response
	 */
	def example(ViewModel<String> model) {
		println model
		println "user: ${model.user}"
		return HTMLResponse.success("foo: ${model.user.firstName} ${model.user.lastName}")
	}

	/**
	 * As defined in {@link #getRoutes}, Method will be invoked when /reverseTask/json is requested
	 * @param model
	 * @return a simple json response
	 */
	def json(ViewModel<Map> model) {
		println model
		model.object.foo = "fizz"
		return JsonResponse.of(model.object)
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
		println "Backup jobs: ${vaults.backupVaultList()}"

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
		println "Recovery points: ${recoveryPoints}"

		//def dataOut = []
		//vaults.backupVaultList().each{
		//	dataOut << it.backupVaultName()
		//}
        model.object.recoveryPoints = recoveryPoints
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
}