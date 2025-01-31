package io.mosip.idrepository.identity.constant;

public enum CitizenshipType {
	BIRTH("Birth", "C"),
	NATURALIZATION("Naturalization", "N"),
	REGISTRATION("Registration", "R"), DUAL("Dual", "R"), ARTICLE("Article", "C");
	
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
