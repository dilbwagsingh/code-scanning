package com.mykaarma.kcommunications.model.api;

public enum ErrorCodes {

    CREDENTIALS_ARE_NULL(10039, "NULL_CREDENTIALS", "When login credentials are missing from header", "Auth credentials are empty or null"),
    ACCESS_FORBIDDEN(10040, "ACCESS_FORBIDDEN", "Access Forbidden!!", "Access Forbidden!!"),
    WRONG_CREDENTIAL(10041, "WRONG_CREDENTIAL", "Credentials are wrong", "Authorization refused for provided credentials. Wrong username and password."),
    NOT_AUTHROIZED_REQUEST(10042, "REQUEST_NOT_AUTHROIZED", "Credentials are correct but is not authorised to do the specific action", "Authentication passed but Authorization failed."),
    INTERNAL_SERVER_EXCEPTION(10043, "INTERNAL_SERVER_EXCEPTION","Something went wrong on our side", "Something went wrong on our side"),
    INTERNAL_ERROR_GET_CUSTOMER(10044, "INTERNAL_ERROR_GET_CUSTOMER","Some error occured while getting customer", "Some error occured while getting customer"),
    INTERNAL_ERROR_GET_CUSTOMER_LIST(10045, "INTERNAL_ERROR_GET_CUSTOMER_LIST", "Some error occured while getting customers list", "Some error occured while getting customers list"),
    INTERNAL_ERROR_SAVE_BESTTIMETOCONTACT(10046, "INTERNAL_ERROR_SAVE_BEST_TIME_TO_CONTACT", "Some error occured while saving best time to contact. Most probably problem with getting db connection", "Some error occured while saving best time to contact"),
    INTERNAL_ERROR_UPDATE_BESTTIMETOCONTACT(10047, "INTERNAL_ERROR_UPDATE_BEST_TIME_TO_CONTACT", "Some error occured while updating best time to contact. Most probably problem with getting db connection", "Some error occured while updating best time to contact"),
    INTERNAL_ERROR_SAVE_ADDRESS(10048, "INTERNAL_ERROR_SAVE_ADDRESS", "Some error occured while saving address. Most probably problem with getting db connection", "Some error occured while saving address"),
    INTERNAL_ERROR_UPDATE_ADDRESS(10049, "INTERNAL_ERROR_UPDATE_ADDRESS", "Some error occured while updating address. Most probably problem with getting db connection", "Some error occured while updating address"),
    INTERNAL_ERROR_SAVE_PREFCOMM(10050, "INTERNAL_ERROR_SAVE_PREFCOMM", "Some error occured while saving preferred communication. Most probably problem with getting db connection", "Some error occured while saving preferred communication"),
    INTERNAL_ERROR_UPDATE_PREFCOMM(10051, "INTERNAL_ERROR_UPDATE_PREFCOMM", "Some error occured while updating preferred communication. Most probably problem with getting db connection", "Some error occured while updating preferred communication"),
    INTERNAL_ERROR_SAVE_VEHICLE(10052, "INTERNAL_ERROR_SAVE_VEHICLE", "Some error occured while saving vehicles. Most probably problem with getting db connection", "Some error occured while saving vehicles"),
    INTERNAL_ERROR_UPDATE_VEHICLE(10053, "INTERNAL_ERROR_UPDATE_VEHICLE", "Some error occured while updating vehicles. Most probably problem with getting db connection", "Some error occured while updating vehicles"),
    CUSTOMER_EXISTS(10054, "CUSTOMER_EXISTS", "Customer with given customer key already exists. Use POST to update it", "Customer already exists please try POST request instead of PUT"),
    CUSTOMER_NOT_EXISTS(10055, "CUSTOMER_NOT_EXISTS", "Customer with given uuid doesn't exists", "Customer doesn't exists for given UUID"),
    NO_CUSTOMER_GUID(10056, "NO_CUSTOMER_UUID", "When customer uuid is not given in POST request", "Customer Uuid is required for POST request"),
    INCORRECT_CUSTOMER_KEY(10057, "INCORRECT_CUSTOMER_KEY","When another customer already exists with same customer key", "A different customer already exists with provided customer key. Please try again with a unique customer key"),
    SEARCH_TERM_EMPTY(10058, "SEARCH_TERM_EMPTY", "Search term can't be null or empty", "Invalid request. Search Term can not be null or empty."),
    INVALID_SEARCH_TERM(10059, "INVALID_SEARCH_TERM","Search term should be of atleast 3 characters to proceed", "Invalid request. Search Term should have minimum of 3 characters."),
    NO_MERGE_RECORD_FOUND(10060,"NO_MERGE_RECORD_FOUND","No merge log found for this customer","CustomerGUID should be of primary customer"),
    DA_NOT_EXISTS(10061,"DA_NOT_EXISTS" ,"Record not found","Provide valid service advisor uuid"),
	FROM_DA_NOT_EXISTS(40001,"DA_NOT_EXISTS" ,"Record not found","Provide valid service advisor uuid from who you want to delegate"),
	EMPTY_LIST(40002,"EMPTY_LIST" ,"List is empty","Add entries to the list"),
	CUSTOMER_NOT_EXIST(40003, "CUSTOMER_NOT_EXISTS", "Customer with given uuid doesn't exist", "Customer with given uuid doesn't exist"),
	INSUFFICIENT_DETAILS(40004, "INSUFFICIENT_DETAILS", "Details are not sufficient.", "More details are required"),
	DEALER_DEPARTMENT_NOT_EXISTS(40005, "DEALER_DEPARTMENT_NOT_EXISTS", "Dealer department with given uuid doesn't exist", "Dealer department with given uuid doesn't exist"),
	TO_DA_NOT_EXISTS(40006,"DA_NOT_EXISTS" ,"Record not found","Provide valid service advisor uuid to whom you want to delegate"),
	DELEGATOR_DA_NOT_EXISTS(40007,"DA_NOT_EXISTS" ,"Record not found","Provide valid service advisor uuid who is making the delegation"),
	DEPARTMENT_DEALER_MISMATCH(40008,"DEPARTMENT_DEALER_MISMATCH" ,"Department and dealer dont match","Department of the dealer is not same as the logged in dealer"),
	DA_DEPARTMENT_MISMATCH(40008,"DA_DEPARTMENT_MISMATCH" ,"Departments of delegator or delegatee do not match with department given in uuid","Departments of delegator or delegatee do not match with department given in uuid"),
	CUSTOMER_DEALER_MISMATCH(40009,"CUSTOMER_DEALER_MISMATCH" ,"DealerUUID of customer and logged in dealership dont match","DealerUUID of customer and logged in dealership dont match"),
	INTERNAL_ERROR(40010,"INTERNAL_ERROR" ,"Internal error.","Internal error."),
	
	NO_CUSTOMER_USER_SESSION_UUID(40011, "NULL_CU_SESS_UUID", "No customer user session UUID is provided in the request", "No customer user session UUID is provided in the request"),
	NO_DDEPT_UUID(40012, "NULL_DDEPT_UUID", "No dealer department UUID is provided in the request", "No dealer department UUID UUID is provided in the request"),
	INVALID_CUSTOMER_USER_SESSION_UUID(40013, "INVALID_CU_SESS_UUID", "Invalid customer user session UUID is provided in the request", "Invalid customer user session UUID is provided in the request"),
	INVALID_DDEPT_UUID(40014, "INVALID_DDEPT_UUID", "Invalid dealer department UUID is provided in the request", "Invalid dealer department UUID UUID is provided in the request"),
	INTERNAL_SERVER_ERROR(40015, "INTERNAL_SERVER_ERROR", "Internal Server Error", "Internal Server Error"),
	CUSTOMER_USER_NOT_FOUND(40016, "CU_NOT_FOUND", "Customer User not found using email and password", "ICustomer User not found using email and password"),
	OLD_PASSWORD_WRONG(40017, "WRONG_OLD_PASS", "Old password entered is wrong", "Old password entered is wrong"),
	NEW_PASSWORD_SAME(40018, "SAME_NEW_PASS", "Old and new password are same", "Old and new password are same"),
	MULTIPLE_CU_WITH_SAME_EMAIL(40019, "MULTIPLE_CU_WITH_SAME_EMAIL", "Multiple customer users found with same email", "Multiple customer users found with same email"),
	EMAIL_ALREADY_REGISTERED(40020, "EMAIL_ALREADY_REGISTERED", "Email Address already registered. Please login.", "Email Address already registered. Please login."),
	CUSTOMER_USER_PASS_WRONG(40021, "CU_USER_PASS_WRONG", "Customer User username password combination wrong ", "Customer User username password combination wrong "),
	MISSING_MESSAGE_UUID(40022, "MISSING_MESSAGE_UUID", "MessageUUID missing in request", "MessageUUID missing in request"),
	INVALID_MESSAGE_ID(40023, "INVALID_MESSAGE_ID", "Message id is not present for message", "Message id is not present for message"),
	INVALID_DEALER_ID(40024, "INVALID_DEALER_ID", "Dealer id not present for message", "Dealer id not present for message"),
	QUEUE_PUSH_FAILURE(40025, "QUEUE_PUSH_FAILUE", "Unable to push messsage to queue", "Unable to push messsage to queue"),
	MISSING_DEALER_IDS(40026, "MISSING_DEALER_IDS", "DealerIds are missing in request", "DealerIds are missing in request"),
	FEATURE_NOT_ENABLED(40027,"FEATURE_NOT_ENABLED","Feature not enabled for dealership","Feature not enabled for dealership"),
	MIGRATION_NOT_ALLOWED(40028, "MIGRATION_NOT_ALLOWED", "Migration not allowed for dealer","Migration not allowed for dealer"),
	REQUEST_NULL(4002, "REQUEST_NULL", "Request Object Not Present","Request object is null");

    private Integer errorCode;
    private String errorTitle;
    private String errorDescription;
    private String errorMessage;
	
	private ErrorCodes(Integer errorCode, String errorTitle, String errorDescription, String errorMessage) {
		this.errorCode = errorCode;
		this.errorTitle = errorTitle;
		this.errorDescription = errorDescription;
		this.errorMessage = errorMessage;
	}
	
	public Integer getErrorCode() {
		return errorCode;
	}
	public String getErrorDescription() {
		return errorDescription;
	}
	public String getErrorMessage() {
		return errorMessage;
	}

	public String getErrorTitle() {
		return errorTitle;
	}

}