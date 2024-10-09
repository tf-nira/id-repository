package io.mosip.credential.minio.cleanup.config;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import io.mosip.commons.khazana.impl.S3Adapter;
import io.mosip.commons.khazana.spi.ObjectStoreAdapter;
import io.mosip.credential.minio.cleanup.entity.CredentialEntity;
import io.mosip.credential.minio.cleanup.repositary.CredentialRepositary;
import io.mosip.idrepository.core.builder.RestRequestBuilder;
import io.mosip.idrepository.core.constant.RestServicesConstants;
import io.mosip.idrepository.core.helper.AuditHelper;
import io.mosip.idrepository.core.helper.RestHelper;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.DummyPartnerCheckUtil;
import io.mosip.kernel.dataaccess.hibernate.config.HibernateDaoConfig;
import io.mosip.kernel.dataaccess.hibernate.repository.impl.HibernateRepositoryImpl;

@Configuration
@EnableJpaRepositories(entityManagerFactoryRef = "entityManagerFactory", basePackageClasses = {
		CredentialRepositary.class }, basePackages = "io.mosip.cleanup.minio.repositary.*", repositoryBaseClass = HibernateRepositoryImpl.class)
@EntityScan(basePackageClasses = { CredentialEntity.class })
public class CredentialMinioCleanupConfig extends HibernateDaoConfig {

	@Bean
	public DummyPartnerCheckUtil dummyPartnerCheckUtil() {
		return new DummyPartnerCheckUtil();
	}

	@Bean
	public IdRepoSecurityManager securityManager() {
		return new IdRepoSecurityManager();
	}

	@Bean
	public AuditHelper getAuditHelper() {
		return new AuditHelper();

	}

	@Bean
	public RestHelper restHelper() {
		return new RestHelper();
	}

	@Bean
	public RestRequestBuilder getRestRequestBuilder() {
		return new RestRequestBuilder(Arrays.stream(RestServicesConstants.values())
				.map(RestServicesConstants::getServiceName).collect(Collectors.toList()));
	}

	@Bean
	public ObjectStoreAdapter objectStoreAdapter() {
		return new S3Adapter();
	}

}
