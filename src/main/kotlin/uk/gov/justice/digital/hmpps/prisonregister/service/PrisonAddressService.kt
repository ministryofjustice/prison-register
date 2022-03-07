package uk.gov.justice.digital.hmpps.prisonregister.service

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prisonregister.model.Address
import uk.gov.justice.digital.hmpps.prisonregister.model.AddressRepository
import uk.gov.justice.digital.hmpps.prisonregister.resource.AddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.UpdateAddressDto
import javax.persistence.EntityNotFoundException

@Service
@Transactional(readOnly = true)
class PrisonAddressService(
  private val addressRepository: AddressRepository,
  private val telemetryClient: TelemetryClient
) {

  fun findById(prisonId: String, addressId: Long): AddressDto {
    return AddressDto(getAddress(addressId, prisonId))
  }

  @Transactional
  fun updateAddress(prisonId: String, addressId: Long, updateAddressRecord: UpdateAddressDto): AddressDto {
    val address = getAddress(addressId, prisonId)

    with(updateAddressRecord) {
      address.addressLine1 = addressLine1
      address.addressLine2 = addressLine2
      address.town = town
      address.county = county
      address.postcode = postcode
      address.country = country
    }
    val trackingAttributes = mapOf(
      "prisonId" to prisonId,
      "addressId" to addressId.toString(),
      "addressLine1" to updateAddressRecord.addressLine1,
      "addressLine2" to updateAddressRecord.addressLine2,
      "town" to updateAddressRecord.town,
      "county" to updateAddressRecord.county,
      "postcode" to updateAddressRecord.postcode,
      "country" to updateAddressRecord.country,
    )

    telemetryClient.trackEvent("prison-register-address-update", trackingAttributes, null)
    return AddressDto(address)
  }

  private fun getAddress(addressId: Long, prisonId: String): Address {
    val address = addressRepository.findById(addressId)
      .orElseThrow { EntityNotFoundException("Address $addressId not found") }

    if (prisonId != address.prison.prisonId) {
      throw EntityNotFoundException("Address $addressId not in prison $prisonId")
    }
    return address
  }
}
