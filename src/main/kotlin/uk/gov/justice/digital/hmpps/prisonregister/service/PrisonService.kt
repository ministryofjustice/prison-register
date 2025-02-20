package uk.gov.justice.digital.hmpps.prisonregister.service

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.persistence.EntityExistsException
import jakarta.persistence.EntityNotFoundException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.PrisonNameDto

const val CLIENT_CAN_MAINTAIN_PRISON_DETAILS = "hasAnyRole('ROLE_MAINTAIN_REF_DATA','ROLE_MAINTAIN_PRISON_DATA') and hasAuthority('SCOPE_write')"

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

  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

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
    val prison = prisonRepository.findOneByGpPractice(gpPracticeCode)
      ?: throw EntityNotFoundException("Prison with gp practice $gpPracticeCode not found")
    return GpDto(prison)
  }

  fun findAll(): List<PrisonDto> = prisonRepository.findAll().map { PrisonDto(it) }

  fun findByPrisonFilter(
    active: Boolean? = null,
    lthse: Boolean? = null,
    textSearch: String? = null,
    genders: List<Gender>? = listOf(),
    prisonTypeCodes: List<Type>? = listOf(),
  ): List<PrisonDto> =
    prisonRepository.findAll(PrisonFilter(active, lthse, textSearch, genders, prisonTypeCodes)).map { PrisonDto(it) }

  @Transactional
  fun insertPrison(prisonInsertRecord: InsertPrisonDto): String {
    if (prisonRepository.findById(prisonInsertRecord.prisonId).isPresent) {
      throw EntityExistsException("Prison $prisonInsertRecord.prisonId already exists")
    }

    with(prisonInsertRecord) {
      val prison = Prison(
        prisonId = prisonId,
        name = prisonName,
        prisonNameInWelsh = prisonNameInWelsh,
        active = true,
        male = male,
        female = female,
        contracted = contracted,
        lthse = lthse,
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
      prison.prisonNameInWelsh = prisonNameInWelsh
      prison.male = male
      prison.female = female
      prison.contracted = contracted
      prison.lthse = lthse

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
    LOG.debug("Enter getEmailAddress $prisonId / ${departmentType.toMessage()}")
    return contactDetailsRepository.getEmailAddressByPrisonIdAndDepartment(prisonId, departmentType)
  }

  @Transactional(readOnly = true)
  fun getContactDetails(prisonId: String, type: DepartmentType): ContactDetailsDto {
    val contactDetails = contactDetailsRepository.getByPrisonIdAndType(prisonId, type)
    return contactDetails?.let {
      ContactDetailsDto(contactDetails)
    } ?: throw ContactDetailsNotFoundException(
      prisonId,
      type,
    )
  }

  @Transactional
  @PreAuthorize(CLIENT_CAN_MAINTAIN_PRISON_DETAILS)
  fun setEmailAddress(prisonId: String, newEmailAddress: String, departmentType: DepartmentType): SetOutcome {
    LOG.debug("Enter setEmailAddress $prisonId / ${departmentType.toMessage()}")

    val contactDetails = contactDetailsRepository.getByPrisonIdAndType(prisonId, departmentType)
    if (contactDetails == null) {
      prisonRepository.getReferenceById(prisonId) ?: throw EntityNotFoundException()
      val persistedEmailAddress = createOrGetEmailAddress(newEmailAddress)
      contactDetailsRepository.save(ContactDetails(prisonId, departmentType, persistedEmailAddress))
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
    contactDetailsRepository.save(contactDetails)

    oldEmailAddress?.let {
      if (contactDetailsRepository.isEmailOrphaned(oldEmailAddress)) {
        emailAddressRepository.delete(oldEmailAddress)
      }
    }
  }

  @Transactional
  @PreAuthorize(CLIENT_CAN_MAINTAIN_PRISON_DETAILS)
  fun deleteEmailAddress(prisonId: String, departmentType: DepartmentType, throwNotFound: Boolean = false) {
    LOG.debug("Enter deleteEmailAddress $prisonId / ${departmentType.toMessage()}")

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
    val emailEntity = emailAddressRepository.getByValue(newEmailAddress)
    return emailEntity?.let {
      emailEntity
    } ?: emailAddressRepository.save(EmailAddress(newEmailAddress))
  }

  private fun createOrGetPhoneNumber(
    newPhoneNumber: String,
  ): PhoneNumber {
    val phoneNumberEntity = phoneNumberRepository.getByValue(newPhoneNumber)
    return phoneNumberEntity?.let {
      phoneNumberEntity
    } ?: phoneNumberRepository.save(PhoneNumber(newPhoneNumber))
  }

  private fun createOrGetWebAddress(
    value: String,
  ): WebAddress {
    val entity = webAddressRepository.getByValue(value)
    return entity?.let {
      entity
    } ?: webAddressRepository.save(WebAddress(value))
  }

  @Transactional
  @PreAuthorize(CLIENT_CAN_MAINTAIN_PRISON_DETAILS)
  fun createContactDetails(prisonId: String, contactDetailsDto: ContactDetailsDto): ContactDetailsDto {
    LOG.debug("Enter createContactDetails $prisonId / ${contactDetailsDto.type.toMessage()}")

    prisonRepository.getReferenceById(prisonId) ?: throw PrisonNotFoundException(prisonId)
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
      contactDetailsDto.type,
      emailAddress = persistedEmailAddress,
      webAddress = persistedWebAddress,
      phoneNumber = persistedPhoneNumber,
    )

    return ContactDetailsDto(contactDetailsRepository.save(entity))
  }

  @Transactional
  @PreAuthorize(CLIENT_CAN_MAINTAIN_PRISON_DETAILS)
  fun updateContactDetails(prisonId: String, updateContactDetailsDto: ContactDetailsDto, removeIfNull: Boolean = true): ContactDetailsDto {
    LOG.debug("Enter updateContactDetails $prisonId / ${updateContactDetailsDto.type.toMessage()}")

    prisonRepository.getReferenceById(prisonId) ?: throw EntityNotFoundException()

    val contactDetails =
      contactDetailsRepository.getByPrisonIdAndType(prisonId, updateContactDetailsDto.type) ?: throw ContactDetailsNotFoundException(
        prisonId,
        updateContactDetailsDto.type,
      )

    if (haveContactDetailsChanged(updateContactDetailsDto, contactDetails)) {
      val originalDetails = ContactDetailsDto(contactDetails)

      updateContactDetailsDto.emailAddress?.let {
        val emailAddress = createOrGetEmailAddress(it)
        emailAddress.contactDetails.add(contactDetails)
        contactDetails.emailAddress = emailAddress
      } ?: run { if (removeIfNull) contactDetails.emailAddress = null }

      updateContactDetailsDto.webAddress?.let {
        val webAddress = createOrGetWebAddress(it)
        webAddress.contactDetails.add(contactDetails)
        contactDetails.webAddress = webAddress
      } ?: run { if (removeIfNull) contactDetails.webAddress = null }

      updateContactDetailsDto.phoneNumber?.let {
        val phoneNumber = createOrGetPhoneNumber(it)
        phoneNumber.contactDetails.add(contactDetails)
        contactDetails.phoneNumber = phoneNumber
      } ?: run { if (removeIfNull) contactDetails.phoneNumber = null }

      val persistedEntity = contactDetailsRepository.save(contactDetails)

      removeOrphanedContactDetails(originalDetails)

      return ContactDetailsDto(persistedEntity)
    }

    return ContactDetailsDto(contactDetails)
  }

  @Transactional
  @PreAuthorize(CLIENT_CAN_MAINTAIN_PRISON_DETAILS)
  fun deleteContactDetails(prisonId: String, type: DepartmentType) {
    LOG.debug("Enter deleteContactDetails $prisonId / ${type.toMessage()}")

    val contactDetails =
      contactDetailsRepository.getByPrisonIdAndType(prisonId, type) ?: throw ContactDetailsNotFoundException(
        prisonId,
        type,
      )

    contactDetailsRepository.delete(contactDetails)

    removeOrphanedContactDetails(ContactDetailsDto(contactDetails))
  }

  @Transactional(readOnly = true)
  fun getPrisonNames(active: Boolean? = null, prisonId: String? = null): List<PrisonNameDto> {
    return prisonRepository.findByActiveOrderByPrisonName(active = active)
      .filter { prisonId == null || it.prisonId == prisonId }
      .map { PrisonNameDto(it.prisonId, it.name) }
  }

  private fun removeOrphanedContactDetails(contactDetails: ContactDetailsDto) {
    contactDetails.emailAddress?.let {
      if (contactDetailsRepository.isEmailOrphaned(it)) {
        emailAddressRepository.delete(it)
      }
    }

    contactDetails.phoneNumber?.let {
      if (contactDetailsRepository.isPhoneNumberOrphaned(it)) {
        phoneNumberRepository.delete(it)
      }
    }

    contactDetails.webAddress?.let {
      if (contactDetailsRepository.isWebAddressOrphaned(it)) {
        webAddressRepository.delete(it)
      }
    }
  }

  private fun haveContactDetailsChanged(updateContactDetailsDto: ContactDetailsDto, contactDetails: ContactDetails): Boolean {
    return updateContactDetailsDto.type != contactDetails.type ||
      updateContactDetailsDto.emailAddress != contactDetails.emailAddress?.value ||
      updateContactDetailsDto.webAddress != contactDetails.webAddress?.value ||
      updateContactDetailsDto.phoneNumber != contactDetails.phoneNumber?.value
  }

  fun findPrisonsByIds(ids: List<String>): List<PrisonDto> = prisonRepository.findAllByPrisonIdIsIn(ids).map { PrisonDto(it) }
}
