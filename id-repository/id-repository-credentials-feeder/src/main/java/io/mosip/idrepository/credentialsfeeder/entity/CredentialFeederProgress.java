package io.mosip.idrepository.credentialsfeeder.entity;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "credential_feeder_progress", schema = "idrepo")
public class CredentialFeederProgress {
	
	@Id
	@Column(name = "instance_id")
	private String instanceId;
	
	@Column(name = "from_date")
	private LocalDateTime fromDate;
	
	@Column(name = "to_date")
	private LocalDateTime toDate;
	
	@Column(name = "upd_dtimes")
	private LocalDateTime updateDateTime;
	
	@Column(name = "cr_dtimes")
	private LocalDateTime createdDateTime;
	
	@Column(name = "processed_count")
	private Integer processedCount;
}
