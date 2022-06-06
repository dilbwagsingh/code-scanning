package com.mykaarma.kcommunications.model.kre;

public class RoutingRuleResponse implements java.io.Serializable{

	static final long serialVersionUID = 1L;

	   private java.lang.Long dealerID;
	   private java.lang.Long customerID;
	   private java.lang.Long dealerDepartmentID;
	   private java.lang.Long dealerAssociateID;
	   private java.lang.Long threadID;
	   private java.lang.String forwardingEmailGatewayType;
	   private java.lang.String daTextReplyTemplate;
	   private java.lang.Boolean isStop = false;
	   private java.util.List<java.lang.Long> forwardingDAIDList;
	   private java.lang.Boolean forwardToEmail = false;

	   private java.util.List<java.lang.String> forwardingEmailIDList;

	   private java.lang.Boolean concatenateLongTexts = false;

	   private java.lang.Boolean forwardText = false;

	   private java.lang.Boolean isMessageFromDA = false;

	   private java.lang.Long senderDAID;

	   private java.util.List<java.lang.String> connectingNumbersList;

	   private java.util.List<java.lang.Long> connectingDAsList;

	   private java.lang.String dealerDepartmentName;

	   public RoutingRuleResponse()
	   {
	   }

	   public java.lang.Long getDealerID()
	   {
	      return this.dealerID;
	   }

	   public void setDealerID(java.lang.Long dealerID)
	   {
	      this.dealerID = dealerID;
	   }

	   public java.lang.Long getCustomerID()
	   {
	      return this.customerID;
	   }

	   public void setCustomerID(java.lang.Long customerID)
	   {
	      this.customerID = customerID;
	   }

	   public java.lang.Long getDealerDepartmentID()
	   {
	      return this.dealerDepartmentID;
	   }

	   public void setDealerDepartmentID(java.lang.Long dealerDepartmentID)
	   {
	      this.dealerDepartmentID = dealerDepartmentID;
	   }

	   public java.lang.Long getDealerAssociateID()
	   {
	      return this.dealerAssociateID;
	   }

	   public void setDealerAssociateID(java.lang.Long dealerAssociateID)
	   {
	      this.dealerAssociateID = dealerAssociateID;
	   }

	   public java.lang.Long getThreadID()
	   {
	      return this.threadID;
	   }

	   public void setThreadID(java.lang.Long threadID)
	   {
	      this.threadID = threadID;
	   }

	   public java.lang.String getForwardingEmailGatewayType()
	   {
	      return this.forwardingEmailGatewayType;
	   }

	   public void setForwardingEmailGatewayType(
	         java.lang.String forwardingEmailGatewayType)
	   {
	      this.forwardingEmailGatewayType = forwardingEmailGatewayType;
	   }

	   public java.lang.String getDaTextReplyTemplate()
	   {
	      return this.daTextReplyTemplate;
	   }

	   public void setDaTextReplyTemplate(java.lang.String daTextReplyTemplate)
	   {
	      this.daTextReplyTemplate = daTextReplyTemplate;
	   }

	   public java.lang.Boolean getIsStop()
	   {
	      return this.isStop;
	   }

	   public void setIsStop(java.lang.Boolean isStop)
	   {
	      this.isStop = isStop;
	   }

	   public java.util.List<java.lang.Long> getForwardingDAIDList()
	   {
	      return this.forwardingDAIDList;
	   }

	   public void setForwardingDAIDList(
	         java.util.List<java.lang.Long> forwardingDAIDList)
	   {
	      this.forwardingDAIDList = forwardingDAIDList;
	   }

	   public java.lang.Boolean getForwardToEmail()
	   {
	      return this.forwardToEmail;
	   }

	   public void setForwardToEmail(java.lang.Boolean forwardToEmail)
	   {
	      this.forwardToEmail = forwardToEmail;
	   }

	   public java.util.List<java.lang.String> getForwardingEmailIDList()
	   {
	      return this.forwardingEmailIDList;
	   }

	   public void setForwardingEmailIDList(
	         java.util.List<java.lang.String> forwardingEmailIDList)
	   {
	      this.forwardingEmailIDList = forwardingEmailIDList;
	   }

	   public java.lang.Boolean getConcatenateLongTexts()
	   {
	      return this.concatenateLongTexts;
	   }

	   public void setConcatenateLongTexts(java.lang.Boolean concatenateLongTexts)
	   {
	      this.concatenateLongTexts = concatenateLongTexts;
	   }

	   public java.lang.Boolean getForwardText()
	   {
	      return this.forwardText;
	   }

	   public void setForwardText(java.lang.Boolean forwardText)
	   {
	      this.forwardText = forwardText;
	   }

	   public java.lang.Boolean getIsMessageFromDA()
	   {
	      return this.isMessageFromDA;
	   }

	   public void setIsMessageFromDA(java.lang.Boolean isMessageFromDA)
	   {
	      this.isMessageFromDA = isMessageFromDA;
	   }

	   public java.lang.Long getSenderDAID()
	   {
	      return this.senderDAID;
	   }

	   public void setSenderDAID(java.lang.Long senderDAID)
	   {
	      this.senderDAID = senderDAID;
	   }

	   public java.util.List<java.lang.String> getConnectingNumbersList()
	   {
	      return this.connectingNumbersList;
	   }

	   public void setConnectingNumbersList(
	         java.util.List<java.lang.String> connectingNumbersList)
	   {
	      this.connectingNumbersList = connectingNumbersList;
	   }

	   public java.util.List<java.lang.Long> getConnectingDAsList()
	   {
	      return this.connectingDAsList;
	   }

	   public void setConnectingDAsList(
	         java.util.List<java.lang.Long> connectingDAsList)
	   {
	      this.connectingDAsList = connectingDAsList;
	   }

	   public java.lang.String getDealerDepartmentName()
	   {
	      return this.dealerDepartmentName;
	   }

	   public void setDealerDepartmentName(java.lang.String dealerDepartmentName)
	   {
	      this.dealerDepartmentName = dealerDepartmentName;
	   }

	   public RoutingRuleResponse(java.lang.Long dealerID, java.lang.Long customerID,
	         java.lang.Long dealerDepartmentID, java.lang.Long dealerAssociateID,
	         java.lang.Long threadID, java.lang.String forwardingEmailGatewayType,
	         java.lang.String daTextReplyTemplate, java.lang.Boolean isStop,
	         java.util.List<java.lang.Long> forwardingDAIDList,
	         java.lang.Boolean forwardToEmail,
	         java.util.List<java.lang.String> forwardingEmailIDList,
	         java.lang.Boolean concatenateLongTexts, java.lang.Boolean forwardText,
	         java.lang.Boolean isMessageFromDA, java.lang.Long senderDAID,
	         java.util.List<java.lang.String> connectingNumbersList,
	         java.util.List<java.lang.Long> connectingDAsList,
	         java.lang.String dealerDepartmentName)
	   {
	      this.dealerID = dealerID;
	      this.customerID = customerID;
	      this.dealerDepartmentID = dealerDepartmentID;
	      this.dealerAssociateID = dealerAssociateID;
	      this.threadID = threadID;
	      this.forwardingEmailGatewayType = forwardingEmailGatewayType;
	      this.daTextReplyTemplate = daTextReplyTemplate;
	      this.isStop = isStop;
	      this.forwardingDAIDList = forwardingDAIDList;
	      this.forwardToEmail = forwardToEmail;
	      this.forwardingEmailIDList = forwardingEmailIDList;
	      this.concatenateLongTexts = concatenateLongTexts;
	      this.forwardText = forwardText;
	      this.isMessageFromDA = isMessageFromDA;
	      this.senderDAID = senderDAID;
	      this.connectingNumbersList = connectingNumbersList;
	      this.connectingDAsList = connectingDAsList;
	      this.dealerDepartmentName = dealerDepartmentName;
	   }

}
