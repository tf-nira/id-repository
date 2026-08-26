package io.mosip.idrepository.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import io.mosip.idrepository.identity.entity.UinBiometricHistory;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * The Interface UinBiometricHistoryRepo.
 *
 * @author Manoj SP
 */
public interface UinBiometricHistoryRepo extends JpaRepository<UinBiometricHistory, String> {


    @Query("select b from UinBiometricHistory b where b.uinRefId = :uinRefId and b.effectiveDateTime <= :asOf order by b.effectiveDateTime desc")
    List<UinBiometricHistory> findAllUpToAsOf(@Param("uinRefId") String uinRefId, @Param("asOf") LocalDateTime asOf);

}
