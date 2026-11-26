-- MAPB-1196: drop the two archive tables, which nothing reads.
--
-- They hold the offender management unit and video link conferencing centre email addresses from before
-- prison contact details were restructured. V1_28 copied every address in them into contact_details (as the
-- OFFENDER_MANAGEMENT_UNIT and VIDEOLINK_CONFERENCING_CENTRE departments), where they have been maintained
-- since, and V1_42 then renamed the originals to *_archive. No code reads them.
--
-- Rows on 26 November 2026 (offender management unit, video link conferencing centre): dev 3 and 14,
-- preprod 107 and 14, prod 107 and 14.
--
-- IF EXISTS in case either has already gone from an environment. No CASCADE: nothing depends on them.

drop table if exists offender_management_unit_archive;
drop table if exists videolink_conferencing_centre_archive;
