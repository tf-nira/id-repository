package io.mosip.idrepository.identity.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.mosip.idrepository.identity.entity.CardDetail;

public interface CardDetailRepository extends JpaRepository<CardDetail, String> {

	@Query(value = "SELECT * FROM card_detail c WHERE c.nin =:nin  order by c.date_of_issuance  DESC LIMIT 1 ", nativeQuery = true)
	List<CardDetail> getCardDetail(@Param("nin") String nin);

	@Query(value = "SELECT * FROM card_detail c WHERE c.nin =:nin  order by c.date_of_issuance  DESC  ", nativeQuery = true)
	List<CardDetail> getCardDetails(@Param("nin") String nin);

}
