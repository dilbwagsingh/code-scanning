insert into DMS (DMSDescription) values ("myKaarmaAPI");

insert into Api (ID, Name, Description) select MAX(ID) + 1, 'order', 'API to push and pull repair orders.' FROM Api;

insert into ApiScope (ID, ApiID, Scope) select MAX(ID) + 1, (select ID from DMS where DMSDescription = 'myKaarmaAPI'), 'order.create' from ApiScope;
insert into ApiScope (ID, ApiID, Scope) select MAX(ID) + 1, (select ID from DMS where DMSDescription = 'myKaarmaAPI'), 'order.update' from ApiScope;
insert into ApiScope (ID, ApiID, Scope) select MAX(ID) + 1, (select ID from DMS where DMSDescription = 'myKaarmaAPI'), 'order.fetch' from ApiScope;
insert into ApiScope (ID, ApiID, Scope) select MAX(ID) + 1, (select ID from DMS where DMSDescription = 'myKaarmaAPI'), 'order.list' from ApiScope;

drop procedure if exists sp_korder_api_create;
DELIMITER $$
CREATE PROCEDURE sp_korder_api_create(
															    p_dealerId varchar(20), 
															    p_orderNumber varchar(75), 
															    p_orderStatus varchar(25),
															    p_dealerDepartmentToken varchar(255), 
															    p_customerKey varchar(128),
															    p_advisorNumber varchar(20),
															    p_vehicleVIN varchar(20),
															    p_vehicleKey varchar(50),
															    p_createDate varchar(10),
															    p_createTime varchar(10),
															    p_customerPayAmount varchar(24),
															    p_mileageIn varchar(30),
															    p_labourOpTypes varchar(1000),
                                                                p_tagOrHat varchar(45),
                                                                p_departmentType varchar(150),
                                                                p_laborTypes varchar(1000),
																p_payTypes varchar(100)
															    )
begin

	declare v_deptID bigint default NULL;
    declare v_customerId bigint(20) default NULL;
    declare v_dealerAssociateId bigint(20) default NULL;
    declare v_dealertimezone varchar(50) default NULL;
    declare v_vehicleId bigint(20) default NULL;
    declare v_orderDate datetime default NULL;
    declare v_DmsId bigint(20) default NULL;
    
	declare v_daFName varchar(45) default NULL;
    declare v_daLName varchar(45) default NULL;
    declare v_dmsDesc varchar(45) default NULL;
    declare v_wt_auto_send_q varchar(10) default NULL;
    

    -- get dealerDMSID
    select DMSID
    into v_DmsId
    from DealerDMS
    where DealerID = p_dealerId;
    
    -- get dms description
    select DMSDescription
    into v_dmsDesc
    from DMS
    where ID = v_DmsId;
    
	-- get timezone
    select OptionValue 
    into v_dealertimezone 
	from DealerSetupOption 
	where OptionKey = 'Dealer.Timezone' 
	and DealerID = p_dealerId;

    
    -- set order date
    select  (case when coalesce(concat(p_createDate, ' ', p_createTime),'') = '' 
			  then date_format(current_timestamp, '%Y-%m-%d %T')
			  else date_format(timestampadd(minute, -(v_dealertimezone + 480), concat(p_createDate, ' ', p_createTime)),'%Y-%m-%d %h:%i:%s') 
			  end) as orderDate
	into v_orderDate;
    
    
    -- get department ID
    select ID into v_deptID FROM DealerDepartment WHERE UUID = p_dealerDepartmentToken COLLATE utf8mb4_unicode_ci AND dealerid = p_dealerId;
        
    -- get customer ID from customer Key
    select ID
    into v_customerId
	from Customer
	where DealerID = p_dealerId
	and CustomerKey = p_customerKey
    limit 1;
    
    -- what to do with multiple customers with same key?
    
    -- get dealer associate id from advisor number
    select sf_getDealerAssociateFromDMSID(p_dealerId, p_advisorNumber)
    into v_dealerAssociateId;
    
	
	-- get da first and last name
	select FName, LName into v_daFName, v_daLName from DealerAssociate where ID = v_dealerAssociateId;
    
    -- get vehicle id
    select ID 
    into v_vehicleId
    from Vehicle
    where DealerID = p_dealerId
    and (VIN = p_vehicleVIN or VehicleKey = p_vehicleKey)
    and CustomerID = v_customerId
	limit 1;
    
    -- what to do with multiple vehicles?
    
    select OptionValue into v_wt_auto_send_q from DealerSetupOption where DealerID = p_dealerId and OptionKey = 'welcome.text.auto.send.mq';
    

	insert into DealerOrder(
							DealerID, 
							CustomerID, 
							DealerAssociateID, 
							VehicleID, 
							OrderNumber, 
							OrderType, 
							OrderStatus,  
							OrderDate, 
							NumberOfInvoices, 
							Amount,
							IsPaid,
							MileageText, 
							DMSDealerAssociateID,
                            LabourOpTypes,
                            TagOrHat,
                            DepartmentType,
                            LaborTypes,
                            DMSID,
							PayTypes,
                            UpdateTS
                            )
					  values(
							 p_dealerId,
                             v_customerId,
                             v_dealerAssociateId,
                             v_vehicleId,
                             p_orderNumber,
                             'RO',
                             p_orderStatus,
							 v_orderDate,
                             0,
                             cast(p_customerPayAmount as decimal(20,2)),
                             0,
                             p_mileageIn,
                             v_dealerAssociateId,
                             p_labourOpTypes,
                             p_tagOrHat,
                             p_departmentType,
                             p_laborTypes,
                             v_DmsId,
							 p_payTypes,
                             current_timestamp
                             )
	on duplicate key update ID = LAST_INSERT_ID(ID),
							CustomerID = VALUES(CustomerID),
							VehicleID = VALUES(VehicleID),
							DealerAssociateID = VALUES(DealerAssociateID),
							Amount = cast(p_customerPayAmount as decimal(20,2)),
							OrderStatus = VALUES(OrderStatus),
							DepartmentType = VALUES(DepartmentType),
                            LabourOpTypes = VALUES(LabourOpTypes),
                            TagOrHat = VALUES(TagOrHat),
                            LaborTypes = VALUES(LaborTypes),
                            OrderDate = VALUES(OrderDate),
                            IsPaid = (case when VALUES(OrderStatus) = 'O' then b'0' else IsPaid end),
							PayTypes = VALUES(PayTypes),
                            UpdateTS = current_timestamp,
							DMSDealerAssociateID = (case when DMSDealerAssociateID is null then VALUES(DMSDealerAssociateID) else DMSDealerAssociateID end); 

SELECT * FROM (select LAST_INSERT_ID() as ID, v_daFName, v_daLName, v_dmsDesc, (select GUID from Invoice where OrderID = LAST_INSERT_ID() and PaidDate is not null order by ID desc limit 1) as v_invoiceGuid, v_wt_auto_send_q) as A 
JOIN
(select * from DealerOrder where ID = LAST_INSERT_ID()) as B
ON A.ID = B.ID;

commit;

end$$