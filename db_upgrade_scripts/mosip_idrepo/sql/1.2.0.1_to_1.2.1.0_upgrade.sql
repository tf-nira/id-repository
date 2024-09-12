\c mosip_idrepo sysadmin
---------------------------------------------------------------------------------------------------

ALTER TABLE idrepo.uin ADD COLUMN part1 character varying(64) DEFAULT 'ZmFsc2U=' NOT NULL;

ALTER TABLE idrepo.uin ADD COLUMN part2 character varying(64) DEFAULT 'ZmFsc2U=' NOT NULL;

ALTER TABLE idrepo.uin ADD COLUMN part3 character varying(64) DEFAULT 'ZmFsc2U=' NOT NULL;

ALTER TABLE idrepo.uin ADD COLUMN part4 character varying(64) DEFAULT 'ZmFsc2U=' NOT NULL;

ALTER TABLE idrepo.uin_h ADD COLUMN part1 character varying(64) DEFAULT 'ZmFsc2U=' NOT NULL;

ALTER TABLE idrepo.uin_h ADD COLUMN part2 character varying(64) DEFAULT 'ZmFsc2U=' NOT NULL;

ALTER TABLE idrepo.uin_h ADD COLUMN part3 character varying(64) DEFAULT 'ZmFsc2U=' NOT NULL;

ALTER TABLE idrepo.uin_h ADD COLUMN part4 character varying(64) DEFAULT 'ZmFsc2U=' NOT NULL;
