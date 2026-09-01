package io.mosip.idrepository.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.mosip.idrepository.identity.entity.UinHistory;

import java.util.List;

/**
 * The Interface UinHistoryRepo.
 *
 * @author Manoj SP
 */
public interface UinHistoryRepo extends JpaRepository<UinHistory, String> {
	
	/**
	 * Exists by reg id.
	 *
	 * @param regId the reg id
	 * @return true, if successful
	 */
	boolean existsByRegId(String regId);
	
	/**
	 * Gets the uin by refId .
	 *
	 * @param regId the reg id
	 * @return the Uin
	 */
	@Query("select uinHash from UinHistory where regId = :regId")
	String getUinHashByRid(@Param("regId") String regId);

	/**
	 * for history
	 */
	@Query("select u from UinHistory u where u.uinRefId = :uinRefId and u.isDeleted = false order by u.effectiveDateTime desc")
	List<UinHistory> findByUinRefIdOrderByEffectiveDateTimeDesc(@Param("uinRefId") String uinRefId);


}
