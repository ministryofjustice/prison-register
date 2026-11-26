-- MAPB-1196: drop agency_type, which nothing reads.
--
-- V1_43 created it with the 21 NOMIS agency type codes (APPR, CRC, INST and so on), but no code ever read it,
-- and no table or view refers to it. The register works with its own type names instead: agency.agency_type
-- holds values such as PROBATION_CRC and CHILDREN_SECURE_HOME (OtherAgencyType), not these codes, and the
-- translation from NOMIS codes is in PublicApiService. HMPPS Data Hub does not take this table, and DPR reports
-- use NOMIS agency types only as filters on NOMIS's own tables, never as a lookup.
--
-- Reads recorded by Postgres since it began counting, on 26 November 2026: dev 2 (the last on 12 November 2026),
-- preprod 1 and prod 1 (both on the morning of 6 July 2026, two days after V1_43). Against that, court_type has
-- had over 22,000 reads in preprod and over 1,200 in prod.
--
-- V1_71 gave this table comments. They go with it. IF EXISTS in case it has already gone from an environment.

drop table if exists agency_type;
