-- -------------------------------------------------------------------------------------------------
-- Database Name: mosip_repo
-- Table Name 	: idrepo.card_detail
-- Purpose    	: card_detail: Information related to an individual card 
--           
-- Create By   	: Sowmya
-- Created Date	: 12-Dec-2024
-- 


-- object: idrepo.card_detail | type: TABLE --
-- DROP TABLE IF EXISTS idrepo.card_detail CASCADE;
CREATE TABLE idrepo.card_detail(
	nin character varying NOT NULL,
	card_number character varying(11),                             
	date_of_issuance DATE NOT NULL,
	date_of_expiry DATE,
	cr_by character varying(256) NOT NULL,
	cr_dtimes timestamp NOT NULL,
	upd_by character varying(256),
	upd_dtimes timestamp,
	CONSTRAINT pk_nind PRIMARY KEY (nin,date_of_issuance)
);
-- ddl-end --
COMMENT ON TABLE idrepo.card_detail IS 'card_detail: Card details of individual';
-- ddl-end --
COMMENT ON COLUMN idrepo.card_detail.nin IS 'NIN Hash:  Hash value of the NIN.';
-- ddl-end --
COMMENT ON COLUMN idrepo.card_detail.card_number IS 'Card Number : Card number given by print partner.';
-- ddl-end --
COMMENT ON COLUMN idrepo.card_detail.date_of_issuance IS 'Date of Issuance of Card.';
-- ddl-end --
COMMENT ON COLUMN idrepo.card_detail.date_of_expiry IS 'Date of Expiry of Card';
-- ddl-end --
COMMENT ON COLUMN idrepo.card_detail.cr_by IS 'Created By : ID or name of the user who create / insert record';
-- ddl-end --
COMMENT ON COLUMN idrepo.card_detail.cr_dtimes IS 'Created DateTimestamp : Date and Timestamp when the record is created/inserted';
-- ddl-end --
COMMENT ON COLUMN idrepo.card_detail.upd_by IS 'Updated By : ID or name of the user who update the record with new values';
-- ddl-end --
COMMENT ON COLUMN idrepo.card_detail.upd_dtimes IS 'Updated DateTimestamp : Date and Timestamp when any of the fields in the record is updated with new values.';

