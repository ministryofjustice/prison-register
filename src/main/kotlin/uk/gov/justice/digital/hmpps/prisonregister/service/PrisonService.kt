package uk.gov.justice.digital.hmpps.prisonregister.service

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.persistence.EntityExistsException
import jakarta.persistence.EntityNotFoundException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prisonregister.exceptions.ContactNotFoundException
import uk.gov.justice.digital.hmpps.prisonregister.model.ContactDetails
import uk.gov.justice.digital.hmpps.prisonregister.model.ContactDetailsRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType
import uk.gov.justice.digital.hmpps.prisonregister.model.EmailAddress
import uk.gov.justice.digital.hmpps.prisonregister.model.EmailAddressRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.Gender
import uk.gov.justice.digital.hmpps.prisonregister.model.Prison
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonFilter
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonType
import uk.gov.justice.digital.hmpps.prisonregister.model.SetOutcome
import uk.gov.justice.digital.hmpps.prisonregister.model.TelephoneAddress
import uk.gov.justice.digital.hmpps.prisonregister.model.TelephoneAddressRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.Type
import uk.gov.justice.digital.hmpps.prisonregister.resource.GpDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.InsertPrisonDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.PrisonDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.UpdatePrisonDto

const val CLIENT_CAN_MAINTAIN_ADDRESSES = "hasRole('MAINTAIN_REF_DATA') and hasAuthority('SCOPE_write')"

@Service
@Transactional(readOnly = true)
class PrisonService(
  private val prisonRepository: PrisonRepository,
  private val contactDetailsRepository: ContactDetailsRepository,
  private val emailAddressRepository: EmailAddressRepository,
  private val telephoneAddressRepository: TelephoneAddressRepository,
  private val telemetryClient: TelemetryClient,
) {
  fun findById(prisonId: String): PrisonDto {
    val prison =
      prisonRepository.findById(prisonId).orElseThrow { EntityNotFoundException("Prison $prisonId not found") }
    return PrisonDto(prison)
  }

  fun findPrisonAndGpPracticeById(prisonId: String): GpDto {
    val prison =
      prisonRepository.findById(prisonId).orElseThrow { EntityNotFoundException("Prison $prisonId not found") }
    return GpDto(prison)
  }

  fun findByGpPractice(gpPracticeCode: String): GpDto {
    val prison = prisonRepository.findByGpPracticeGpPracticeCode(gpPracticeCode)
      ?: throw EntityNotFoundException("Prison with gp practice $gpPracticeCode not found")
    return GpDto(prison)
  }

  fun findAll(): List<PrisonDto> = prisonRepository.findAll().map { PrisonDto(it) }

  fun findByPrisonFilter(
    active: Boolean? = null,
    textSearch: String? = null,
    genders: List<Gender>? = listOf(),
    prisonTypeCodes: List<Type>? = listOf(),
  ): List<PrisonDto> =
    prisonRepository.findAll(PrisonFilter(active, textSearch, genders, prisonTypeCodes)).map { PrisonDto(it) }

  @Transactional
  fun insertPrison(prisonInsertRecord: InsertPrisonDto): String {
    if (prisonRepository.findById(prisonInsertRecord.prisonId).isPresent) {
      throw EntityExistsException("Prison $prisonInsertRecord.prisonId already exists")
    }

    with(prisonInsertRecord) {
      val prison = Prison(
        prisonId = prisonId,
        name = prisonName,
        active = true,
        male = male,
        female = female,
        contracted = contracted,
      )

      prison.categories.retainAll(categories)
      prison.categories.addAll(categories)

      prison.prisonTypes = prisonTypes.map { PrisonType(type = it, prison = prison) }.toMutableSet()
      addresses.forEach {
        prison.addAddress(it)
      }
      telemetryClient.trackEvent("prison-register-insert", mapOf("prison" to prison.name), null)
      return prisonRepository.save(prison).prisonId
    }
  }

  @Transactional
  fun updatePrison(prisonId: String, prisonUpdateRecord: UpdatePrisonDto): PrisonDto {
    val prison = prisonRepository.findById(prisonId)
      .orElseThrow { EntityNotFoundException("Prison $prisonId not found") }

    with(prisonUpdateRecord) {
      prison.name = prisonName
      prison.active = active
      prison.male = male
      prison.female = female
      prison.contracted = contracted

      val updatedTypes = prisonTypes.map { PrisonType(type = it, prison = prison) }.toSet()
      prison.prisonTypes.retainAll(updatedTypes)
      prison.prisonTypes.addAll(updatedTypes)

      prison.categories.retainAll(categories)
      prison.categories.addAll(categories)
    }
    telemetryClient.trackEvent("prison-register-update", mapOf("prison" to prison.name), null)
    return PrisonDto(prison)
  }

  fun getEmailAddress(prisonId: String, departmentType: DepartmentType): String? {
    return contactDetailsRepository.getEmailAddressByPrisonIdAndDepartment(prisonId, departmentType)
  }

  fun getTelephoneAddress(prisonId: String, departmentType: DepartmentType): String? {
    return contactDetailsRepository.getTelephoneAddressByPrisonIdAndDepartment(prisonId, departmentType)
  }

  @Transactional
  @PreAuthorize(CLIENT_CAN_MAINTAIN_ADDRESSES)
  fun setEmailAddress(prisonId: String, newEmailAddress: String, departmentType: DepartmentType): SetOutcome {
    val contactDetails = contactDetailsRepository.getByPrisonIdAndType(prisonId, departmentType)
    if (contactDetails == null) {
      val prison = prisonRepository.getReferenceById(prisonId) ?: throw EntityNotFoundException()
      val persistedEmailAddress = createOrGetEmailAddress(newEmailAddress)
      contactDetailsRepository.saveAndFlush(ContactDetails(prisonId, prison, departmentType, persistedEmailAddress))
      return SetOutcome.CREATED
    }
    val oldEmailAddress = contactDetails.emailAddress?.value
    if (oldEmailAddress != newEmailAddress) {
      val persistedEmailAddress = createOrGetEmailAddress(newEmailAddress)

      persistedEmailAddress.contactDetails.add(contactDetails)
      contactDetails.emailAddress = persistedEmailAddress
      contactDetailsRepository.saveAndFlush(contactDetails)

      oldEmailAddress?.let {
        if (contactDetailsRepository.isEmailOrphaned(oldEmailAddress)) {
          emailAddressRepository.delete(oldEmailAddress)
        }
      }
    }
    return SetOutcome.UPDATED
  }

  @Transactional
  @PreAuthorize(CLIENT_CAN_MAINTAIN_ADDRESSES)
  fun setTelephoneAddress(prisonId: String, newTelephoneAddress: String, departmentType: DepartmentType): SetOutcome {
    val contactDetails = contactDetailsRepository.getByPrisonIdAndType(prisonId, departmentType)
    if (contactDetails == null) {
      val prison = prisonRepository.getReferenceById(prisonId) ?: throw EntityNotFoundException()
      val persistedTelephoneAddress = createOrGetTelephoneAddress(newTelephoneAddress)
      contactDetailsRepository.saveAndFlush(ContactDetails(prisonId, prison, departmentType, telephoneAddress = persistedTelephoneAddress))
      return SetOutcome.CREATED
    }
    val oldTelephoneAddress = contactDetails.emailAddress?.value
    if (oldTelephoneAddress != newTelephoneAddress) {
      val persistedTelephoneAddress = createOrGetTelephoneAddress(newTelephoneAddress)

      persistedTelephoneAddress.contactDetails.add(contactDetails)
      contactDetails.telephoneAddress = persistedTelephoneAddress
      contactDetailsRepository.saveAndFlush(contactDetails)

      oldTelephoneAddress?.let {
        if (contactDetailsRepository.isTelephoneAddressOrphaned(oldTelephoneAddress)) {
          telephoneAddressRepository.delete(oldTelephoneAddress)
        }
      }
    }
    return SetOutcome.UPDATED
  }

  @Transactional
  @PreAuthorize(CLIENT_CAN_MAINTAIN_ADDRESSES)
  fun deleteEmailAddress(prisonId: String, departmentType: DepartmentType, throwNotFound: Boolean = false) {
    val contactDetails = contactDetailsRepository.getByPrisonIdAndType(prisonId, departmentType)
    contactDetails?.let {
      it.emailAddress?.let {
        if (contactDetails.telephoneAddress == null) {
          contactDetailsRepository.delete(contactDetails)
        }
        contactDetails.emailAddress = null
        if (contactDetailsRepository.isEmailOrphaned(it.value)) {
          emailAddressRepository.delete(it.value)
          return
        }
      }
    }
    if (throwNotFound) {
      throw ContactNotFoundException(prisonId, departmentType)
    }
  }

  @Transactional
  @PreAuthorize(CLIENT_CAN_MAINTAIN_ADDRESSES)
  fun deleteTelephoneAddress(prisonId: String, departmentType: DepartmentType, throwNotFound: Boolean = false) {
    val contactDetails = contactDetailsRepository.getByPrisonIdAndType(prisonId, departmentType)
    contactDetails?.let {
      it.telephoneAddress?.let {
        if (contactDetails.emailAddress == null) {
          contactDetailsRepository.delete(contactDetails)
        }
        contactDetails.telephoneAddress = null
        if (contactDetailsRepository.isTelephoneAddressOrphaned(it.value)) {
          telephoneAddressRepository.delete(it.value)
          return
        }
      }
    }
    if (throwNotFound) {
      throw ContactNotFoundException(prisonId, departmentType)
    }
  }

  private fun createOrGetEmailAddress(
    newEmailAddress: String,
  ): EmailAddress {
    val emailEntity = emailAddressRepository.getEmailAddress(newEmailAddress)
    return emailEntity?.let {
      emailEntity
    } ?: emailAddressRepository.saveAndFlush(EmailAddress(newEmailAddress))
  }

  private fun createOrGetTelephoneAddress(
    newTelephoneAddress: String,
  ): TelephoneAddress {
    val telephoneAddressEntity = telephoneAddressRepository.getTelephoneAddress(newTelephoneAddress)
    return telephoneAddressEntity?.let {
      telephoneAddressEntity
    } ?: telephoneAddressRepository.saveAndFlush(TelephoneAddress(newTelephoneAddress))
  }
}
