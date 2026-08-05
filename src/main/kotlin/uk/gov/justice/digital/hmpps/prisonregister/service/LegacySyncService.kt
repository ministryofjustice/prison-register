package uk.gov.justice.digital.hmpps.prisonregister.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prisonregister.model.AgencyAddress
import uk.gov.justice.digital.hmpps.prisonregister.model.EmailAddress
import uk.gov.justice.digital.hmpps.prisonregister.model.PhoneNumber
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyEmailDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyPhoneDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyResponse
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyType

@Service
class LegacySyncService(val courtService: CourtService) {
  fun createOrUpdateAgency(agencyId: String, agencyDto: LegacyAgencyDto): LegacyAgencyResponse = when (agencyDto.agencyType) {
    LegacyAgencyType.COURT -> courtService.createOrUpdateCourtFromLegacyData(agencyId, agencyDto)
    else -> throw IllegalArgumentException("Unsupported agency type: ${agencyDto.agencyType}")
  }
}

fun LegacyAgencyAddressDto.toAgencyAddress() = AgencyAddress(
  addressLine1 = this.addressLine1,
  addressLine2 = this.addressLine2,
  town = this.town,
  county = this.county,
  postcode = this.postcode,
  country = this.country,
)

fun LegacyAgencyPhoneDto.toAgencyPhone() = PhoneNumber(
  value = this.number,
)
fun LegacyAgencyEmailDto.toAgencyEmail() = EmailAddress(
  value = this.address,
)
