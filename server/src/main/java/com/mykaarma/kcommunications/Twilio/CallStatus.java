package com.mykaarma.kcommunications.Twilio;

public enum CallStatus {

	queued("queued", "Call is queued", 1),
	wrong_prompt("wrong_prompt", "wrong prompt", 12),
	ringing("ringing", "Phone is ringing", 1),
	empty_prompt("empty-prompt", "empty prompt", 13),
	in_progress("in-progress","Call In Progress", 1),
	party1_dropped("party1-dropped","You cancelled the call", 9),
	party1_connected("party1-connected","You are connected", 10),
	serviceAdvisor_greeting("sa-greeting","Playing greetings for SA", 107),
	waiting_connect("party1-connected", "press 1 to connect", 106),
	party2_connected("party2-connected", "Call is now connected", 20),
	key("enter-key","Connecting you..", 102),
	party2_dropped("party2-dropped","Call Dropped", 19),
	completed("completed", "Call completed", 100),
	busy("busy", "Caller busy", 105),
	failed("failed", "Call failed", 99),
	missed("missed", "Missed call", 102),
	no_answer("no-answer", "No Answer",	104),
	canceled("ui-icon-grip-diagonal-secanceled", "Call canceled",null), 
	party1_connected_conference("party1_connected_conference","Party 1 is connected to conference", 201),
	party2_connected_conference("party2_connected_conference","Party 2 is connected to conference", 202),
	answered("answered","answered", 203),
	playing_conference_greeting("playing_conference_greeting","Playing conference greeting", 204);
	
	
	private String formattedName;
	private String message;
	private Integer id; // id maps to the id column of voicecallstatus table

	
	CallStatus(String formattedName, String message, Integer id) {
		this.formattedName = formattedName;
		this.message = message;
		this.id = id;
	}

	public String getFormattedName() {
		return formattedName;
	}

	public String getMessage() {
		return message;
	}

	public Integer getID() {
		return id;
	}

	public static CallStatus getByFormattedName(String formattedName) {
		String name = formattedName.replace("-", "_");
		return valueOf(name);
	}

	public static CallStatus getByID(Long id2) {
		long id = id2.longValue();
		for (CallStatus cs : values()) {
			if (cs.getID() == null)
				continue;
			long cs_id = cs.getID().longValue();
			if (id == cs_id)
				return cs;
		}
		return null;
	}
}
