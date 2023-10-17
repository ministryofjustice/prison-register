package uk.gov.justice.digital.hmpps.prisonregister.service

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.persistence.EntityExistsException
import jakarta.persistence.EntityNotFoundException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prisonregister.exceptions.ContactDetailsAlreadyExistException
import uk.gov.justice.digital.hmpps.prisonregister.exceptions.ContactDetailsNotFoundException
import uk.gov.justice.digital.hmpps.prisonregister.exceptions.PrisonNotFoundException
import uk.gov.justice.digital.hmpps.prisonregister.model.ContactDetails
import uk.gov.justice.digital.hmpps.prisonregister.model.ContactDetailsRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType
import uk.gov.justice.digital.hmpps.prisonregister.model.EmailAddress
import uk.gov.justice.digital.hmpps.prisonregister.model.EmailAddressRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.Gender
import uk.gov.justice.digital.hmpps.prisonregister.model.PhoneNumber
import uk.gov.justice.digital.hmpps.prisonregister.model.PhoneNumberRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.Prison
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonFilter
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonType
import uk.gov.justice.digital.hmpps.prisonregister.model.SetOutcome
import uk.gov.justice.digital.hmpps.prisonregister.model.SetOutcome.CREATED
import uk.gov.justice.digital.hmpps.prisonregister.model.SetOutcome.UPDATED
import uk.gov.justice.digital.hmpps.prisonregister.model.Type
import uk.gov.justice.digital.hmpps.prisonregister.model.WebAddress
import uk.gov.justice.digital.hmpps.prisonregister.model.WebAddressRepository
import uk.gov.justice.digital.hmpps.prisonregister.resource.GpDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.InsertPrisonDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.PrisonDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.UpdatePrisonDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.ContactDetailsDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.UpdateContactDetailsDto

const val CLIENT_CAN_MAINTAIN_ADDRESSES = "hasRole('MAINTAIN_REF_DATA') and hasAuthority('SCOPE_write')"

@Service
@Transactional(readOnly = true)
class PrisonService(
  private val prisonRepository: PrisonRepository,
  private val contactDetailsRepository: ContactDetailsRepository,
  private val emailAddressRepository: EmailAddressRepository,
  private val phoneNumberRepository: PhoneNumberRepository,
  private val webAddressRepository: WebAddressRepository,
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

  @Transactional(readOnly = true)
  fun getEmailAddress(prisonId: String, departmentType: DepartmentType): String? {
    return contactDetailsRepository.getEmailAddressByPrisonIdAndDepartment(prisonId, departmentType)
  }

  @Transactional(readOnly = true)
  fun get(prisonId: String, type: DepartmentType): ContactDetailsDto {
    val contactDetails = contactDetailsRepository.get(prisonId, type)
    return contactDetails?.let {
      ContactDetailsDto(contactDetails)
    } ?: throw ContactDetailsNotFoundException(
      prisonId,
      type,
    )
  }

  @Transactional
  @PreAuthorize(CLIENT_CAN_MAINTAIN_ADDRESSES)
  fun setEmailAddress(prisonId: String, newEmailAddress: String, departmentType: DepartmentType): SetOutcome {
    val contactDetails = contactDetailsRepository.getByPrisonIdAndType(prisonId, departmentType)
    if (contactDetails == null) {
      val prison = prisonRepository.getReferenceById(prisonId) ?: throw EntityNotFoundException()
      val persistedEmailAddress = createOrGetEmailAddress(newEmailAddress)
      contactDetailsRepository.saveAndFlush(ContactDetails(prisonId, prison, departmentType, persistedEmailAddress))
      return CREATED
    }
    val oldEmailAddress = contactDetails.emailAddress?.value
    if (oldEmailAddress != newEmailAddress) {
      updateEmailAddress(newEmailAddress, contactDetails, oldEmailAddress)
    }
    return UPDATED
  }

  private fun updateEmailAddress(
    newEmailAddress: String,
    contactDetails: ContactDetails,
    oldEmailAddress: String?,
  ) {
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

  @Transactional
  @PreAuthorize(CLIENT_CAN_MAINTAIN_ADDRESSES)
  fun deleteEmailAddress(prisonId: String, departmentType: DepartmentType, throwNotFound: Boolean = false) {
    contactDetailsRepository.getByPrisonIdAndType(prisonId, departmentType)?.let { contactDetails ->
      contactDetails.emailAddress?.let { emailAddressToBeDeleted ->
        contactDetails.emailAddress = null
        deleteIfEmpty(contactDetails)
        if (contactDetailsRepository.isEmailOrphaned(emailAddressToBeDeleted.value)) {
          emailAddressRepository.delete(emailAddressToBeDeleted.value)
        }
        return
      }
    }
    if (throwNotFound) {
      throw ContactDetailsNotFoundException(prisonId, departmentType)
    }
  }

  private fun deleteIfEmpty(contactDetails: ContactDetails) {
    if (isContactDetailsEmpty(contactDetails)) {
      contactDetailsRepository.delete(contactDetails)
    }
  }

  private fun isContactDetailsEmpty(contactDetails: ContactDetails): Boolean {
    return with(contactDetails) {
      webAddress == null && phoneNumber == null && emailAddress == null
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

  private fun createOrGetPhoneNumber(
    newPhoneNumber: String,
  ): PhoneNumber {
    val phoneNumberEntity = phoneNumberRepository.getPhoneNumber(newPhoneNumber)
    return phoneNumberEntity?.let {
      phoneNumberEntity
    } ?: phoneNumberRepository.saveAndFlush(PhoneNumber(newPhoneNumber))
  }

  private fun createOrGetWebAddress(
    value: String,
  ): WebAddress {
    val entity = webAddressRepository.get(value)
    return entity?.let {
      entity
    } ?: webAddressRepository.saveAndFlush(WebAddress(value))
  }

  @Transactional
  @PreAuthorize(CLIENT_CAN_MAINTAIN_ADDRESSES)
  fun createContactDetails(prisonId: String, contactDetailsDto: ContactDetailsDto): ContactDetailsDto {
    val prison = prisonRepository.getReferenceById(prisonId) ?: throw PrisonNotFoundException(prisonId)
    if (contactDetailsRepository.getByPrisonIdAndType(prisonId, contactDetailsDto.type) != null) {
      throw ContactDetailsAlreadyExistException(prisonId, contactDetailsDto.type)
    }

    val persistedEmailAddress = contactDetailsDto.emailAddress?.let {
      createOrGetEmailAddress(it)
    }
    val persistedWebAddress = contactDetailsDto.webAddress?.let {
      createOrGetWebAddress(it)
    }
    val persistedPhoneNumber = contactDetailsDto.phoneNumber?.let {
      createOrGetPhoneNumber(it)
    }

    val entity = ContactDetails(
      prisonId,
      prison,
      contactDetailsDto.type,
      emailAddress = persistedEmailAddress,
      webAddress = persistedWebAddress,
      phoneNumber = persistedPhoneNumber,
    )

    return ContactDetailsDto(contactDetailsRepository.saveAndFlush(entity))
  }

  @Transactional
  @PreAuthorize(CLIENT_CAN_MAINTAIN_ADDRESSES)
  fun updateContactDetails(prisonId: String, updateContactDetailsDto: UpdateContactDetailsDto, removeIfNull: Boolean = true): ContactDetailsDto {
    prisonRepository.getReferenceById(prisonId) ?: throw EntityNotFoundException()

    val contactDetails =
      contactDetailsRepository.getByPrisonIdAndType(prisonId, updateContactDetailsDto.type) ?: throw ContactDetailsNotFoundException(
        prisonId,
        updateContactDetailsDto.type,
      )

    if (haveContactDetailsChanged(updateContactDetailsDto, contactDetails)) {
      updateContactDetailsDto.emailAddress?.let {
        createOrGetEmailAddress(it).contactDetails.add(contactDetails)
      } ?: run { if (removeIfNull) contactDetails.emailAddress = null }

      updateContactDetailsDto.webAddress?.let {
        createOrGetWebAddress(it).contactDetails.add(contactDetails)
      } ?: run { if (removeIfNull) contactDetails.webAddress = null }

      updateContactDetailsDto.phoneNumber?.let {
        createOrGetPhoneNumber(it).contactDetails.add(contactDetails)
      } ?: run { if (removeIfNull) contactDetails.phoneNumber = null }

      val persistedEntity = contactDetailsRepository.saveAndFlush(contactDetails)
      removeOrphanedContactDetails(contactDetails)
      return ContactDetailsDto(persistedEntity)
    }

    return ContactDetailsDto(contactDetails)
  }

  @Transactional
  @PreAuthorize(CLIENT_CAN_MAINTAIN_ADDRESSES)
  fun deleteContactDetails(prisonId: String, type: DepartmentType) {
    val contactDetails =
      contactDetailsRepository.getByPrisonIdAndType(prisonId, type) ?: throw ContactDetailsNotFoundException(
        prisonId,
        type,
      )

    contactDetailsRepository.delete(contactDetails)

    removeOrphanedContactDetails(contactDetails)
  }

  private fun removeOrphanedContactDetails(contactDetails: ContactDetails) {
    contactDetails.emailAddress?.let {
      if (contactDetailsRepository.isEmailOrphaned(it.value)) {
        emailAddressRepository.delete(it.value)
      }
    }

    contactDetails.phoneNumber?.let {
      if (contactDetailsRepository.isPhoneNumberOrphaned(it.value)) {
        phoneNumberRepository.delete(it.value)
      }
    }

    contactDetails.webAddress?.let {
      if (contactDetailsRepository.isWebAddressOrphaned(it.value)) {
        emailAddressRepository.delete(it.value)
      }
    }
  }

  private fun haveContactDetailsChanged(updateContactDetailsDto: UpdateContactDetailsDto, contactDetails: ContactDetails): Boolean {
    return updateContactDetailsDto.type != contactDetails.type ||
      updateContactDetailsDto.emailAddress != contactDetails.emailAddress?.value ||
      updateContactDetailsDto.webAddress != contactDetails.webAddress?.value ||
      updateContactDetailsDto.phoneNumber != contactDetails.phoneNumber?.value
  }
}
