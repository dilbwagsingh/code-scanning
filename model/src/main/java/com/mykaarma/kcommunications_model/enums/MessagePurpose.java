package com.mykaarma.kcommunications_model.enums;

public enum MessagePurpose {
	F("F"),                  // For drafts
	OPTIN_APP("OPTIN_APP"),          // opt-in approved
	OPTIN_REJ("OPTIN_REJ"),          // opt-in rejected
	OPTIN_REQ("OPTIN_REQ"),          // opt-in requested
	OPTIN_ACT("OPTIN_ACT"),          // opt-in accepted 
	D("D"), 		            // auto-delegation
	PDINOTIFY("PDINOTIFY"),          // PickupDelivery internal notification
	INS_NOTIFY("INS_NOTIFY"), 		// Video walkaround performed internal notification
	INSPECT_RESTORED("INSPECT_RESTORED"),   // Video walkaround restored
	TECH_INS_NOTIFY("TECH_INS_NOTIFY"), 	// Technician video walkaround performed internal notification
	VWA_REGULAR_MANUAL("VWA_REGULAR_MANUAL"), // Customer/Loaner Video walkaround manual message
	VWA_TECH_MANUAL("VWA_TECH_MANUAL"),    // Tech Video walkaround manual message
	VWA_TECH_AUTO("VWA_TECH_AUTO"),      // Tech Video walkaround auto message
	AUTORESPOND("AUTORESPOND"),		// Auto-responder
	CONSENT("CONSENT"),
	TIRE_PROFILE_INSP("TIRE_PROFILE_INSP"),  // Tire Inspection Inspection performed internal notification
	TIRE_RECOMM_APPROVAL("TIRE_RECOMM_APPROVAL"),  // Tire Recommendation approved internal notification
	W("W"),                  //Welcome Text/Email
	P("P"),                  //Customer Card Save Notification
	R("R"),
	S("S"),                  //Appointment/Inspection
	A("A"),                  //Appointment Notificaiton to Customer
	CIWNotOp("CIWNotOp"),           //CIW No Opt In
	PR("PR"),                 //Payment Request
	APR("APR"),                //Automatic payment Request
	AC("AC"),                 //Appointment Confirmation
	AU("AU"),                 //Appointment Updation
	AD("AD"),                 //Appointment Cancelation
	V("V"),                  //Payment Voided/Refunded
	TF("TF"),
	CF("CF"),
	CWM("CWM"),
	AER("AER"),                //OutOfOffice: Auto responder Email
	ATR("ATR"),                //OutOfOffice: Auto responder Text
	OOO_DELEGATION_DELEGATEE_NOTIFY("OOO_DELEGATION_DELEGATEE_NOTIFY"), //OutOfOffice delegation notification to delegatee
	DELEGATION_OOO_NOTE("DELEGATION_OOO_NOTE"), //Note in customer thread due to OoO delegation
	MANUAL_DELEGATION_NOTE("MANUAL_DELEGATION_NOTE"),
	MANUAL_DELEGATION_NOTIFICATION("MANUAL_DELEGATION_NOTIFICATION"),
	MESSAGE_SEND_FAILURE_NOTIFICATION("MESSAGE_SEND_FAILURE_NOTIFICATION"),
	UNASSIGNED_MESSAGE_NOTIFICATION("UNASSIGNED_MESSAGE_NOTIFICATION"),
	MDLCACT("MDLCACT"),            //MDL Customer Arrival, to Customer
	RECALL("RECALL"),             //Service Recalls
	PDMSG("PDMSG"),              //Pickup Dropoff, to Customer
	NE("NE"),                 
	APPNOTIFY("APPNOTIFY"),          //Appointment Scheduled
	MDLLACT("MDLLACT"),            //MDL Loaner Return, to Customer
	DRIV_CALL("DRIV_CALL"),          //Pickup and delivery call between driver and customer
	PDM_CALL("PDM_CALL"),           //Pickup and delivery call between customer and transport manager
	FDNOTIFY("FDNOTIFY"),           //Fraud Detection Notification
	CARDDELETE("CARDDELETE"),         //Card Delete Notificaiton
	WTNS("WTNS"),               //Welcome Text
	FinInviteM("FinInviteM"),         //Payment Offer to Customer
	FIN_ALERT("FIN_ALERT"),          //Finance Amount Alert
	PAY_RCPT("PAY_RCPT"),           //Payment Receipt to Customer
	REF_RCPT("REF_RCPT"),           //Refund Receipt
	SCH_TXT("SCH_TXT"),            //Schedule Service, to Customer
	CIWOptIn("CIWOptIn"),           //CIW Opt In
	MDLLRSA("MDLLRSA"),            //MDL Loaner Return, to SA
	MDLCWSA("MDLCWSA"),            //MDL Car Wash, to SA
	MDLCASA("MDLCASA"),            //MDL Customer Arrival, to SA
	PDNOTIFY("PDNOTIFY"),           //Pickup Dropoff, to SA
	CASessStrt("CASessStrt"),         //Appointment Chat Started, to SA
	PDNOTES("PDNOTES"),            //Pickup and delivery Internal Notes
	DRIV_SA("DRIV_SA"),            //Pickup and delivery call between driver and service advisor
	DRIV_PDM("DRIV_PDM"),           //Pickup and delivery call between driver and tranport manager
	ACNOT("ACNOT"),              //Appt Customer Arrival Notification
	APP_NOTIF("APP_NOTIF"),          //Internal App notificaitons
	AUTH("AUTH"),
	JAUTH("JAUTH"),
	LEAD_NOTIF("LEAD_NOTIF"),          //Sales Lead Notification
    CardSave_RCPT("CardSave_RCPT"),
	DO_VEH_OPT_ALERT("DO_VEH_OPT_ALERT"),
    OPT_OUT("OPT_OUT"),             
    REF_REQ("REF_REQ"),			 // refund required
    MPI_AUTO("MPI_AUTO"),			 // Auto MPI message for green inspection
    MPI_NOTIF("MPI_NOTIF"),          // MPI notifications
    LEAD_NOTIF_MISSED("LEAD_NOTIF_MISSED"),   // Missed Lead Notifications   
    WFR("WFR"),
	CUSTOMER_SENTIMENT_STATUS("CUSTOMER_SENTIMENT_STATUS"),
	BDC_CALL_FORWARD("BDC_CALL_FORWARD"),    // BDC Call forwarding
	FOLLOWUP("FOLLOWUP"),			 //	Follow up message
	THREE_D_S_APPROVAL("THREE_D_S_APPROVAL"),
	THREE_D_S_APPROVAL_UNDO("THREE_D_S_APPROVAL_UNDO"),
	SIGNED_INVOICE_NOTIFICATION("SIGNED_INVOICE_NOTIFICATION"),
	INCOMING_MESSAGE("INCOMING_MESSAGE"),
    COMMUNICATION_PREFERENCE_CHANGE_NOTIFICATION("COMMUNICATION_PREFERENCE_CHANGE_NOTIFICATION"),
	UBER_RIDE_COMPLETION("UBER_RIDE_COMPLETION"),
	UBER_RIDE_CANCELLATION("UBER_RIDE_CANCELLATION"),
	SHARED_RIDE_MESSAGE("SHARED_RIDE_MESSAGE"),
	THREAD_OWNERSHIP_CHANGED_NOTIFICATION("THREAD_OWNERSHIP_CHANGED_NOTIFICATION"),
	USER_ONBOARDING_INVITE("USER_ONBOARDING_INVITE"), //Outgoing messages for user invites using third part twilio number or user onboarding email id
	OUT_OF_OFFICE_TURN_OFF_REMINDER("OOO:TO"),
	RETRY_AUTORESPONDER("RETRY_AUTORESPONDER")
	;
	
	 private String messagePurpose;

		private MessagePurpose(String messagePurpose) {
			this.messagePurpose = messagePurpose;
		}

		public String getMessagePurpose() {
			return this.messagePurpose;
		}

		public static MessagePurpose getMessagePurposeForString(String messagePurposeStr)
		{
			MessagePurpose messagePurpose = null;
			if(messagePurposeStr != null) {
				for(MessagePurpose type : MessagePurpose.values()) {
					if(type.getMessagePurpose().equalsIgnoreCase(messagePurposeStr.trim())) {
						messagePurpose = type;
						break;
					}
				}
			}

			return messagePurpose;
		}
}
