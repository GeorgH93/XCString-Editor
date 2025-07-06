-- Migration: Populate version history for existing files
-- Created: 2025-07-06
-- Description: Create initial versions for files that existed before version history was implemented

-- This migration will be handled by a special PHP script since it requires
-- logic to generate hashes and handle existing file data
-- The actual data population is done in the Database.php migration runner