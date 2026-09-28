package uk.gov.justice.digital.hmpps.prisonregister.dsl

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.prisonregister.model.AccessibleAccess
import uk.gov.justice.digital.hmpps.prisonregister.model.AgencyAddress
import uk.gov.justice.digital.hmpps.prisonregister.model.AreaRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.EmailAddress
import uk.gov.justice.digital.hmpps.prisonregister.model.LocalAuthorityRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.OtherAgency
import uk.gov.justice.digital.hmpps.prisonregister.model.OtherAgencyRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.OtherAgencyType
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
  private val otherAgencyRepository: OtherAgencyRepository,
  private val addressBuilder: AgencyAddressBuilder,
  private val phoneBuilder: PhoneNumberBuilder,
  private val emailBuilder: EmailAddressBuilder,
) {
  lateinit var otherAgency: OtherAgency

  fun build(
    agencyId: String,
    name: String,
    description: String,
    active: Boolean,
    accessibleAccess: AccessibleAccess,
    otherAgencyType: OtherAgencyType,
    inactiveDate: LocalDate?,
    cjitCode: String?,
    areaCode: String?,
    regionCode: String?,
    geographicalAreaCode: String?,
    payrollRegionCode: String?,
    localAuthorityCode: String?,
  ): OtherAgency = OtherAgency(
    agencyId = agencyId,
    name = name,
    description = description,
    active = active,
    accessibleAccess = accessibleAccess,
    otherAgencyType = otherAgencyType,
    inactiveDate = inactiveDate,
    cjitCode = cjitCode,
    area = areaCode?.let { areaRepository.findByIdOrNull(it) },
    region = regionCode?.let { regionRepository.findByIdOrNull(it) },
    geographicalArea = geographicalAreaCode?.let { areaRepository.findByIdOrNull(it) },
    payrollRegion = payrollRegionCode?.let { payrollRegionRepository.findByIdOrNull(it) },
    localAuthority = localAuthorityCode?.let { localAuthorityRepository.findByIdOrNull(it) },
  ).let {
    otherAgencyRepository.saveAndFlush(it)
  }.also {
    otherAgency = it
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
    otherAgency.addresses.add(it)
    otherAgencyRepository.save(otherAgency)
  }

  fun email(
    emailAddress: String = "test@justice.gov.uk",
  ): EmailAddress = emailBuilder.build(
    emailAddress = emailAddress,
  ).also {
    otherAgency.emailAddresses.add(it)
    otherAgencyRepository.save(otherAgency)
  }

  fun phoneNumber(
    phoneNumber: String = "0114 555 8989",
  ): PhoneNumber = phoneBuilder.build(
    phoneNumber = phoneNumber,
  ).also {
    otherAgency.phoneNumbers.add(it)
    otherAgencyRepository.save(otherAgency)
  }
}
