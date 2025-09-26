package io.mosip.credential.request.generator.dao;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.mosip.credential.request.generator.context.CryptoContext;
import io.mosip.credential.request.generator.entity.CredentialEntity;
import io.mosip.credential.request.generator.repositary.CredentialRepositary;

/**
 * This class is designed for keeping the data access methods where we want
 * control the cryptographic loading CredentialEntity. Example: Avoid decryption
 * while loading the CredentialEntity.
 *
 * @author tarique-azeez
 */

@Component
public class EncryptedCredentialDao {

	@Autowired
	private CredentialRepositary<CredentialEntity, String> credentialRepo;

	public List<CredentialEntity> getCredentialByStatus(String statusCode, String[] issuers, int pageSize) {
		try {
			CryptoContext.setSkipDecryption(true);
			return credentialRepo.findCredentialByStatusCodeAndIssuers(statusCode, issuers, pageSize);
		} finally {
			CryptoContext.setSkipDecryption(false);
		}
	}
}