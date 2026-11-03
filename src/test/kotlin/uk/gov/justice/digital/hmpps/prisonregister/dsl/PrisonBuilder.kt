package uk.gov.justice.digital.hmpps.prisonregister.dsl

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.prisonregister.model.Address
import uk.gov.justice.digital.hmpps.prisonregister.model.AreaRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.ContactDetails
import uk.gov.justice.digital.hmpps.prisonregister.model.ContactDetailsRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType
import uk.gov.justice.digital.hmpps.prisonregister.model.EmailAddress
import uk.gov.justice.digital.hmpps.prisonregister.model.LocalAuthorityRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PayrollRegionRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PhoneNumber
import uk.gov.justice.digital.hmpps.prisonregister.model.Prison
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.RegionRepository
import java.time.LocalDate

@DslMarker
annotation class PrisonDslMarker

@PrisonDslMarker
@Component
class PrisonBuilder(
  private val areaRepository: AreaRepository,
  private val regionRepository: RegionRepository,
  private val payrollRegionRepository: PayrollRegionRepository,
  private val localAuthorityRepository: LocalAuthorityRepository,
  private val prisonRepository: PrisonRepository,
  private val contactDetailsRepository: ContactDetailsRepository,
  private val phoneBuilder: PhoneNumberBuilder,
  private val emailBuilder: EmailAddressBuilder,
) {
  lateinit var prison: Prison
  fun build(
    prisonId: String,
    name: String,
    description: String?,
    active: Boolean,
    male: Boolean,
    female: Boolean,
    contracted: Boolean,
    lthse: Boolean,
    prisonNameInWelsh: String?,
    inactiveDate: LocalDate?,
    cjitCode: String?,
    areaCode: String?,
    regionCode: String?,
    geographicalAreaCode: String?,
    payrollRegionCode: String?,
    localAuthorityCode: String?,
  ): Prison = Prison(
    prisonId = prisonId,
    name = name,
    description = description,
    active = active,
    male = male,
    female = female,
    contracted = contracted,
    lthse = lthse,
    prisonNameInWelsh = prisonNameInWelsh,
    inactiveDate = inactiveDate,
    cjitCode = cjitCode,
    area = areaCode?.let { areaRepository.findByIdOrNull(it) },
    region = regionCode?.let { regionRepository.findByIdOrNull(it) },
    geographicalArea = geographicalAreaCode?.let { areaRepository.findByIdOrNull(it) },
    payrollRegion = payrollRegionCode?.let { payrollRegionRepository.findByIdOrNull(it) },
    localAuthority = localAuthorityCode?.let { localAuthorityRepository.findByIdOrNull(it) },
  ).let {
    prisonRepository.saveAndFlush(it)
  }.also {
    prison = it
  }

  fun address(
    addressLine1: String? = null,
    addressLine2: String? = null,
    town: String,
    county: String? = null,
    postcode: String,
    country: String,
  ): Address = Address(
    addressLine1 = addressLine1,
    addressLine2 = addressLine2,
    town = town,
    county = county,
    postcode = postcode,
    country = country,
    prison = prison,
  ).also {
    (prison.addresses as MutableSet<Address>).add(it)
    prisonRepository.saveAndFlush(prison)
  }

  fun department(
    departmentType: DepartmentType,
    phoneNumber: String? = null,
    emailAddress: String? = null,
  ): ContactDetails {
    val contactDetails = getOrCreateContactDetails(departmentType)
    phoneNumber?.let { contactDetails.phoneNumber = phoneBuilder.build(it) }
    emailAddress?.let { contactDetails.emailAddress = emailBuilder.build(it) }
    return contactDetailsRepository.saveAndFlush(contactDetails)
  }

  fun email(emailAddress: String = "test@justice.gov.uk"): EmailAddress = department(
    departmentType = DepartmentType.PRISON,
    emailAddress = emailAddress,
  ).emailAddress!!

  fun phoneNumber(phoneNumber: String = "0114 555 8989"): PhoneNumber = department(
    departmentType = DepartmentType.PRISON,
    phoneNumber = phoneNumber,
  ).phoneNumber!!

  private fun getOrCreateContactDetails(departmentType: DepartmentType): ContactDetails = contactDetailsRepository.getByPrisonIdAndType(
    prison.prisonId,
    departmentType,
  ) ?: ContactDetails(prison.prisonId, departmentType)
}
