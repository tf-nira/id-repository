package io.mosip.idrepository.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;


import io.mosip.idrepository.identity.entity.UinDocumentHistory;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * The Interface UinDocumentHistoryRepo.
 *
 * @author Manoj SP
 */
public interface UinDocumentHistoryRepo extends JpaRepository<UinDocumentHistory, String> {

    @Query("select d from UinDocumentHistory d where d.uinRefId = :uinRefId and d.effectiveDateTime <= :asOf order by d.effectiveDateTime desc")
    List<UinDocumentHistory> findAllUpToAsOf(@Param("uinRefId") String uinRefId, @Param("asOf") LocalDateTime asOf);

}
