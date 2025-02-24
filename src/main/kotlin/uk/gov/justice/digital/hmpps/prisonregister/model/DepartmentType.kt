package uk.gov.justice.digital.hmpps.prisonregister.model

enum class DepartmentType {
  PRISON,
  SOCIAL_VISIT,
  VIDEOLINK_CONFERENCING_CENTRE,
  OFFENDER_MANAGEMENT_UNIT,
  ;

  fun toMessage(): String = this.name.lowercase().replace("_", " ")
}
