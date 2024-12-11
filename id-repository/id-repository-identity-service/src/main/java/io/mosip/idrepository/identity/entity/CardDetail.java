package io.mosip.idrepository.identity.entity;

import java.sql.Date;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@Table(name = "card_detail", schema = "idrepo")
@Entity
@IdClass(CardDetailPK.class)
public class CardDetail {

	@Id
	@NotNull
	@Column(name = "nin")
	private String nin;

	@Column(name = "card_number")
	private String cardNumber;

	@Id
	@NotNull
	@Column(name = "date_of_issuance")
	private Date dateOfIssuance;

	@Column(name = "date_of_expiry")
	private Date dateOfExpiry;

	@NotNull
	@Column(name = "cr_by")
	private String createdBy;

	@NotNull
	@Column(name = "cr_dtimes")
	private LocalDateTime crDTimes;

	@Column(name = "upd_by")
	private String updatedBy;

	@Column(name = "upd_dtimes")
	private LocalDateTime updDTimes;
}
