package io.mosip.idrepository.identity.constant;

public enum SpouseNumberMapping {
	ONE(1, ""), TWO(2, "Two"), THREE(3, "Three"), FOUR(4, "Four");

	final int spouseCode;
	final String spouseNumber;

	private SpouseNumberMapping(int spouseCode, String spouseNumber) {
		this.spouseCode = spouseCode;
		this.spouseNumber = spouseNumber;
	}
	
	public int getspouseCode()
	{
		return spouseCode;
	}
	
	public String getSpouseNumber() {
		return spouseNumber;
	}
}
