package uk.gov.justice.digital.hmpps.prisonregister.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prisonregister.model.CourtRepository
import uk.gov.justice.digital.hmpps.prisonregister.resource.AgencyAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.AgencyEmailDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.AgencyPhoneDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.CodeDescription
import uk.gov.justice.digital.hmpps.prisonregister.resource.CourtDto

@Service
@Transactional
class CourtService(
  private val courtRepository: CourtRepository,
) {
  fun findById(courtId: String): CourtDto = courtRepository.findByIdOrNull(courtId)?.let {
    CourtDto(
      courtId = it.courtId,
      courtName = it.name,
      description = it.description,
      active = it.active,
      inactiveDate = it.inactiveDate,
      cjitCode = it.cjitCode,
      area = it.area?.let { area -> CodeDescription(area.code, area.description) },
      region = it.region?.let { area -> CodeDescription(area.code, area.description) },
      courtType = CodeDescription(it.courtType.code, it.courtType.description),
      addresses = it.addresses.map { address ->
        AgencyAddressDto(
          id = address.id,
          addressLine1 = address.addressLine1,
          addressLine2 = address.addressLine2,
          town = address.town,
          county = address.county,
          postcode = address.postcode,
          country = address.country,
        )
      },
      emailAddresses = it.emailAddresses.map { emailAddress ->
        AgencyEmailDto(
          id = emailAddress.id,
          address = emailAddress.value,
        )
      },
      phoneNumbers = it.phoneNumbers.map { phoneNumber ->
        AgencyPhoneDto(
          id = phoneNumber.id,
          number = phoneNumber.value,
        )
      },
    )
  } ?: throw EntityNotFoundException("Court $courtId not found")
}
