package uk.gov.justice.digital.hmpps.prisonregister.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prisonregister.model.AgencyAddress
import uk.gov.justice.digital.hmpps.prisonregister.model.EmailAddress
import uk.gov.justice.digital.hmpps.prisonregister.model.PhoneNumber
import uk.gov.justice.digital.hmpps.prisonregister.resource.AgencyId
import uk.gov.justice.digital.hmpps.prisonregister.resource.AgencyIdsResponse
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyEmailDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyPhoneDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyResponse
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyType

@Service
@Transactional
class LegacySyncService(val courtService: CourtService, val hospitalService: HospitalService, val probationOfficeService: ProbationOfficeService, val approvedPremiseService: ApprovedPremiseService, val policeCustodySuiteService: PoliceCustodySuiteService, val agencyService: AgencyService) {
  fun createOrUpdateAgency(agencyId: String, agencyDto: LegacyAgencyDto): LegacyAgencyResponse = when (agencyDto.agencyType) {
    LegacyAgencyType.COURT -> courtService.createOrUpdateCourtFromLegacyData(agencyId, agencyDto)
    LegacyAgencyType.HOSPITAL -> hospitalService.createOrUpdateHospitalFromLegacyData(agencyId, agencyDto, highSecurity = false)
    LegacyAgencyType.SECURE_HOSPITAL -> hospitalService.createOrUpdateHospitalFromLegacyData(agencyId, agencyDto, highSecurity = true)
    LegacyAgencyType.PROBATION_OFFICE -> probationOfficeService.createOrUpdateProbationOfficeFromLegacyData(agencyId, agencyDto)
    LegacyAgencyType.APPROVED_PREMISE -> approvedPremiseService.createOrUpdateApprovedPremiseFromLegacyData(agencyId, agencyDto)
    LegacyAgencyType.POLICE_CUSTODY_SUITE -> policeCustodySuiteService.createOrUpdatePoliceCustodySuiteFromLegacyData(agencyId, agencyDto)
    else -> agencyService.createOrUpdateAgencyFromLegacyData(agencyId, agencyDto)
  }

  fun deleteAll() {
    courtService.deleteAll()
    hospitalService.deleteAll()
    probationOfficeService.deleteAll()
    approvedPremiseService.deleteAll()
    policeCustodySuiteService.deleteAll()
    agencyService.deleteAll()
  }

  fun getAllIds(): AgencyIdsResponse {
    val ids = (
      courtService.getAllIds() +
        hospitalService.getAllIds() +
        probationOfficeService.getAllIds() +
        approvedPremiseService.getAllIds() +
        policeCustodySuiteService.getAllIds() +
        agencyService.getAllIds()
      )
      .sorted()
      .map { AgencyId(it) }

    return AgencyIdsResponse(ids)
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
