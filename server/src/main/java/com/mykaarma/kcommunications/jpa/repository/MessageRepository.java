package com.mykaarma.kcommunications.jpa.repository;

import java.math.BigInteger;
import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.mykaarma.kcommunications.model.jpa.Message;
import com.mykaarma.kcommunications_model.enums.MessageProtocol;

@Transactional
public interface MessageRepository extends JpaRepository<Message, Long> {
	
	@SuppressWarnings("unchecked")
	 public Message saveAndFlush(Message message);
	
	 @Query(value = " select count(m.ID) from " 
						+  " Message as m "
						+" where m.MessageType = 'S' and m.CustomerID = :customerID "
						+" and m.Protocol = 'X' "
						+" and m.ToNumber = :phoneNumber "
						+" and m.isArchive = 0 "
						+" and m.ReceivedOn >= DATE_ADD(NOW(), INTERVAL - :days DAY) ", nativeQuery = true)
	 public Long isTextMessageSentInLastXDays(@Param("customerID") Long customerID, @Param("phoneNumber") String phoneNumber, @Param("days") Integer days);

	 @Query(value = "select fromnumber from Message where customerid = :customerID and messagetype = 'S' "
				+ " and (protocol = 'X' or protocol = 'V') and dealerdepartmentid = :departmentID and IsArchive=0 "
				+ " and (messagepurpose is null or messagePurpose not in (:excludedPurposes)) "
				+ " order by receivedon desc limit 1", nativeQuery = true)
	 public String getLastBrokerNumberUsedForCustomerAndDepartment(@Param("customerID") Long customerID, @Param("departmentID") Long departmentID, @Param("excludedPurposes") List<String> excludedPurposes);

	 public Message findByuuid(String messageUUID);
	 
	 public List<Message> findByCommunicationUidInOrderByReceivedOnDesc(List<String> communicationUidList );
	 
	 public List<Message> findByuuidInOrderByReceivedOnDesc(List<String> messageUuids);
	 
	 public Message findByid(Long messageID);
	 
	 @Query(value = "select ID from Message where dealerid = :dealerID and protocol = :protocol order by receivedon desc", nativeQuery = true)
	 public List<BigInteger> fetchAllMessagesForDealerByProtocol(@Param("dealerID") Long dealerID, @Param("protocol") String protocol);
	 
	 @Query(value = "select ID from Message where dealerid = :dealerID and protocol = :protocol and receivedon >= :startDate and "
	 		+ "      receivedon <= :endDate order by receivedon desc", nativeQuery = true)
	 public List<BigInteger> fetchAllMessagesForDealerByStartAndEndDateAndProtocol(@Param("dealerID") Long dealerID, @Param("startDate") Date startDate, @Param("endDate") Date endDate, @Param("protocol") String protocol);

	 @Query(value = "select id, dealerid from Message where uuid=:messageUUID", nativeQuery = true)
	 public List<Object[]> findDealerIDAndMessageIDByuuid(@Param("messageUUID") String messageUUID);
	 
	 @Query(value = "select m.MessageType, mmd.MetaData from Message m left outer join MessageMetaData mmd on m.ID=mmd.MessageID "
			 + "where customerId = :customerID and protocol = :protocol and ID < :messageID and MessageType!='F' and MessageType!='D' order by receivedon desc limit 1 ", nativeQuery = true)
	 public List<Object[]> findLatestPreviousMessageForGivenProtocolAndCustomerIDAndMessageID(@Param("messageID") Long messageID,@Param("customerID") Long customerID,@Param("protocol") String protocol);
	 
	 @Query(value = "select m.ID, m.MessageType, m.Protocol, m.CustomerID, "
				+ "m.DealerID, m.DealerAssociateID , me.Subject , me.MessageBody "
				+ "from Message m join MessageExtn me on m.ID=me.MessageID "
		 		+ "where m.UUID = :messageUUID ", nativeQuery = true)
	 public List<Object[]> fetchMessageForGivenUUID(@Param("messageUUID") String messageUUID);
	 
	 @Query(value = "select m.receivedOn, me.MessageBody "
				+ "from Message m join MessageExtn me on m.ID=me.MessageID "
		 		+ "where m.ID = :messageID ", nativeQuery = true)
	 public List<Object[]> fetchMessageBodyAndReceivedOnForGivenMessageID(@Param("messageID") Long messageID);

	 @Query(value="select messageID, messageBody from ( " +
			 "select m.id as messageID, me.messageBody as messageBody, m.receivedOn from Message m " +
			 "inner join MessageExtn me on me.messageId=m.id " +
			 "inner join MessageThread mt on mt.messageId = m.id " +
			 "inner join Thread t on t.id = mt.threadId " +
			 "left join MessageAttributes ma on m.ID=ma.MessageID " +
			 "where m.customerID = :customerId " +
			 "and m.dealerID = :dealerId " +
			 "and (case when :messageProtocol is not null then m.Protocol= :messageProtocol " +
			 "when :messageProtocol is null and :showInternalComments is false then m.Protocol in ('X', 'E', 'V') " +
			 "else m.Protocol like '%' end) " +
			 "and (case when :messageType is not null THEN  m.MessageType= :messageType " +
			 "when :messageType is null and :showInternalComments is false then m.messageType in ('S', 'I') " +
			 "else m.MessageType LIKE '%' end) " +
			 "and (case when :fromDate is not null then m.ReceivedOn>= :fromDate else m.ReceivedOn LIKE '%' end) " +
			 "and (case when :toDate is not null THEN  m.ReceivedOn<= :toDate else m.ReceivedOn LIKE '%' end) " +
			 "and t.closed = false " +
			 "and (coalesce(ma.showInCustomerConversation, 1) = 1) " +
			 "order by m.receivedOn desc) " +
			 "as mm order by messageID desc ", nativeQuery=true)
	 public List<Object[]> getMessages(@Param("showInternalComments") Boolean showInternalComments, @Param("dealerId") Long dealerId,@Param("customerId") Long customerId,@Param("messageProtocol") String messageProtocol,@Param("messageType") String messageType,@Param("fromDate") Date fromDate, 
				@Param("toDate") Date toDate);

	@Query(value="select messageID, messageBody from ( " +
			"select m.id as messageID, me.messageBody as messageBody, m.receivedOn from Message m " +
			"inner join MessageExtn me on me.messageId=m.id " +
			"inner join MessageThread mt on mt.messageId = m.id " +
			"inner join Thread t on t.id = mt.threadId " +
			"left join MessageAttributes ma on m.ID=ma.MessageID " +
			"where m.customerID = :customerId " +
			"and m.dealerID = :dealerId " +
			"and (case when :messageProtocol is not null then m.Protocol= :messageProtocol " +
			"when :messageProtocol is null and :showInternalComments is false then m.Protocol in ('X', 'E', 'V') " +
			"else m.Protocol like '%' end) " +
			"and (case when :messageType is not null THEN  m.MessageType= :messageType " +
			"when :messageType is null and :showInternalComments is false then m.messageType in ('S', 'I') " +
			"else m.MessageType LIKE '%' end) " +
			"and (case when :fromDate is not null then m.ReceivedOn>= :fromDate else m.ReceivedOn LIKE '%' end) " +
			"and (case when :toDate is not null THEN  m.ReceivedOn<= :toDate else m.ReceivedOn LIKE '%' end) " +
			"and t.closed = false " +
			"and (coalesce(ma.showInCustomerConversation, 1) = 1) " +
			"order by m.receivedOn desc) " +
			"as mm order by messageID desc limit :lastNMessages ", nativeQuery=true)
	public List<Object[]> getMessagesWithLastNMessages(@Param("showInternalComments") Boolean showInternalComments, @Param("dealerId") Long dealerId,@Param("customerId") Long customerId,@Param("messageProtocol") String messageProtocol,@Param("messageType") String messageType,@Param("fromDate") Date fromDate, 
			@Param("toDate") Date toDate,@Param("lastNMessages") int lastNMessages);
	
	
	@Query(value="select m.id from Message m "
			+ "left join MessageAttributes ma on m.ID=ma.MessageID "
			+ "where m.customerID = :customerId "
			+ "and m.dealerID = :dealerId "
			+ "and (case when :messageProtocol is not null then  m.Protocol= :messageProtocol else m.Protocol LIKE '%' end) "
			+ "and (case when :messageType is not null THEN  m.MessageType= :messageType else m.MessageType LIKE '%' end) "
			+ "and (case when :fromDate is not null then m.ReceivedOn>= :fromDate else m.ReceivedOn LIKE '%' end) "
			+ "and (case when :toDate is not null THEN  m.ReceivedOn<= :toDate else m.ReceivedOn LIKE '%' end) "
			+ "and coalesce(ma.CountInThreadMessageCount, 1) = 1 "
			+ "group by m.id order by m.receivedOn desc ",nativeQuery=true)
	public List<BigInteger> getMessageCountForLastGivenDates(@Param("dealerId") Long dealerId,@Param("customerId") Long customerId,@Param("messageProtocol") String messageProtocol,@Param("messageType") String messageType,@Param("fromDate") Date fromDate, 
			@Param("toDate") Date toDate);
	
	@Query(value="select p.Name from DealerVirtualAttributes ds "
			+ "join vw_partner p on ds.FrontEndPartnerID = p.ID where ds.dealerID= :dealerId",nativeQuery=true)
	public String getFrontEndPartnerForDealer(@Param("dealerId") Long dealerId);
	
	@Query(value = "select m.id, m.customerID, m.dealerID, m.dealerAssociateID, m.dealerDepartmentId,"
			+ " mt.threadID, m.messageType from Message m join MessageThread mt on m.id = mt.messageID"
			+ " where m.communicationUID = :sid ",nativeQuery=true)
	public List<Object[]> getFilterDataRequest(@Param("sid")String sid);
	
	@Query(value = "select m from Message as m where m.communicationUid = :cuid ")
	public Message getMessageFromCommunicationUidMatch(@Param("cuid")String communicationID);
	
	@Query(value = "select * from Message as m "
			+ "where m.communicationUid = :cuid ",nativeQuery=true)
	public List<Message> getMessageFromCommunicationUid(@Param("cuid")String communicationID);
	
	@Query(value = "select m.uuid from Message m where m.communicationUid = :sid", nativeQuery = true)
	public String getMessageUUIDFromCommunicationUid(@Param("sid")String sid);
	
	@Query(value = "select m.dealerDepartmentId from Message m where m.communicationUid = :sid", nativeQuery = true)
	public Long getDepartmentIDFromCommunicationUid(@Param("sid")String sid);
	
	@Query(value = "select m.dealerId from  Message m "
			+ "where m.communicationUid = :sid limit 1",nativeQuery=true)
	public Long getMessageDealerIDFromCommunicationSID(@Param("sid")String sid);
	
	@Query(value = "select m.Uuid from  Message m "
			+ " left join MessageAttributes ma on ma.MessageID=m.ID "
			+ "where m.customerId = :customerId "
			+ " and m.ReceivedOn is not null "
			+ " and (case when :messageProtocolList is not null then  m.Protocol in (:messageProtocolList) else m.Protocol LIKE '%' end) "
			+ " and m.IsArchive = 0 "
			+ " and coalesce(ma.ShowInCustomerConversation, 1) = 1 "
			+ "order by m.ReceivedOn desc, m.id desc limit :messageLimit",nativeQuery=true)
	public List<String> getMessageUuidsForCustomer(@Param("customerId")Long customerId, 
			@Param("messageLimit") int messageLimit, @Param("messageProtocolList") List<String> messageProtocolList);
	
	@Query(value = "select m.Uuid from  Message m "
			+ " left join MessageAttributes ma on ma.MessageID=m.ID "
			+ "where m.customerId = :customerId and m.ReceivedOn <= :lastMessageReceivedDate"
			+ " and m.Uuid not in ( :messageUuidsList ) "
			+ " and m.ReceivedOn is not null "
			+ " and (case when :messageProtocolList is not null then  m.Protocol in (:messageProtocolList) else m.Protocol LIKE '%' end) "
			+ " and m.IsArchive = 0 "
			+ " and coalesce(ma.ShowInCustomerConversation, 1) = 1 "
			+ " order by m.ReceivedOn desc, m.id desc limit :messageLimit",nativeQuery=true)
	public List<String> getMessageUuidsForCustomerOlderThanGivenData(@Param("customerId")Long customerId, @Param("lastMessageReceivedDate") Date lastMessageReceivedDate,
			@Param("messageLimit") int messageLimit,@Param("messageUuidsList") List<String> messageUuidsReceivedAtSameTime,  @Param("messageProtocolList") List<String> messageProtocolList);
	
	@Query(value = "select m.Uuid from  Message m "
			+ " left join MessageAttributes ma on ma.MessageID=m.ID "
			+ "where m.customerId = :customerId and m.ReceivedOn <= :lastMessageReceivedDate"
			+ " and m.ReceivedOn is not null "
			+ " and (case when :messageProtocolList is not null then  m.Protocol in (:messageProtocolList) else m.Protocol LIKE '%' end) "
			+ " and m.IsArchive = 0 "
			+ " and coalesce(ma.ShowInCustomerConversation, 1) = 1 "
			+ " order by m.ReceivedOn desc, m.id desc limit :messageLimit",nativeQuery=true)
	public List<String> getMessageUuidsForCustomerOlderThanGivenData(@Param("customerId")Long customerId, @Param("lastMessageReceivedDate") Date lastMessageReceivedDate,
			@Param("messageLimit") int messageLimit,  @Param("messageProtocolList") List<String> messageProtocolList);
	
	@Query(value = "select m.Uuid from  Message m "
			+ "where m.customerId = :customerId "
			+ " and m.MessageType= :messageType "
			+ " and m.IsArchive = 0 "
			+ " order by m.id desc limit :messageLimit",nativeQuery=true)
	public List<String> getMessageUuidsOfTypeAndLimit(@Param("customerId")Long customerId, @Param("messageType") String messageType,
			@Param("messageLimit") int messageLimit);
	
	@Query(value = "UPDATE Message SET deliveryStatus= :deliveryStatus,"
			+ "twilioDeliveryFailureMessage= :failureReason  where ID = :messageID",nativeQuery=true)
	public void updateMessageDeliveryStatusForMessageID(@Param("messageID")Long messageID, @Param("deliveryStatus")String deliveryStatus, @Param("failureReason")String failureReason);

	
}
