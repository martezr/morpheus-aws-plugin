package com.morpheusdata.aws

import com.morpheusdata.core.Plugin
import com.morpheusdata.model.Permission
import com.morpheusdata.model.OptionType
import com.morpheusdata.views.HandlebarsRenderer

class IntegrationPlugin extends Plugin {

	@Override
	String getCode() {
		return 'aws-integration'
	}

	@Override
	void initialize() {
		AWSBackupTabProvider awsBackupTabProvider = new AWSBackupTabProvider(this, morpheus)
		this.pluginProviders.put(awsBackupTabProvider.code, awsBackupTabProvider)
		this.setRenderer(new HandlebarsRenderer(this.classLoader))
		this.controllers.add(new AWSBackupTabController(this, morpheus))
		this.setName("AWS Integration")
		this.setDescription("AWS integration plugin")
		this.settings << new OptionType(
			name: 'AWS Access Key',
			code: 'aws-access-key',
			fieldName: 'accessKey',
			displayOrder: 0,
			fieldLabel: 'Access Key',
			helpText: 'The AWS access key',
			required: true,
			inputType: OptionType.InputType.PASSWORD
		)

		this.settings << new OptionType(
			name: 'AWS Secret Key',
			code: 'aws-secret-key',
			fieldName: 'secretKey',
			displayOrder: 1,
			fieldLabel: 'Secret Key',
			helpText: 'The AWS Secret key',
			required: true,
			inputType: OptionType.InputType.PASSWORD
		)
	}

	@Override
	void onDestroy() {
	}

	@Override
	public List<Permission> getPermissions() {
		Permission permission = new Permission('AWS Integration', 'awsIntegrationPlugin', [Permission.AccessType.full])
		return [permission];
	}
}