package uk.gov.justice.digital.hmpps.prisonregister.service

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prisonregister.model.AgencyAddress
import uk.gov.justice.digital.hmpps.prisonregister.model.AllAgencyRepository
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
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyUsageDto

@Service
@Transactional
class LegacySyncService(val courtService: CourtService, val hospitalService: HospitalService, val probationOfficeService: ProbationOfficeService, val approvedPremisesService: ApprovedPremisesService, val policeCustodySuiteService: PoliceCustodySuiteService, val otherAgencyService: OtherAgencyService, val allAgencyRepository: AllAgencyRepository) {
  fun createOrUpdateAgency(agencyId: String, agencyDto: LegacyAgencyDto): LegacyAgencyResponse = when (agencyDto.agencyType) {
    LegacyAgencyType.COURT -> courtService.createOrUpdateCourtFromLegacyData(agencyId, agencyDto)
    LegacyAgencyType.HOSPITAL -> hospitalService.createOrUpdateHospitalFromLegacyData(agencyId, agencyDto, highSecurity = false)
    LegacyAgencyType.SECURE_HOSPITAL -> hospitalService.createOrUpdateHospitalFromLegacyData(agencyId, agencyDto, highSecurity = true)
    LegacyAgencyType.PROBATION_OFFICE -> probationOfficeService.createOrUpdateProbationOfficeFromLegacyData(agencyId, agencyDto)
    LegacyAgencyType.APPROVED_PREMISES -> approvedPremisesService.createOrUpdateApprovedPremisesFromLegacyData(agencyId, agencyDto)
    LegacyAgencyType.POLICE_CUSTODY_SUITE -> policeCustodySuiteService.createOrUpdatePoliceCustodySuiteFromLegacyData(agencyId, agencyDto)
    else -> otherAgencyService.createOrUpdateAgencyFromLegacyData(agencyId, agencyDto)
  }

  fun deleteAll() {
    courtService.deleteAll()
    hospitalService.deleteAll()
    probationOfficeService.deleteAll()
    approvedPremisesService.deleteAll()
    policeCustodySuiteService.deleteAll()
    otherAgencyService.deleteAll()
  }

  fun getAllIds(): AgencyIdsResponse {
    val ids = (
      courtService.getAllIds() +
        hospitalService.getAllIds() +
        probationOfficeService.getAllIds() +
        approvedPremisesService.getAllIds() +
        policeCustodySuiteService.getAllIds() +
        otherAgencyService.getAllIds()
      )
      .sorted()
      .map { AgencyId(it) }

    return AgencyIdsResponse(ids)
  }

  fun getDetails(agencyId: String): LegacyAgencyDto = courtService.tryFindById(agencyId)
    ?: hospitalService.tryFindById(agencyId)
    ?: probationOfficeService.tryFindById(agencyId)
    ?: approvedPremisesService.tryFindById(agencyId)
    ?: policeCustodySuiteService.tryFindById(agencyId)
    ?: otherAgencyService.tryFindById(agencyId)
    ?: throw EntityNotFoundException("Agency $agencyId not found")

  fun findAgencyUsage(agencyId: String): LegacyAgencyUsageDto? = allAgencyRepository.findFirstByAgencyId(agencyId)?.let {
    LegacyAgencyUsageDto(agencyId = it.agencyId, name = it.name, legacyType = it.legacyType)
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
  // match entities to dtos one-to-one so duplicates are respected on both sides
  val unmatched = this.toMutableList()
  val toAdd = mutableListOf<DTO>()
  dtoList.forEach { dto ->
    val match = unmatched.indexOfFirst { compare(it, dto) }
    if (match >= 0) unmatched.removeAt(match) else toAdd.add(dto)
  }
  // identity-based removal so equal entities that were matched are kept
  this.removeAll { entity -> unmatched.any { it === entity } }
  this.addAll(toAdd.map { transform(it) })
}

fun MutableList<EmailAddress>.updateEmailAddressFrom(emailDtoList: List<LegacyAgencyEmailDto>) = update(emailDtoList, { entity, dto -> entity.value == dto.address }) { it.toAgencyEmail() }
fun MutableList<PhoneNumber>.updatePhoneNumberFrom(phoneDtoList: List<LegacyAgencyPhoneDto>) = update(phoneDtoList, { entity, dto -> entity.value == dto.number }) { it.toAgencyPhone() }
