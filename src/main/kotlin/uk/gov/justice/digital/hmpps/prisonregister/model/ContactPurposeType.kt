package uk.gov.justice.digital.hmpps.prisonregister.model

import uk.gov.justice.digital.hmpps.prisonregister.exceptions.UnsupportedContactPurposeTypeException

enum class ContactPurposeType(val value: String) {
  SOCIAL_VISIT("social-visit"),
  VIDEO_LINK_CONFERENCING("video-link-conferencing"),
  OFFENDER_MANAGEMENT_UNIT("offender-management-unit"),
  ;

  companion object {
    fun getFromPathVariable(pathVariable: String): ContactPurposeType {
      return entries.firstOrNull { it.value == pathVariable } ?: throw UnsupportedContactPurposeTypeException(pathVariable)
    }
  }
}
