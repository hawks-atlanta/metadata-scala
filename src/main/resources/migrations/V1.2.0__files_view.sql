-- Add a new column to separate the extension from the name
ALTER TABLE archives ADD COLUMN extension VARCHAR(16) NOT NULL DEFAULT '';

-- View to simplify the queries
CREATE OR REPLACE VIEW files_view AS
    SELECT
        files."uuid",
        files."owner_uuid",
        files."parent_uuid",
        files."volume",
        files."name",
        archives."extension",
        archives."hash_sum",
        archives."size",
        archives."ready",
        files."is_shared",
        files."created_at",
        files."updated_at"
    FROM files
LEFT JOIN archives ON files."archive_uuid" = archives."uuid";