package io.mosip.idrepository.credentialsfeeder.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import io.mosip.idrepository.credentialsfeeder.entity.CredentialFeederProgress;

public interface CredentialFeederProgressRepo extends JpaRepository<CredentialFeederProgress, String>{

}
