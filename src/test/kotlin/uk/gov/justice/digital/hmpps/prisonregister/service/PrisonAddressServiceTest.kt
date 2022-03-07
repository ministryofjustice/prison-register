package uk.gov.justice.digital.hmpps.prisonregister.service

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.prisonregister.model.Address
import uk.gov.justice.digital.hmpps.prisonregister.model.AddressRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.Prison
import uk.gov.justice.digital.hmpps.prisonregister.resource.AddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.UpdateAddressDto
import java.util.Optional
import javax.persistence.EntityNotFoundException

class PrisonAddressServiceTest {
  private val addressRepository: AddressRepository = mock()
  private val telemetryClient: TelemetryClient = mock()

  private val prisonAddressService =
    PrisonAddressService(addressRepository, telemetryClient)

  @Nested
  inner class FindById {
    @Test
    fun `find prison address`() {
      val prison = Prison("MDI", "A Prison", active = true)
      val address = Address(
        id = 21,
        addressLine1 = "Bawtry Road",
        addressLine2 = "Hatfield Woodhouse",
        town = "Doncaster",
        county = "South Yorkshire",
        country = "England",
        postcode = "DN7 6BW",
        prison = prison
      )
      prison.addresses = listOf(address)

      whenever(addressRepository.findById(any())).thenReturn(
        Optional.of(address)
      )
      val prisonAddressDto = AddressDto(address)

      val actual = prisonAddressService.findById("MDI", 21)
      assertThat(actual).isEqualTo(prisonAddressDto)
      verify(addressRepository).findById(21)
    }

    @Test
    fun `find prison address not found`() {
      whenever(addressRepository.findById(any())).thenReturn(
        Optional.empty()
      )

      assertThatThrownBy { prisonAddressService.findById("MDI", 21) }
        .isInstanceOf(EntityNotFoundException::class.java).hasMessage("Address 21 not found")
    }

    @Test
    fun `find prison address not associated with the prison`() {
      val prison = Prison("MDI", "A Prison", active = true)
      val address = Address(
        id = 21,
        addressLine1 = "Bawtry Road",
        addressLine2 = "Hatfield Woodhouse",
        town = "Doncaster",
        county = "South Yorkshire",
        country = "England",
        postcode = "DN7 6BW",
        prison = prison
      )
      prison.addresses = listOf(address)

      whenever(addressRepository.findById(any())).thenReturn(
        Optional.of(address)
      )

      assertThatThrownBy { prisonAddressService.findById("PDI", 21) }
        .isInstanceOf(EntityNotFoundException::class.java).hasMessage("Address 21 not in prison PDI")
    }
  }

  @Nested
  inner class MaintainPrisonAddress {

    @Test
    fun `try to update a prison address that doesn't exist`() {
      whenever(addressRepository.findById(any())).thenReturn(
        Optional.empty()
      )

      assertThrows(EntityNotFoundException::class.java) {
        prisonAddressService.updateAddress(
          "MDI", 12,
          UpdateAddressDto(
            "Line1", "line2", "town", "county",
            "postcode", "country"
          )
        )
      }
      verify(addressRepository).findById(12)
      verifyNoInteractions(telemetryClient)
    }

    @Test
    fun `try to update a prison address that doesn't belong to the prison`() {
      val prison = Prison("MDI", "A Prison", active = true)
      val address = Address(
        id = 21,
        addressLine1 = "Bawtry Road",
        addressLine2 = "Hatfield Woodhouse",
        town = "Doncaster",
        county = "South Yorkshire",
        country = "England",
        postcode = "DN7 6BW",
        prison = prison
      )
      prison.addresses = listOf(address)

      whenever(addressRepository.findById(any())).thenReturn(
        Optional.of(address)
      )

      assertThrows(EntityNotFoundException::class.java) {
        prisonAddressService.updateAddress(
          "PDI", 21,
          UpdateAddressDto(
            "Line1", "line2", "town", "county",
            "postcode", "country"
          )
        )
      }
      verify(addressRepository).findById(21)
      verifyNoInteractions(telemetryClient)
    }

    @Test
    fun `update a prison address`() {
      val prison = Prison("MDI", "A Prison", active = true)
      val address = Address(
        id = 21,
        addressLine1 = "Bawtry Road",
        addressLine2 = "Hatfield Woodhouse",
        town = "Doncaster",
        county = "South Yorkshire",
        country = "England",
        postcode = "DN7 6BW",
        prison = prison
      )
      prison.addresses = listOf(address)

      whenever(addressRepository.findById(any())).thenReturn(
        Optional.of(address)
      )

      val updatedAddress =
        prisonAddressService.updateAddress(
          "MDI", 21,
          UpdateAddressDto(
            "Line1", "line2", "town", "county",
            "postcode", "country"
          )
        )

      assertThat(updatedAddress).isEqualTo(AddressDto(address))
      verify(addressRepository).findById(21)
      verify(telemetryClient).trackEvent(eq("prison-register-address-update"), any(), isNull())
    }
  }
}
