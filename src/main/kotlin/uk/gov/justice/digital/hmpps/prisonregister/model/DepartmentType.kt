package uk.gov.justice.digital.hmpps.prisonregister.model

import uk.gov.justice.digital.hmpps.prisonregister.exceptions.UnsupportedDepartmentTypeException

enum class DepartmentType {
  SOCIAL_VISIT,
  VIDEO_LINK_CONFERENCING,
  OFFENDER_MANAGEMENT_UNIT,
  ;

  val pathVariable: String = this.name.lowercase().replace("_", "-")

  companion object {
    fun getFromPathVariable(pathVariable: String): DepartmentType {
      return entries.firstOrNull { it.pathVariable == pathVariable } ?: throw UnsupportedDepartmentTypeException(pathVariable)
    }
  }
}
