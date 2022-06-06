package com.mykaarma.kcommunications.model.kre;

public class OutOfOfficeResponse implements java.io.Serializable {

	 static final long serialVersionUID = 1L;

	   private java.lang.Boolean delegateConversation = false;
	   private java.lang.Boolean forwardText = false;
	   private java.lang.Boolean forwardEmail = false;
	   private java.lang.Boolean forwardCall = false;
	   private java.util.List<java.lang.String> textForwardingNumbers;
	   private java.util.List<java.lang.String> emailForwardingAddresses;
	   private java.util.List<java.lang.String> callForwardingNumbers;
	   private java.lang.Boolean daAutoReplyText = false;
	   private java.lang.Boolean daAutoReplyEmail = false;
	   private java.lang.Boolean daAutoReplyCall = false;
	   private java.util.List<java.lang.Long> autoReplySenderDAIDList;

	   private java.lang.Long delegateFrom;

	   private java.lang.Long delegateTo;

	   private java.lang.Boolean isActive = false;

	   private java.lang.Boolean dealerCallAutoReply = false;

	   private java.lang.Boolean dealerEmailAutoReply = false;

	   private java.lang.Boolean dealerTextAutoReply = false;

	   private java.util.List<java.lang.Long> textForwardingDAIDList;

	   private java.util.List<java.lang.Long> emailForwardingDAIDList;

	   private java.util.List<java.lang.Long> callForwardingDAIDList;

	   public OutOfOfficeResponse()
	   {
	   }

	   public java.lang.Boolean getDelegateConversation()
	   {
	      return this.delegateConversation;
	   }

	   public void setDelegateConversation(java.lang.Boolean delegateConversation)
	   {
	      this.delegateConversation = delegateConversation;
	   }

	   public java.lang.Boolean getForwardText()
	   {
	      return this.forwardText;
	   }

	   public void setForwardText(java.lang.Boolean forwardText)
	   {
	      this.forwardText = forwardText;
	   }

	   public java.lang.Boolean getForwardEmail()
	   {
	      return this.forwardEmail;
	   }

	   public void setForwardEmail(java.lang.Boolean forwardEmail)
	   {
	      this.forwardEmail = forwardEmail;
	   }

	   public java.lang.Boolean getForwardCall()
	   {
	      return this.forwardCall;
	   }

	   public void setForwardCall(java.lang.Boolean forwardCall)
	   {
	      this.forwardCall = forwardCall;
	   }

	   public java.util.List<java.lang.String> getTextForwardingNumbers()
	   {
	      return this.textForwardingNumbers;
	   }

	   public void setTextForwardingNumbers(
	         java.util.List<java.lang.String> textForwardingNumbers)
	   {
	      this.textForwardingNumbers = textForwardingNumbers;
	   }

	   public java.util.List<java.lang.String> getEmailForwardingAddresses()
	   {
	      return this.emailForwardingAddresses;
	   }

	   public void setEmailForwardingAddresses(
	         java.util.List<java.lang.String> emailForwardingAddresses)
	   {
	      this.emailForwardingAddresses = emailForwardingAddresses;
	   }

	   public java.util.List<java.lang.String> getCallForwardingNumbers()
	   {
	      return this.callForwardingNumbers;
	   }

	   public void setCallForwardingNumbers(
	         java.util.List<java.lang.String> callForwardingNumbers)
	   {
	      this.callForwardingNumbers = callForwardingNumbers;
	   }

	   public java.lang.Boolean getDaAutoReplyText()
	   {
	      return this.daAutoReplyText;
	   }

	   public void setDaAutoReplyText(java.lang.Boolean daAutoReplyText)
	   {
	      this.daAutoReplyText = daAutoReplyText;
	   }

	   public java.lang.Boolean getDaAutoReplyEmail()
	   {
	      return this.daAutoReplyEmail;
	   }

	   public void setDaAutoReplyEmail(java.lang.Boolean daAutoReplyEmail)
	   {
	      this.daAutoReplyEmail = daAutoReplyEmail;
	   }

	   public java.lang.Boolean getDaAutoReplyCall()
	   {
	      return this.daAutoReplyCall;
	   }

	   public void setDaAutoReplyCall(java.lang.Boolean daAutoReplyCall)
	   {
	      this.daAutoReplyCall = daAutoReplyCall;
	   }

	   public java.util.List<java.lang.Long> getAutoReplySenderDAIDList()
	   {
	      return this.autoReplySenderDAIDList;
	   }

	   public void setAutoReplySenderDAIDList(
	         java.util.List<java.lang.Long> autoReplySenderDAIDList)
	   {
	      this.autoReplySenderDAIDList = autoReplySenderDAIDList;
	   }

	   public java.lang.Long getDelegateFrom()
	   {
	      return this.delegateFrom;
	   }

	   public void setDelegateFrom(java.lang.Long delegateFrom)
	   {
	      this.delegateFrom = delegateFrom;
	   }

	   public java.lang.Long getDelegateTo()
	   {
	      return this.delegateTo;
	   }

	   public void setDelegateTo(java.lang.Long delegateTo)
	   {
	      this.delegateTo = delegateTo;
	   }

	   public java.lang.Boolean getIsActive()
	   {
	      return this.isActive;
	   }

	   public void setIsActive(java.lang.Boolean isActive)
	   {
	      this.isActive = isActive;
	   }

	   public java.lang.Boolean getDealerCallAutoReply()
	   {
	      return this.dealerCallAutoReply;
	   }

	   public void setDealerCallAutoReply(java.lang.Boolean dealerCallAutoReply)
	   {
	      this.dealerCallAutoReply = dealerCallAutoReply;
	   }

	   public java.lang.Boolean getDealerEmailAutoReply()
	   {
	      return this.dealerEmailAutoReply;
	   }

	   public void setDealerEmailAutoReply(java.lang.Boolean dealerEmailAutoReply)
	   {
	      this.dealerEmailAutoReply = dealerEmailAutoReply;
	   }

	   public java.lang.Boolean getDealerTextAutoReply()
	   {
	      return this.dealerTextAutoReply;
	   }

	   public void setDealerTextAutoReply(java.lang.Boolean dealerTextAutoReply)
	   {
	      this.dealerTextAutoReply = dealerTextAutoReply;
	   }

	   public java.util.List<java.lang.Long> getTextForwardingDAIDList()
	   {
	      return this.textForwardingDAIDList;
	   }

	   public void setTextForwardingDAIDList(
	         java.util.List<java.lang.Long> textForwardingDAIDList)
	   {
	      this.textForwardingDAIDList = textForwardingDAIDList;
	   }

	   public java.util.List<java.lang.Long> getEmailForwardingDAIDList()
	   {
	      return this.emailForwardingDAIDList;
	   }

	   public void setEmailForwardingDAIDList(
	         java.util.List<java.lang.Long> emailForwardingDAIDList)
	   {
	      this.emailForwardingDAIDList = emailForwardingDAIDList;
	   }

	   public java.util.List<java.lang.Long> getCallForwardingDAIDList()
	   {
	      return this.callForwardingDAIDList;
	   }

	   public void setCallForwardingDAIDList(
	         java.util.List<java.lang.Long> callForwardingDAIDList)
	   {
	      this.callForwardingDAIDList = callForwardingDAIDList;
	   }

	   public OutOfOfficeResponse(java.lang.Boolean delegateConversation,
	         java.lang.Boolean forwardText, java.lang.Boolean forwardEmail,
	         java.lang.Boolean forwardCall,
	         java.util.List<java.lang.String> textForwardingNumbers,
	         java.util.List<java.lang.String> emailForwardingAddresses,
	         java.util.List<java.lang.String> callForwardingNumbers,
	         java.lang.Boolean daAutoReplyText, java.lang.Boolean daAutoReplyEmail,
	         java.lang.Boolean daAutoReplyCall,
	         java.util.List<java.lang.Long> autoReplySenderDAIDList,
	         java.lang.Long delegateFrom, java.lang.Long delegateTo,
	         java.lang.Boolean isActive, java.lang.Boolean dealerCallAutoReply,
	         java.lang.Boolean dealerEmailAutoReply,
	         java.lang.Boolean dealerTextAutoReply,
	         java.util.List<java.lang.Long> textForwardingDAIDList,
	         java.util.List<java.lang.Long> emailForwardingDAIDList,
	         java.util.List<java.lang.Long> callForwardingDAIDList)
	   {
	      this.delegateConversation = delegateConversation;
	      this.forwardText = forwardText;
	      this.forwardEmail = forwardEmail;
	      this.forwardCall = forwardCall;
	      this.textForwardingNumbers = textForwardingNumbers;
	      this.emailForwardingAddresses = emailForwardingAddresses;
	      this.callForwardingNumbers = callForwardingNumbers;
	      this.daAutoReplyText = daAutoReplyText;
	      this.daAutoReplyEmail = daAutoReplyEmail;
	      this.daAutoReplyCall = daAutoReplyCall;
	      this.autoReplySenderDAIDList = autoReplySenderDAIDList;
	      this.delegateFrom = delegateFrom;
	      this.delegateTo = delegateTo;
	      this.isActive = isActive;
	      this.dealerCallAutoReply = dealerCallAutoReply;
	      this.dealerEmailAutoReply = dealerEmailAutoReply;
	      this.dealerTextAutoReply = dealerTextAutoReply;
	      this.textForwardingDAIDList = textForwardingDAIDList;
	      this.emailForwardingDAIDList = emailForwardingDAIDList;
	      this.callForwardingDAIDList = callForwardingDAIDList;
	   }
}
