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
import uk.gov.justice.digital.hmpps.prisonregister.model.Gender
import uk.gov.justice.digital.hmpps.prisonregister.model.PhoneNumber
import uk.gov.justice.digital.hmpps.prisonregister.model.Prison
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonFilter
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonType
import uk.gov.justice.digital.hmpps.prisonregister.model.SetOutcome
import uk.gov.justice.digital.hmpps.prisonregister.model.SetOutcome.CREATED
import uk.gov.justice.digital.hmpps.prisonregister.model.SetOutcome.UPDATED
import uk.gov.justice.digital.hmpps.prisonregister.model.Type
import uk.gov.justice.digital.hmpps.prisonregister.model.WebAddress
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

  fun findAll(): List<PrisonDto> = prisonRepository.findAll().map { PrisonDto(it) }

  fun findByPrisonFilter(
    active: Boolean? = null,
    lthse: Boolean? = null,
    textSearch: String? = null,
    genders: List<Gender>? = listOf(),
    prisonTypeCodes: List<Type>? = listOf(),
  ): List<PrisonDto> = prisonRepository.findAll(PrisonFilter(active, lthse, textSearch, genders, prisonTypeCodes)).map { PrisonDto(it) }

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
      prisonRepository.findById(prisonId).orElseThrow { PrisonNotFoundException(prisonId) }
      contactDetailsRepository.save(ContactDetails(prisonId, departmentType, EmailAddress(newEmailAddress)))
      return CREATED
    }
    val oldEmailAddress = contactDetails.emailAddress?.value
    if (oldEmailAddress != newEmailAddress) {
      contactDetails.emailAddress = EmailAddress(newEmailAddress)
    }
    return UPDATED
  }

  @Transactional
  @PreAuthorize(CLIENT_CAN_MAINTAIN_PRISON_DETAILS)
  fun deleteEmailAddress(prisonId: String, departmentType: DepartmentType, throwNotFound: Boolean = false) {
    LOG.debug("Enter deleteEmailAddress $prisonId / ${departmentType.toMessage()}")

    contactDetailsRepository.getByPrisonIdAndType(prisonId, departmentType)?.let { contactDetails ->
      contactDetails.emailAddress?.let {
        contactDetails.emailAddress = null
        deleteIfEmpty(contactDetails)
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

  private fun isContactDetailsEmpty(contactDetails: ContactDetails): Boolean = with(contactDetails) {
    webAddress == null && phoneNumber == null && emailAddress == null
  }

  @Transactional
  @PreAuthorize(CLIENT_CAN_MAINTAIN_PRISON_DETAILS)
  fun createContactDetails(prisonId: String, contactDetailsDto: ContactDetailsDto): ContactDetailsDto {
    LOG.debug("Enter createContactDetails $prisonId / ${contactDetailsDto.type.toMessage()}")

    prisonRepository.findById(prisonId).orElseThrow { PrisonNotFoundException(prisonId) }

    if (contactDetailsRepository.getByPrisonIdAndType(prisonId, contactDetailsDto.type) != null) {
      throw ContactDetailsAlreadyExistException(prisonId, contactDetailsDto.type)
    }

    val persistedEmailAddress = contactDetailsDto.emailAddress?.let {
      EmailAddress(it)
    }
    val persistedWebAddress = contactDetailsDto.webAddress?.let {
      WebAddress(it)
    }
    val persistedPhoneNumber = contactDetailsDto.phoneNumber?.let {
      PhoneNumber(it)
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

    prisonRepository.findById(prisonId).orElseThrow { PrisonNotFoundException(prisonId) }

    val contactDetails =
      contactDetailsRepository.getByPrisonIdAndType(prisonId, updateContactDetailsDto.type) ?: throw ContactDetailsNotFoundException(
        prisonId,
        updateContactDetailsDto.type,
      )

    if (haveContactDetailsChanged(updateContactDetailsDto, contactDetails)) {
      updateContactDetailsDto.emailAddress?.let {
        if (contactDetails.emailAddress?.value != it) contactDetails.emailAddress = EmailAddress(it)
      } ?: run { if (removeIfNull) contactDetails.emailAddress = null }

      updateContactDetailsDto.webAddress?.let {
        if (contactDetails.webAddress?.value != it) contactDetails.webAddress = WebAddress(it)
      } ?: run { if (removeIfNull) contactDetails.webAddress = null }

      updateContactDetailsDto.phoneNumber?.let {
        if (contactDetails.phoneNumber?.value != it) contactDetails.phoneNumber = PhoneNumber(it)
      } ?: run { if (removeIfNull) contactDetails.phoneNumber = null }

      val persistedEntity = contactDetailsRepository.save(contactDetails)

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
  }

  @Transactional(readOnly = true)
  fun getPrisonNames(active: Boolean? = null, prisonId: String? = null): List<PrisonNameDto> = prisonRepository.findByActiveOrderByPrisonName(active = active)
    .filter { prisonId == null || it.prisonId == prisonId }
    .map { PrisonNameDto(it.prisonId, it.name, it.prisonNameInWelsh) }

  private fun haveContactDetailsChanged(updateContactDetailsDto: ContactDetailsDto, contactDetails: ContactDetails): Boolean = updateContactDetailsDto.type != contactDetails.type ||
    updateContactDetailsDto.emailAddress != contactDetails.emailAddress?.value ||
    updateContactDetailsDto.webAddress != contactDetails.webAddress?.value ||
    updateContactDetailsDto.phoneNumber != contactDetails.phoneNumber?.value

  fun findPrisonsByIds(ids: List<String>): List<PrisonDto> = prisonRepository.findAllByPrisonIdIsIn(ids).map { PrisonDto(it) }
}
