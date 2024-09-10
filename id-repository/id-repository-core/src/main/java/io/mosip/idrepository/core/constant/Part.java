package io.mosip.idrepository.core.constant;

public enum Part {

	
	
	PART1("1"),
	
	PART2("2"),
	
	PART3("3"),

	PART4("4");
	

	private final String part;

	
	public String getPart() {
		return part;
	}
	
	
	private Part(String part) {
		this.part = part;
	}
}
