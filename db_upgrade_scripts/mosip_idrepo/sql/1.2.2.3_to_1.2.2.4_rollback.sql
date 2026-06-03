-- Rollback: Remove 'remark' column from idrepo.uin and idrepo.uin_h tables.

ALTER TABLE idrepo.uin DROP COLUMN IF EXISTS remark;
ALTER TABLE idrepo.uin_h DROP COLUMN IF EXISTS remark;

