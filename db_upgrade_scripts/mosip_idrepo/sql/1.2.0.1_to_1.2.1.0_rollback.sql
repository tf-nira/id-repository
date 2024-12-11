\c mosip_idrepo

REASSIGN OWNED BY postgres TO sysadmin;

GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA idrepo TO sysadmin;

---------------------------------------------------------------------------------------------------
ALTER TABLE idrepo.uin
DROP COLUMN part1,
DROP COLUMN part2,
DROP COLUMN part3,
DROP COLUMN part4;

ALTER TABLE idrepo.uin_h
DROP COLUMN part1,
DROP COLUMN part2,
DROP COLUMN part3,
DROP COLUMN part4;
----------------------------------------------------------------------------------------------------
DROP TABLE IF EXISTS idrepo.card_detail;