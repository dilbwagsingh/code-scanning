package com.mykaarma.kcommunications.controller.impl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mykaarma.global.MessageProtocol;
import com.mykaarma.global.MessagePurpose;
import com.mykaarma.kcommunications.jpa.repository.GeneralRepository;
import com.mykaarma.kcommunications.jpa.repository.MessageRepository;
import com.mykaarma.kcommunications.model.jpa.Message;
import com.mykaarma.kcommunications.mq.impl.RabbitHelper;
import com.mykaarma.kcommunications.utils.ConvertToJpaEntity;
import com.mykaarma.kcommunications.utils.Helper;
import com.mykaarma.kcommunications.utils.KManageApiHelper;
import com.mykaarma.kcommunications.utils.MessagingViewControllerHelper;
import com.mykaarma.kmanage.model.dto.json.GetDealerAssociateResponseDTO;

@Service
public class InternalCommentImpl {
	
	@Autowired
	private KManageApiHelper kManageApiHelper;
	
	@Autowired
	private Helper helper;
	
	@Autowired
	MessageRepository messageRepository;
	
	@Autowired
	private SaveMessageHelper saveMessageHelper;
	
	@Autowired
	private RabbitHelper rabbitHelper;
	
	@Autowired
	private MessagingViewControllerHelper messagingViewControllerHelper;
	
	@Autowired
	private GeneralRepository generalRepository;
	
	@Autowired
	private ConvertToJpaEntity convertToJpaEntity;
	
	private final static Logger LOGGER = LoggerFactory.getLogger(InternalCommentImpl.class);	
	

	public String publishInternalComment(String messageBody, String customerUUID, String fromUserUUID, String departmentUUID,
			MessageProtocol messageProtocol, MessagePurpose messagePurpose, List<Long> newExternalSubscribers,
			List<Long> newInternalSubscribers, List<Long> oneTimeNotifiers, Boolean isManual) throws Exception {
		try {
			GetDealerAssociateResponseDTO dealerAssociate = kManageApiHelper.getDealerAssociate(departmentUUID, fromUserUUID);
			if(dealerAssociate==null || dealerAssociate.getDealerAssociate()==null) {
				LOGGER.warn("User for UUID not found for user_uuid={}", fromUserUUID);
				return null;
			}
			LOGGER.info("publishInternalComment da_id={} customer_uuid={} ", dealerAssociate.getDealerAssociate().getId(), customerUUID);
			Long customerID = null;
			try {
				customerID = generalRepository.getCustomerIDForUUID(customerUUID);
			} catch (Exception e) {
				LOGGER.warn("Customer for UUID not found for customer_uuid={} e", customerUUID, e);
				return null;
			}
			LOGGER.info("publishInternalComment customer_id={}", customerID);

			Message message =convertToJpaEntity.getMessageJpaEntityForInternalComment(messageBody, dealerAssociate, customerID,
					messageProtocol, messagePurpose, isManual, oneTimeNotifiers);

			saveMessageHelper.saveMessage(message);
			if((newInternalSubscribers!=null && !newInternalSubscribers.isEmpty())
					|| (newExternalSubscribers!=null && !newExternalSubscribers.isEmpty()) ){
				LOGGER.info("publishInternalComment publishing subscription event for new_internal_subscribers={} "
						+ "new_external_subscribers={} for customer_id={} dealer_id={}",newInternalSubscribers,newExternalSubscribers,customerID,message.getDealerID());
				messagingViewControllerHelper.publishSubscriptionEvent(message.getCustomerID(), message.getDealerID(), message.getDealerDepartmentId(),
					newInternalSubscribers, newExternalSubscribers,  null, null, message.getDealerAssociateID());
			}
			rabbitHelper.pushToMessagePostSendingQueue(message, null, false, false, false, null);

			return message.getUuid();
		} catch (Exception e) {
			LOGGER.error("Error in publishInternalComment for message_body={} customer_uuid={} ", 
					messageBody, customerUUID, e);
			throw e;
		}
	}
	
	
}
