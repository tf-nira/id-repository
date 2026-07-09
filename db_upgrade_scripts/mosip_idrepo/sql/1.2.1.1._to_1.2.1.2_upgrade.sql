ALTER TABLE idrepo.card_detail
DROP CONSTRAINT pk_nind;

ALTER TABLE idrepo.card_detail
ADD CONSTRAINT pk_nind PRIMARY KEY (nin,cr_dtimes);
