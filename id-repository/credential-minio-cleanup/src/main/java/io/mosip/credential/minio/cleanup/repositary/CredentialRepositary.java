package io.mosip.credential.minio.cleanup.repositary;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import io.mosip.credential.minio.cleanup.entity.CredentialEntity;
import io.mosip.kernel.core.dataaccess.spi.repository.BaseRepository;


/**
 * The Interface CredentialRepositary.
 *
 * @author Sowmya
 * 
 *         The Interface CredentialRepositary.
 * @param <T> the generic type
 * @param <E> the element type
 */
@Repository
public interface CredentialRepositary<T extends CredentialEntity, E> extends BaseRepository<T, E> {


	/**
	 * Find credential by status codes.
	 *
	 * @param statusCodes the status codes
	 * @param type        the type
	 * @param
	 * @return the page
	 */
	@Query("SELECT crdn FROM CredentialEntity crdn WHERE crdn.statusCode in :statusCodes")
	Page<CredentialEntity> findCredentialByStatusCodes(@Param("statusCodes") String[] statusCodes, Pageable pageable);
}
