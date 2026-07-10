package uk.gov.justice.digital.hmpps.prisonregister.dsl

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.prisonregister.model.AgencyAddress
import uk.gov.justice.digital.hmpps.prisonregister.model.AreaRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.Court
import uk.gov.justice.digital.hmpps.prisonregister.model.CourtRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.CourtTypeRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.EmailAddress
import uk.gov.justice.digital.hmpps.prisonregister.model.PhoneNumber
import uk.gov.justice.digital.hmpps.prisonregister.model.RegionRepository
import java.time.LocalDate

@DslMarker
annotation class CourtDslMarker

@CourtDslMarker
@Component
class CourtBuilder(
  private val courtTypeRepository: CourtTypeRepository,
  private val areaRepository: AreaRepository,
  private val regionRepository: RegionRepository,
  private val courtRepository: CourtRepository,
  private val addressBuilder: AgencyAddressBuilder,
  private val phoneBuilder: PhoneNumberBuilder,
  private val emailBuilder: EmailAddressBuilder,
) {
  lateinit var court: Court
  fun build(
    courtId: String,
    name: String,
    description: String,
    active: Boolean,
    inactiveDate: LocalDate?,
    courtTypeCode: String,
    cjitCode: String?,
    areaCode: String?,
    regionCode: String?,
  ): Court = Court(
    courtId = courtId,
    name = name,
    description = description,
    active = active,
    inactiveDate = inactiveDate,
    courtType = courtTypeRepository.findByIdOrNull(courtTypeCode) ?: throw RuntimeException("Court type $courtTypeCode not found"),
    cjitCode = cjitCode,
    area = areaCode?.let { areaRepository.findByIdOrNull(areaCode) },
    region = regionCode?.let { regionRepository.findByIdOrNull(regionCode) },
  ).let {
    courtRepository.saveAndFlush(it)
  }.also {
    court = it
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
    court.addresses.add(it)
    courtRepository.save(court)
  }
  fun email(
    emailAddress: String = "test@justice.gov.uk",
  ): EmailAddress = emailBuilder.build(
    emailAddress = emailAddress,
  ).also {
    court.emailAddresses.add(it)
    courtRepository.save(court)
  }
  fun phoneNumber(
    phoneNumber: String = "0114 555 8989",
  ): PhoneNumber = phoneBuilder.build(
    phoneNumber = phoneNumber,
  ).also {
    court.phoneNumbers.add(it)
    courtRepository.save(court)
  }
}
