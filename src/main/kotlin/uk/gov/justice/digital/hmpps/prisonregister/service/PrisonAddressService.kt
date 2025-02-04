package uk.gov.justice.digital.hmpps.prisonregister.service

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prisonregister.model.Address
import uk.gov.justice.digital.hmpps.prisonregister.model.AddressRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonRepository
import uk.gov.justice.digital.hmpps.prisonregister.resource.AddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.UpdateAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.UpdateWelshAddressDto

@Service
@Transactional(readOnly = true)
class PrisonAddressService(
  private val addressRepository: AddressRepository,
  private val prisonRepository: PrisonRepository,
  private val telemetryClient: TelemetryClient,
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

    recordPrisonAddressEditEvent("prison-register-address-update", address)
    return AddressDto(address)
  }
  @Transactional
  fun updateWelshAddress(prisonId: String, addressId: Long, updateWelshAddressDto: UpdateWelshAddressDto): AddressDto {
    val address = getAddress(addressId, prisonId)

    with(updateWelshAddressDto) {
      address.addressLine1InWelsh = addressLine1InWelsh
      address.addressLine2InWelsh = addressLine2InWelsh
      address.townInWelsh = townInWelsh
      address.countyInWelsh = countyInWelsh
      address.countryInWelsh = countryInWelsh
    }

    recordPrisonAddressEditEvent("prison-register-address-update", address)
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
          prison = prison,
        ),
      )

      recordPrisonAddressEditEvent("prison-register-address-add", address)
      return AddressDto(address)
    }
  }

  @Transactional
  fun deleteAddress(prisonId: String, addressId: Long): AddressDto {
    val address = getAddress(addressId, prisonId)
    addressRepository.delete(address)

    recordPrisonAddressEditEvent("prison-register-address-delete", address)
    return AddressDto(address)
  }

  private fun recordPrisonAddressEditEvent(eventIdentifier: String, addressDetails: Address) {
    with(addressDetails) {
      val trackingAttributes = mapOf(
        "prisonId" to prison.prisonId,
        "addressId" to id.toString(),
        "addressLine1" to addressLine1,
        "addressLine2" to addressLine2,
        "town" to town,
        "county" to county,
        "postcode" to postcode,
        "country" to country,
        "addressLine1InWelsh" to addressLine1InWelsh,
        "addressLine2InWelsh" to addressLine2InWelsh,
        "townInWelsh" to townInWelsh,
        "countyInWelsh" to countyInWelsh,
        "countryInWelsh" to countryInWelsh,
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
