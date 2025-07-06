-- Migration: Populate version history for existing files
-- Created: 2025-07-06
-- Description: This migration is handled by PHP logic in Database.php
-- to create initial versions for files that existed before version history was implemented

-- No SQL needed - handled by populateExistingFileVersions() method