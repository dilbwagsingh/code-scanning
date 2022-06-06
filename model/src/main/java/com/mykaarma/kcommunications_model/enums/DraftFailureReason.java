package com.mykaarma.kcommunications_model.enums;


public enum DraftFailureReason {

	MDL_EVENT_UNDER_TIME_LIMIT_REASON("mdlEventUnderTimeLimitDraftFailureReason"),
	MIN_DELAY_FOR_CEP_NOT_PASSED_REASON("minDelayForCepNotPassedDraftFailureReason"),
	UNCLOSED_RO_EXISTS_REASON("unclosedRoExistsDraftFailureReason"),
	TIME_NOT_CORRECT_FOR_SENDING_REASON("timeNotCorrectForSendingDraftFailureReason"),
	MESSAGE_NOT_ALLOWED_FOR_LABEL("messageNotAllowedForThisPhoneNumberLabel"),
	PREFERRED_COMMUNICATION_NOT_FOUND("preferredCommunicationNotFound"),
	NUMBER_OPTED_OUT("phoneNumberOptedOut"),
	NOT_OK_TO_EMAIL("notOkayToEmail"),
	FAILED_AUTO_CSI_OLD_CLOSE_DATE("failedAutoCsiOldCloseDate"),
	USER_DOES_NOT_HAVE_AUTHORITY_TEXING_MANUAL("userDoesNotHaveManualTextingAuthority"),
	USER_DOES_NOT_HAVE_AUTHORITY_TEXTING_AUTOMATIC("userDoesNotHaveAutomaticTextingAuthority"),
	OLD_INSPECTION("oldInspection"),
	DEALERSHIP_NOT_AUTHORIZED_TO_SEND_MESSAGE("dealershipNotAuthorizedToSendMessage"),
	NO_CUSTOMER_FOUND("noCustomerFound"),
	INVALID_PHONE_NUMBER("invalidPhoneNumber"),
	CUSTOMER_OPTED_OUT("customerOptedOut"),
	USER_DOES_NOT_HAVE_AUTHORITY_TO_SEND_MESSAGE("userDoesNotHaveAuthorityToSendtextMessage"),
	FAILED_TO_SEND_TEXT("failedToSendText"),
	NO_PHONE_NUMBER("noPhoneNumber"),
	GENERIC_FAILURE_REASON("genericFailureReason"),
	NO_EMAIL_ID("noEmailId"),
	CANT_SEND_EMAIL_TO_THIS_CUSTOMER("cantSendEmailToThisCustomer"),
	USER_DOES_NOT_HAVE_AUTHORITY_TO_EMAIL_MESSAGE("userDoesNotHaveAuthorityToEmailMessage");
	
	private String failureReason;
	
	private DraftFailureReason(String failureReason) {
		this.failureReason = failureReason;
	}

	public String getFailureReason() {
		return this.failureReason;
	}
	
	public static DraftFailureReason getEnumValue(String value) {
		DraftFailureReason failedDraftEnums[] = DraftFailureReason.values();
		for(DraftFailureReason failedDraftEnum: failedDraftEnums) {
			if(failedDraftEnum.getFailureReason().equalsIgnoreCase(value)) {
				return failedDraftEnum;
			}
		}
		return null;
	}
	
	public static DraftFailureReason getDraftFailureForString(String draftFailureReasonStr)
	{
		DraftFailureReason draftFailureReason = null;
		if(draftFailureReasonStr != null) {
			for(DraftFailureReason type : DraftFailureReason.values()) {
				if(type.getFailureReason().equalsIgnoreCase(draftFailureReasonStr.trim())) {
					draftFailureReason = type;
					break;
				}
			}
		}

		return draftFailureReason;
	}
}
