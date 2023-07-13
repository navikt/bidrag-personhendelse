
ALTER TABLE hendelsemottak ALTER COLUMN personidenter TYPE TEXT;
ALTER TABLE hendelsemottak ALTER COLUMN hendelse TYPE TEXT;


-- roll back
-- ALTER TABLE hendelsemottak ALTER COLUMN personidenter TYPE VARCHAR(255);
-- ALTER TABLE hendelsemottak ALTER COLUMN hendelse TYPE VARCHAR(5000);
-- DELETE FROM flyway_schema_history WHERE version = '2.0.5';
