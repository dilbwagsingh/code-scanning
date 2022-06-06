package com.mykaarma.kcommunications_model.enums;

public enum ApiScope {
	

	CREATE_MESSAGE("message.create"),
	FETCH_MESSAGE("communications.message.get"),
	CREATE_CALL("call.create"),
	COMMUNICATIONS_WAITING_FOR_RESPONSE_READ("communications.waitingforresponse.read"),
	
	CANCEL_CALL("call.cancel"),
	UPDATE_MESSAGE("message.update"),
	UPDATE_WFR_FEEDBACK("wfr.feedback.update"),
	
	UPDATE_CUSTOMER_SENTIMENT("customer.sentiment.update"),
	UPDATE_MESSAGE_PREDICTION("message.prediction.update"),
	
	DELETE_NOTIFIER_ENTRIES("delete.notifier.entries"),
	COMMUNICATIONS_RATE_CONTROL("communications.rate.control"),
	COMMUNICATIONS_POST_MESSAGE_SENT("communications.post.message.sent"),
	COMMUNICATIONS_POST_MESSAGE_RECEIVED("communications.post.message.received"),
	COMMUNICATIONS_DEFAULT_THREAD_OWNER_READ("communications.default.thread.owner.read"),
	COMMUNICATIONS_OPT_OUT("communications.optout"),
	COMMUNICATIONS_VOICECREDENTIAL_READ("communications.voicecredential.read"),
	CUST_CONV_GET("messaging.customer.conversation.get"),
	DELETE_SUBSCRIPTIONS("communications.delete.subscriptions"),
	COMMUNICATIONS_REDACT_MESSAGE("communications.message.redact"),
	COMMUNICATIONS_ATTACHMENTS_WRITE("communications.attachments.write"),
	COMMUNICATIONS_ATTACHMENTS_DELETE("communications.attachments.delete"),
	COMMUNICATIONS_SEND_EMAIL_WITHOUT_CUSTOMER("communications.send.email.without.customer"),
    COMMUNICATIONS_PREFERRED_COMMUNICATION_MODE_WRITE("communications.preferred.communication.mode.write"),
    COMMUNICATIONS_PREFERRED_COMMUNICATION_MODE_READ("communications.preferred.communication.mode.read"),
	COMMUNICATIONS_VERIFY_BILLING("communications.billing.verify"),
	COMMUNICATIONS_SEND_NOTIFICATION_WITHOUT_CUSTOMER("communications.notification.without.customer.send"),
	COMMUNICATIONS_FILE_UPLOAD("communications.file.upload"),
	COMMUNICATIONS_FILE_DELETE("communications.file.delete"),
	COMMUNICATIONS_OPT_OUT_READ("communications.optout.read"),
	COMMUNICATIONS_OPT_OUT_WRITE("communications.optout.write"),
	COMMUNICATIONS_OPT_OUT_DEPLOY("communications.optout.deploy"),
	COMMUNICATIONS_TEMPLATE_INDEX("communications.template.index"),
	COMMUNICATIONS_TRANSLATE("communications.translate"),
	COMMUNICATIONS_TEMPLATE_SEARCH("communications.template.search"),
	COMMUNICATIONS_CUSTOMER_LOCK_READ("communications.customer.lock.read"),
	LOG_AUTO_CSI("autocsi.log"),
	COMMUNICATIONS_SEND_MESSAGE_WITHOUT_CUSTOMER("communications.send.message.without.customer"),
	COMMUNICATIONS_CREATE_FREEMARKER_TEMPLATE("communications.create.freemarker.template"),
	COMMUNICATIONS_THREAD_READ("communications.thread.read"),
	COMMUNICATIONS_FORWARDED_MESSAGE_WRITE("communications.forwarded.message.write"),
	COMMUNICATIONS_BOT_MESSAGE_WRITE("communications.bot.message.write");

	private final String apiScopeName;
	
	private ApiScope(String apiScopeName) {
		this.apiScopeName = apiScopeName;
	}

	public String getApiScopeName() {
		return this.apiScopeName;
	}
}
