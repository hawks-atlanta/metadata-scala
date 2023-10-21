ALTER TABLE files
DROP CONSTRAINT IF EXISTS files_parent_uuid_fkey,
ADD CONSTRAINT files_parent_uuid_fkey
FOREIGN KEY (parent_uuid) REFERENCES files(uuid) ON DELETE CASCADE;

ALTER TABLE files
DROP CONSTRAINT IF EXISTS files_uuid_fkey,
ADD CONSTRAINT files_uuid_fkey
FOREIGN KEY (uuid) REFERENCES files(uuid) ON DELETE CASCADE;