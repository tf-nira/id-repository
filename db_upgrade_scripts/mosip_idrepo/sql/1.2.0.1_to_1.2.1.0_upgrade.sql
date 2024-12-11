\c mosip_idrepo

REASSIGN OWNED BY sysadmin TO postgres;

REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA idrepo FROM idrepouser;

REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA idrepo FROM sysadmin;

GRANT SELECT, INSERT, REFERENCES, UPDATE, DELETE ON ALL TABLES IN SCHEMA idrepo TO idrepouser;

GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA idrepo TO postgres;
---------------------------------------------------------------------------------------------------

ALTER TABLE idrepo.uin ADD COLUMN part1 character varying(64) DEFAULT 'ZmFsc2U=' NOT NULL;

ALTER TABLE idrepo.uin ADD COLUMN part2 character varying(64) DEFAULT 'ZmFsc2U=' NOT NULL;

ALTER TABLE idrepo.uin ADD COLUMN part3 character varying(64) DEFAULT 'ZmFsc2U=' NOT NULL;

ALTER TABLE idrepo.uin ADD COLUMN part4 character varying(64) DEFAULT 'ZmFsc2U=' NOT NULL;

ALTER TABLE idrepo.uin_h ADD COLUMN part1 character varying(64) DEFAULT 'ZmFsc2U=' NOT NULL;

ALTER TABLE idrepo.uin_h ADD COLUMN part2 character varying(64) DEFAULT 'ZmFsc2U=' NOT NULL;

ALTER TABLE idrepo.uin_h ADD COLUMN part3 character varying(64) DEFAULT 'ZmFsc2U=' NOT NULL;

ALTER TABLE idrepo.uin_h ADD COLUMN part4 character varying(64) DEFAULT 'ZmFsc2U=' NOT NULL;
-----------------------------------------------------------------------------------------------
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

GRANT SELECT, INSERT, TRUNCATE, REFERENCES, UPDATE, DELETE
   ON idrepo.card_detail
   TO idrepouser;
