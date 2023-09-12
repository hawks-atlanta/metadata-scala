CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Tables
CREATE TABLE IF NOT EXISTS archives (
    "uuid" UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    "hash_sum" VARCHAR(64) NOT NULL,
    "size" BIGINT NOT NULL,
    "ready" BOOLEAN NOT NULL DEFAULT FALSE,
    "created_at" TIMESTAMP NOT NULL DEFAULT NOW(),
    "updated_at" TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT "archive_size_positive" CHECK ("size" > 0)
);

CREATE TABLE IF NOT EXISTS files (
    "uuid" UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    "owner_uuid" UUID NOT NULL,
    "parent_uuid" UUID DEFAULT NULL REFERENCES files("uuid"),
    "archive_uuid" UUID DEFAULT NULL REFERENCES archives("uuid"),
    "volume" VARCHAR(32) DEFAULT NULL,
    "name" VARCHAR(128) NOT NULL,
    "is_shared" BOOLEAN NOT NULL DEFAULT FALSE,
    "created_at" TIMESTAMP NOT NULL DEFAULT NOW(),
    "updated_at" TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS shared_files (
    "uuid" UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    "file_uuid" UUID NOT NULL REFERENCES files("uuid"),
    "user_uuid" UUID NOT NULL
);

-- Indexes
CREATE INDEX IF NOT EXISTS "files_owner_uuid_index" ON files ("owner_uuid");
CREATE INDEX IF NOT EXISTS "files_parent_uuid_index" ON files ("parent_uuid");
CREATE UNIQUE INDEX IF NOT EXISTS "files_unique_triplet_index" ON files ("owner_uuid", "parent_uuid", "name");
CREATE UNIQUE INDEX IF NOT EXISTS "shared_files_unique_tuple_index" ON shared_files ("file_uuid", "user_uuid");

-- Triggers
CREATE OR REPLACE FUNCTION "update_updated_at_column"()
    RETURNS TRIGGER
    LANGUAGE PLPGSQL
    AS $$
BEGIN
    NEW."updated_at" = NOW();
    RETURN NEW;
END $$
;

CREATE OR REPLACE TRIGGER "archives_updated_at_trigger"
    BEFORE UPDATE ON archives
    FOR EACH ROW
    EXECUTE PROCEDURE "update_updated_at_column"();

CREATE OR REPLACE TRIGGER "files_updated_at_trigger"
    BEFORE UPDATE ON files
    FOR EACH ROW
    EXECUTE PROCEDURE "update_updated_at_column"();