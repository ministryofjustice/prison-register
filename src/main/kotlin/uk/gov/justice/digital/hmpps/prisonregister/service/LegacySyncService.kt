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
class LegacySyncService(val courtService: CourtService, val hospitalService: HospitalService) {
  fun createOrUpdateAgency(agencyId: String, agencyDto: LegacyAgencyDto): LegacyAgencyResponse = when (agencyDto.agencyType) {
    LegacyAgencyType.COURT -> courtService.createOrUpdateCourtFromLegacyData(agencyId, agencyDto)
    LegacyAgencyType.HOSPITAL -> hospitalService.createOrUpdateHospitalFromLegacyData(agencyId, agencyDto, highSecurity = false)
    LegacyAgencyType.SECURE_HOSPITAL -> hospitalService.createOrUpdateHospitalFromLegacyData(agencyId, agencyDto, highSecurity = true)
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

fun AgencyAddress.update(addressDto: LegacyAgencyAddressDto) {
  addressLine1 = addressDto.addressLine1
  addressLine2 = addressDto.addressLine2
  town = addressDto.town
  county = addressDto.county
  postcode = addressDto.postcode
  country = addressDto.country
}

fun LegacyAgencyPhoneDto.toAgencyPhone() = PhoneNumber(
  value = this.number,
)

fun LegacyAgencyEmailDto.toAgencyEmail() = EmailAddress(
  value = this.address,
)

fun <E, DTO> MutableList<E>.update(dtoList: List<DTO>, compare: (entity: E, dto: DTO) -> Boolean, transform: (DTO) -> E) {
  val toAdd = dtoList.filter { dto -> this.none { compare(it, dto) } }
  val toRemove = this.filter { entity -> dtoList.none { compare(entity, it) } }
  this.removeAll(toRemove)
  this.addAll(toAdd.map { transform(it) })
}

fun MutableList<EmailAddress>.updateEmailAddressFrom(emailDtoList: List<LegacyAgencyEmailDto>) = update(emailDtoList, { entity, dto -> entity.value == dto.address }) { it.toAgencyEmail() }
fun MutableList<PhoneNumber>.updatePhoneNumberFrom(phoneDtoList: List<LegacyAgencyPhoneDto>) = update(phoneDtoList, { entity, dto -> entity.value == dto.number }) { it.toAgencyPhone() }
