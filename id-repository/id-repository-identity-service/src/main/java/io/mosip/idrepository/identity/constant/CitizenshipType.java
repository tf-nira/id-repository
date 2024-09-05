package io.mosip.idrepository.identity.constant;

public enum CitizenshipType {
	BIRTH("Birth", "C"),
	NATURALISATION("Naturalisation", "N"),
	REGISTRATION("Registration", "R");
	
	final String citizenshipType;
	final String ninCode;

	private CitizenshipType(String citizenshipType, String ninCode) {
		this.citizenshipType = citizenshipType;
		this.ninCode = ninCode;
	}
	
	public String getCitizenshipType() {
		return citizenshipType;
	}
	
	public String getNinCode() {
		return ninCode;
	}
}
