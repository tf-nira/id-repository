package io.mosip.credentialstore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BestFingerDto {

	int fingersIndex;
	String fingerPrint;
}
