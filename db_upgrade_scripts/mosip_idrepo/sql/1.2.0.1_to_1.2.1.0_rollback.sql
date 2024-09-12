\c mosip_idrepo sysadmin
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