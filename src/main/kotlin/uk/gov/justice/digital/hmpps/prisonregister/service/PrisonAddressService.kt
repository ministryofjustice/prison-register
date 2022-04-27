package uk.gov.justice.digital.hmpps.prisonregister.service

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prisonregister.model.Address
import uk.gov.justice.digital.hmpps.prisonregister.model.AddressRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonRepository
import uk.gov.justice.digital.hmpps.prisonregister.resource.AddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.UpdateAddressDto
import javax.persistence.EntityNotFoundException

@Service
@Transactional(readOnly = true)
class PrisonAddressService(
  private val addressRepository: AddressRepository,
  private val prisonRepository: PrisonRepository,
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

    recordPrisonAddressEditEvent("prison-register-address-update", prisonId, addressId.toString(), updateAddressRecord)
    return AddressDto(address)
  }

  @Transactional
  fun addAddress(prisonId: String, additionalAddress: UpdateAddressDto): AddressDto {
    val prison = prisonRepository.findById(prisonId)
      .orElseThrow { EntityNotFoundException("Prison $prisonId not found") }

    with(additionalAddress) {
      val address = addressRepository.save(
        Address(
          addressLine1 = addressLine1,
          addressLine2 = addressLine2,
          town = town,
          county = county,
          postcode = postcode,
          country = country,
          prison = prison
        )
      )

      recordPrisonAddressEditEvent("prison-register-address-add", prisonId, address.id?.toString(), additionalAddress)
      return AddressDto(address)
    }
  }

  private fun recordPrisonAddressEditEvent(eventIdentifier: String, prisonId: String, addressId: String?, addressDetails: UpdateAddressDto) {
    with(addressDetails) {
      val trackingAttributes = mapOf(
        "prisonId" to prisonId,
        "addressId" to addressId,
        "addressLine1" to addressLine1,
        "addressLine2" to addressLine2,
        "town" to town,
        "county" to county,
        "postcode" to postcode,
        "country" to country,
      )

      telemetryClient.trackEvent(eventIdentifier, trackingAttributes, null)
    }
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
