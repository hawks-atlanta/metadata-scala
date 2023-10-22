ALTER TABLE files
DROP CONSTRAINT IF EXISTS files_parent_uuid_fkey,
ADD CONSTRAINT files_parent_uuid_fkey
FOREIGN KEY (parent_uuid) REFERENCES files(uuid) ON DELETE CASCADE;

ALTER TABLE files
DROP CONSTRAINT IF EXISTS files_archive_uuid_fkey,
ADD CONSTRAINT files_archive_uuid_fkey
FOREIGN KEY (archive_uuid) REFERENCES archives(uuid) ON DELETE CASCADE;

ALTER TABLE shared_files
DROP CONSTRAINT IF EXISTS shared_files_file_uuid_fkey,
ADD CONSTRAINT shared_files_file_uuid_fkey
FOREIGN KEY (file_uuid) REFERENCES files(uuid) ON DELETE CASCADE;