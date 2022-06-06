package com.mykaarma.kcommunications.utils;

public enum RabbitQueueInfo {

	MESSAGE_SENDING_QUEUE("mykaarma.communications.api.message.send", "mykaarma.communications.api", "mykaarma.communications.api.message.send.key"),
	MULTIPLE_MESSAGE_SENDING_QUEUE("mykaarma.communications.api.multiple.message.send", "mykaarma.communications.api", "mykaarma.communications.api.multiple.message.send.key"),
	MESSAGE_SAVING_QUEUE("mykaarma.communications.api.message.save", "mykaarma.communications.api", "mykaarma.communications.api.message.save.key"),
	POST_MESSAGE_SENDING_QUEUE("mykaarma.communications.api.post.message.send", "mykaarma.communications.api", "mykaarma.communications.api.post.message.send.key"),
	EVENT_PROCESSING_QUEUE("mykaarma.communications.event.processing","mykaarma.communications.api","mykaarma.communications.event.processing.key"),
	UPDATE_RECORDING_URL_FOR_DEALER("mykaarma.communications.api.dealer.update.recording.url","mykaarma.communications.api","mykaarma.communications.api.dealer.update.recording.url.key"),
	UPDATE_RECORDING_URL_FOR_MESSAGE("mykaarma.communications.api.message.update.recording.url","mykaarma.communications.api","mykaarma.communications.api.message.update.recording.url.key"),
	UPDATE_RECORDING_URL_FOR_MESSAGE_DELAYED("mykaarma.communications.api.message.update.recording.url.delayed","mykaarma.communications.api","mykaarma.communications.api.message.update.recording.url.delayed.key"),
	POST_MESSAGE_RECEIVED_QUEUE("mykaarma.communications.post.message.received","mykaarma.communications.api","mykaarma.communications.post.message.received.key"),
	MAILT_CUSTOMER_THREAD("mykaarma.communications.api.mail.customer.thread", "mykaarma.communications.api", "mykaarma.communications.api.mail.customer.thread.key"),
	DELAYED_FILTER_UPDATE("mykaarma.delayed.filter.update", "mykaarma.delayed.filter.update.exchange", "mykaarma.delayed.filter.update.key"),
	SUBSCRIPTION_DEALER_UPDATE("mykaarma.communications.api.subscription.update.dealer", "mykaarma.communications.api", "mykaarma.communications.api.subscription.update.dealer.key"),
	SUBSCRIPTION_CUSTOMER_UPDATE("mykaarma.communications.api.subscription.update.customer", "mykaarma.communications.api", "mykaarma.communications.api.subscription.update.customer.key"),
	PREFERRED_COMMUNICATION_MODE_PREDICT("mykaarma.communications.api.preferred.communication.mode.predict", "mykaarma.communications.api", "mykaarma.communications.api.preferred.communication.mode.predict.key"),
	VERIIFY_COMMUNICATIONS("mykaarma.communications.scheduler.verification", "mykaarma.communications.scheduler.exchange", "mykaarma.communications.scheduler.verification.key"),
	OPT_OUT_STATUS_UPDATE("mykaarma.communications.optoutstatus.update", "mykaarma.communications.optoutstatus.exchange", "mykaarma.communications.optoutstatus.update.key"),
	POST_OPT_OUT_STATUS_UPDATE("mykaarma.communications.optoutstatus.post.update", "mykaarma.communications.optoutstatus.exchange", "mykaarma.communications.optoutstatus.post.update.key"),
	DOUBLE_OPTIN_DEPLOYMENT("mykaarma.communications.optoutstatus.doubleoptin.deploy", "mykaarma.communications.optoutstatus.exchange", "mykaarma.communications.optoutstatus.doubleoptin.deploy.key"),
	POST_INCOMING_MESSAGE_SAVE_QUEUE("mykaarma.communications.api.post.incoming.message.save", "mykaarma.communications.api", "mykaarma.communications.api.post.incoming.message.save.key"),
	OPTIN_AWAITING_MESSAGE_EXPIRE_QUEUE("mykaarma.communications.api.optin.awaiting.message.expire", "mykaarma.communications.api", "mykaarma.communications.api.optin.awaiting.message.expire.key"),
	SAVE_HISTORICAL_MESSAGES("mykaarma.communications.historical.message.save", "mykaarma.communications.api", "mykaarma.communications.historical.message.save.key"),
    TEMPLATE_INDEXING_QUEUE("mykaarma.communications.template.index", "mykaarma.communications.api", "mykaarma.communications.template.index.key"),
	POST_UNIVERSAL_MESSAGE_SEND_QUEUE("mykaarma.communications.api.post.universal.message.send", "mykaarma.communications.api", "mykaarma.communications.api.post.universal.message.send.key"),
	MESSAGE_WITHOUT_CUSTOMER_SENDING_QUEUE("mykaarma.communications.api.message.without.customer.send", "mykaarma.communications.api", "mykaarma.communications.api.message.without.customer.send.key"),
	POST_INCOMING_BOT_MESSAGE_SAVE_QUEUE("mykaarma.communications.api.post.incoming.bot.message.save", "mykaarma.communications.api", "mykaarma.communications.api.post.incoming.bot.message.save.key"),
	;

	private String queueName;
	private String exchangeName;
	private String queueKey;

	private RabbitQueueInfo(String queueName, String exchangeName, String queueKey) {
		this.queueName = queueName;
		this.exchangeName = exchangeName;
		this.queueKey = queueKey;
	}

	public String getQueueName() {
		return queueName;
	}

	public void setQueueName(String queueName) {
		this.queueName = queueName;
	}

	public String getExchangeName() {
		return exchangeName;
	}

	public void setExchangeName(String exchangeName) {
		this.exchangeName = exchangeName;
	}

	public String getQueueKey() {
		return queueKey;
	}

	public void setQueueKey(String queueKey) {
		this.queueKey = queueKey;
	}

}
