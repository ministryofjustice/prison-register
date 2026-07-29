package uk.gov.justice.digital.hmpps.prisonregister.dsl

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.prisonregister.model.AgencyAddress
import uk.gov.justice.digital.hmpps.prisonregister.model.AreaRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.Hospital
import uk.gov.justice.digital.hmpps.prisonregister.model.HospitalRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PayrollRegionRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PhoneNumber
import uk.gov.justice.digital.hmpps.prisonregister.model.RegionRepository
import java.time.LocalDate

@DslMarker
annotation class HospitalDslMarker

@HospitalDslMarker
@Component
class HospitalBuilder(
  private val areaRepository: AreaRepository,
  private val regionRepository: RegionRepository,
  private val payrollRegionRepository: PayrollRegionRepository,
  private val hospitalRepository: HospitalRepository,
  private val addressBuilder: AgencyAddressBuilder,
  private val phoneBuilder: PhoneNumberBuilder,
) {
  lateinit var hospital: Hospital
  fun build(
    hospitalId: String,
    name: String,
    description: String,
    active: Boolean,
    highSecurity: Boolean,
    inactiveDate: LocalDate?,
    cjitCode: String?,
    areaCode: String?,
    geographicalAreaCode: String?,
    regionCode: String?,
    payrollRegionCode: String?,
  ): Hospital = Hospital(
    hospitalId = hospitalId,
    name = name,
    description = description,
    active = active,
    highSecurity = highSecurity,
    inactiveDate = inactiveDate,
    cjitCode = cjitCode,
    area = areaCode?.let { areaRepository.findByIdOrNull(it) },
    region = regionCode?.let { regionRepository.findByIdOrNull(it) },
    payrollRegion = payrollRegionCode?.let { payrollRegionRepository.findByIdOrNull(it) },
    geographicalArea = geographicalAreaCode?.let { areaRepository.findByIdOrNull(it) },
  ).let {
    hospitalRepository.saveAndFlush(it)
  }.also {
    hospital = it
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
    hospital.addresses.add(it)
    hospitalRepository.save(hospital)
  }
  fun phoneNumber(
    phoneNumber: String = "0114 555 8989",
  ): PhoneNumber = phoneBuilder.build(
    phoneNumber = phoneNumber,
  ).also {
    hospital.phoneNumbers.add(it)
    hospitalRepository.save(hospital)
  }
}
