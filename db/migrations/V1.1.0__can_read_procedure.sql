CREATE OR REPLACE FUNCTION can_read(user_uuid_arg UUID, file_uuid_arg UUID)
	RETURNS BOOLEAN
	LANGUAGE PLPGSQL
	AS $$
DECLARE
	folder_parent_uuid UUID;
	is_shared BOOLEAN;
BEGIN
	-- Check if the file was directly shared with the user
	SELECT COUNT(uuid) > 0
	INTO is_shared
	FROM shared_files
	WHERE
		shared_files.file_uuid = file_uuid_arg AND
		shared_files.user_uuid = user_uuid_arg;

	IF is_shared THEN
		RETURN TRUE;
	END IF;

	-- Check if the file is contained in a directory shared with the user
	SELECT files.parent_uuid
	INTO folder_parent_uuid
	FROM files
	WHERE
		files.uuid = file_uuid_arg;

	IF folder_parent_uuid IS NULL THEN
		RETURN FALSE;
	ELSE
		RETURN can_read(parent_uuid, user_uuid);
	END IF;
END $$
;