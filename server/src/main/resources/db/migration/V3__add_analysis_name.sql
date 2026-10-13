ALTER TABLE analyses
    ADD COLUMN name VARCHAR(120);

UPDATE analyses
SET name = 'Tahlil ' || LEFT(id::text, 8)
WHERE name IS NULL;

ALTER TABLE analyses
    ALTER COLUMN name SET NOT NULL;

ALTER TABLE analyses
    ADD CONSTRAINT ck_analyses_name_not_blank
    CHECK (length(trim(name)) > 0);