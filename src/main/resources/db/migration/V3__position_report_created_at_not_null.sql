UPDATE position_report SET created_at = NOW() WHERE created_at IS NULL;
ALTER TABLE position_report ALTER COLUMN created_at SET NOT NULL;
