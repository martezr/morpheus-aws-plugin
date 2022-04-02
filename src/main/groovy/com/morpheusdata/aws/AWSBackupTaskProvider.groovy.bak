package com.morpheusdata.task

import com.morpheusdata.core.*
import com.morpheusdata.model.OptionType
import com.morpheusdata.model.TaskType

class SlackTaskProvider implements TaskProvider {
	MorpheusContext morpheusContext
	Plugin plugin
	AbstractTaskService service

	SlackTaskProvider(Plugin plugin, MorpheusContext morpheusContext) {
		this.plugin = plugin
		this.morpheusContext = morpheusContext
	}

	@Override
	MorpheusContext getMorpheus() {
		return morpheusContext
	}

	@Override
	Plugin getPlugin() {
		return plugin
	}

	@Override
	ExecutableTaskInterface getService() {
		return new SlackTaskService(plugin,morpheus)
	}

	@Override
	String getCode() {
		return "slackTask"
	}

	@Override
	TaskType.TaskScope getScope() {
		return TaskType.TaskScope.all
	}

	@Override
	String getName() {
		return 'Slack Message'
	}

	@Override
	String getDescription() {
		return 'A custom task to send a slack message'
	}

	@Override
	Boolean isAllowExecuteLocal() {
		return true
	}

	@Override
	Boolean isAllowExecuteRemote() {
		return false
	}

	@Override
	Boolean isAllowExecuteResource() {
		return false
	}

	@Override
	Boolean isAllowLocalRepo() {
		return false
	}

	@Override
	Boolean isAllowRemoteKeyAuth() {
		return false
	}

	@Override
	List<OptionType> getOptionTypes() {
		OptionType slackChannel = new OptionType(
				name: 'slackChannel',
				code: 'slackChannel',
				fieldName: 'slackChannel',
				optionSource: false,
				displayOrder: 0,
				fieldLabel: 'Channel',
				required: true,
				inputType: OptionType.InputType.TEXT
		)
		OptionType slackMessage = new OptionType(
				name: 'slackMessage',
				code: 'slackMessage',
				fieldName: 'slackMessage',
				optionSource: false,
				displayOrder: 1,
				fieldLabel: 'Message',
				required: true,
				inputType: OptionType.InputType.CODE_EDITOR
		)
		return [slackChannel, slackMessage]
	}
}