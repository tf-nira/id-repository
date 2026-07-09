-- Rollback: add pk_kind constriant for nin,data_of_issunce

ALTER TABLE idrepo.card_detail
DROP CONSTRAINT pk_nind;

ALTER TABLE idrepo.card_detail
ADD CONSTRAINT pk_nind PRIMARY KEY (nin,date_of_issuance);
