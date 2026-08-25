package uk.gov.justice.digital.hmpps.prisonregister.dsl

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.prisonregister.model.AccessibleAccess
import uk.gov.justice.digital.hmpps.prisonregister.model.AgencyAddress
import uk.gov.justice.digital.hmpps.prisonregister.model.ApprovedPremise
import uk.gov.justice.digital.hmpps.prisonregister.model.ApprovedPremiseRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.AreaRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.EmailAddress
import uk.gov.justice.digital.hmpps.prisonregister.model.LocalAuthorityRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PayrollRegionRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PhoneNumber
import uk.gov.justice.digital.hmpps.prisonregister.model.RegionRepository
import java.time.LocalDate

@DslMarker
annotation class ApprovedPremiseDslMarker

@ApprovedPremiseDslMarker
@Component
class ApprovedPremiseBuilder(
  private val areaRepository: AreaRepository,
  private val regionRepository: RegionRepository,
  private val payrollRegionRepository: PayrollRegionRepository,
  private val localAuthorityRepository: LocalAuthorityRepository,
  private val approvedPremiseRepository: ApprovedPremiseRepository,
  private val addressBuilder: AgencyAddressBuilder,
  private val phoneBuilder: PhoneNumberBuilder,
  private val emailBuilder: EmailAddressBuilder,
) {
  lateinit var approvedPremise: ApprovedPremise

  fun build(
    approvedPremiseId: String,
    name: String,
    description: String,
    contact: String?,
    active: Boolean,
    accessibleAccess: AccessibleAccess,
    inactiveDate: LocalDate?,
    cjitCode: String?,
    areaCode: String?,
    regionCode: String?,
    geographicalAreaCode: String?,
    payrollRegionCode: String?,
    localAuthorityCode: String?,
  ): ApprovedPremise = ApprovedPremise(
    approvedPremiseId = approvedPremiseId,
    name = name,
    description = description,
    contact = contact,
    active = active,
    accessibleAccess = accessibleAccess,
    inactiveDate = inactiveDate,
    cjitCode = cjitCode,
    area = areaCode?.let { areaRepository.findByIdOrNull(it) },
    region = regionCode?.let { regionRepository.findByIdOrNull(it) },
    geographicalArea = geographicalAreaCode?.let { areaRepository.findByIdOrNull(it) },
    payrollRegion = payrollRegionCode?.let { payrollRegionRepository.findByIdOrNull(it) },
    localAuthority = localAuthorityCode?.let { localAuthorityRepository.findByIdOrNull(it) },
  ).let {
    approvedPremiseRepository.saveAndFlush(it)
  }.also {
    approvedPremise = it
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
    approvedPremise.addresses.add(it)
    approvedPremiseRepository.save(approvedPremise)
  }

  fun email(
    emailAddress: String = "test@justice.gov.uk",
  ): EmailAddress = emailBuilder.build(
    emailAddress = emailAddress,
  ).also {
    approvedPremise.emailAddresses.add(it)
    approvedPremiseRepository.save(approvedPremise)
  }

  fun phoneNumber(
    phoneNumber: String = "0114 555 8989",
  ): PhoneNumber = phoneBuilder.build(
    phoneNumber = phoneNumber,
  ).also {
    approvedPremise.phoneNumbers.add(it)
    approvedPremiseRepository.save(approvedPremise)
  }
}
