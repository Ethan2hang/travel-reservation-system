-- =====================================================================
-- One-shot reload: drops travelres and rebuilds it with full demo data.
--
-- Run from the project root in PowerShell:
--   Get-Content db\reload-all.sql | mysql -u root -p
-- Or from cmd.exe:
--   mysql -u root -p < db\reload-all.sql
--
-- This file does NOT itself contain the schema/seed; it sources them in
-- order so a single command leaves you with a fully demo-ready database.
-- =====================================================================

SOURCE db/schema.sql;
SOURCE db/seed-flights.sql;

-- Confirm
SELECT 'travelres' AS database_loaded;
SELECT COUNT(*) AS airlines  FROM Airline;
SELECT COUNT(*) AS airports  FROM Airport;
SELECT COUNT(*) AS aircraft  FROM Aircraft;
SELECT COUNT(*) AS flights   FROM Flight;
SELECT COUNT(*) AS customers FROM Customer;
SELECT COUNT(*) AS employees FROM Employee;
