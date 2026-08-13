package uk.gov.justice.digital.hmpps.prisonregister.dsl

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.prisonregister.model.AccessibleAccess
import uk.gov.justice.digital.hmpps.prisonregister.model.Agency
import uk.gov.justice.digital.hmpps.prisonregister.model.AgencyAddress
import uk.gov.justice.digital.hmpps.prisonregister.model.AgencyRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.AgencyType
import uk.gov.justice.digital.hmpps.prisonregister.model.AreaRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.EmailAddress
import uk.gov.justice.digital.hmpps.prisonregister.model.LocalAuthorityRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PayrollRegionRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PhoneNumber
import uk.gov.justice.digital.hmpps.prisonregister.model.RegionRepository
import java.time.LocalDate

@DslMarker
annotation class AgencyDslMarker

@AgencyDslMarker
@Component
class AgencyBuilder(
  private val areaRepository: AreaRepository,
  private val regionRepository: RegionRepository,
  private val payrollRegionRepository: PayrollRegionRepository,
  private val localAuthorityRepository: LocalAuthorityRepository,
  private val agencyRepository: AgencyRepository,
  private val addressBuilder: AgencyAddressBuilder,
  private val phoneBuilder: PhoneNumberBuilder,
  private val emailBuilder: EmailAddressBuilder,
) {
  lateinit var agency: Agency

  fun build(
    agencyId: String,
    name: String,
    description: String,
    active: Boolean,
    accessibleAccess: AccessibleAccess,
    agencyType: AgencyType,
    inactiveDate: LocalDate?,
    cjitCode: String?,
    areaCode: String?,
    regionCode: String?,
    geographicalAreaCode: String?,
    payrollRegionCode: String?,
    localAuthorityCode: String?,
  ): Agency = Agency(
    agencyId = agencyId,
    name = name,
    description = description,
    active = active,
    accessibleAccess = accessibleAccess,
    agencyType = agencyType,
    inactiveDate = inactiveDate,
    cjitCode = cjitCode,
    area = areaCode?.let { areaRepository.findByIdOrNull(it) },
    region = regionCode?.let { regionRepository.findByIdOrNull(it) },
    geographicalArea = geographicalAreaCode?.let { areaRepository.findByIdOrNull(it) },
    payrollRegion = payrollRegionCode?.let { payrollRegionRepository.findByIdOrNull(it) },
    localAuthority = localAuthorityCode?.let { localAuthorityRepository.findByIdOrNull(it) },
  ).let {
    agencyRepository.saveAndFlush(it)
  }.also {
    agency = it
  }

  fun address(
    addressLine1: String? = null,
    addressLine2: String? = null,
    town: String? = null,
    county: String? = null,
    postcode: String? = null,
    country: String? = null,
  ): AgencyAddress = addressBuilder.build(
    addressLine1 = addressLine1,
    addressLine2 = addressLine2,
    town = town,
    county = county,
    postcode = postcode,
    country = country,
  ).also {
    agency.addresses.add(it)
    agencyRepository.save(agency)
  }

  fun email(
    emailAddress: String = "test@justice.gov.uk",
  ): EmailAddress = emailBuilder.build(
    emailAddress = emailAddress,
  ).also {
    agency.emailAddresses.add(it)
    agencyRepository.save(agency)
  }

  fun phoneNumber(
    phoneNumber: String = "0114 555 8989",
  ): PhoneNumber = phoneBuilder.build(
    phoneNumber = phoneNumber,
  ).also {
    agency.phoneNumbers.add(it)
    agencyRepository.save(agency)
  }
}
