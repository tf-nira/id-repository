package io.mosip.idrepository.identity.entity;

import java.io.Serializable;
import java.sql.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardDetailPK implements Serializable {
	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = -1124172782509039861L;

	private String nin;

	private Date dateOfIssuance;
}
