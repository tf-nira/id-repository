package io.mosip.idrepository.credentialsfeeder.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.mosip.idrepository.credentialsfeeder.entity.Uin;

/**
 * The Interface UinRepo.
 *
 * @author Manoj SP
 */
public interface UinRepo extends JpaRepository<Uin, String> {
	
	/**
	 * Gets the uin by refId .
	 *
	 * @param regId the reg id
	 * @return the Uin
	 */
	@Query("select uinHash from Uin where regId = :regId")
	String getUinHashByRid(@Param("regId") String regId);
	
	@Query("select uin from Uin where regId = :regId")
	String getUinByRid(@Param("regId") String regId);

	/**
	 * Exists by reg id.
	 *
	 * @param regId the reg id
	 * @return true, if successful
	 */
	boolean existsByRegId(String regId);

	/**
	 * Gets the status by uin.
	 *
	 * @param uin the uin
	 * @return the status by uin
	 */
	@Query("select statusCode from Uin where uin = :uin")
	String getStatusByUin(@Param("uin") String uin);
	
	/**
	 * Find by uin.
	 *
	 * @param uinHash the uin hash
	 * @return the uin
	 */
	Optional<Uin> findByUinHash(String uinHash);
	
	
	/**
	 * Exists by uinHash.
	 *
	 * @param uinHash the uin Hash.
	 * @return true, if successful.
	 */
	boolean existsByUinHash(String uinHash);
	
	Page<Uin> findByStatusCodeAndCreatedDateTimeBefore(String statusCode, LocalDateTime createdDateTime, Pageable pageable);

	/**
	 * Returns a page of encrypted UIN strings for active records created before the
	 * given timestamp. Using a String projection avoids loading the large
	 * {@code uinData} LOB column, which is not needed by the credentials feeder.
	 *
	 * @param statusCode      the UIN status code to filter by
	 * @param createdDateTime the upper-bound creation timestamp
	 * @param pageable        pagination and sort information
	 * @return page of encrypted UIN strings
	 */
	@Query("SELECT u.uin FROM Uin u WHERE u.statusCode = :statusCode AND u.createdDateTime < :createdDateTime")
	Page<String> findEncryptedUinByStatusCodeAndCreatedDateTimeBefore(
			@Param("statusCode") String statusCode,
			@Param("createdDateTime") LocalDateTime createdDateTime,
			Pageable pageable);
}
