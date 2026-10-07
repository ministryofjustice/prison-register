package uk.gov.justice.digital.hmpps.prisonregister.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Service
class SnsService(hmppsQueueService: HmppsQueueService, private val objectMapper: ObjectMapper) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  private val domaineventsTopic by lazy {
    hmppsQueueService.findByTopicId("domainevents")
      ?: throw RuntimeException("Topic with name domainevents doesn't exist")
  }
  private val domaineventsTopicClient by lazy { domaineventsTopic.snsClient }

  fun sendPrisonRegisterInsertedEvent(prisonId: String, occurredAt: Instant) {
    publishToDomainEventsTopic(
      HMPPSDomainEvent(
        "register.prison.inserted",
        AdditionalInformation(prisonId),
        occurredAt,
        "A prison has been inserted",
      ),
    )
  }

  fun sendPrisonRegisterAmendedEvent(prisonId: String, occurredAt: Instant) {
    publishToDomainEventsTopic(
      HMPPSDomainEvent(
        "register.prison.amended",
        AdditionalInformation(prisonId),
        occurredAt,
        "A prison has been updated",
      ),
    )
  }

  fun sendCourtRegisterAmendedEvent(courtId: String, occurredAt: Instant, source: String = "DPS") {
    publishToDomainEventsTopic(
      HMPPSCourtDomainEvent(
        "register.court.amended",
        CourtAdditionalInformation(courtId = courtId, source = source),
        occurredAt,
        "A court has been updated",
      ),
    )
  }

  fun sendCourtRegisterEmailInsertedEvent(courtId: String, emailId: Long, occurredAt: Instant, source: String = "DPS") {
    publishToDomainEventsTopic(
      HMPPSCourtDomainEvent(
        "register.court.email.inserted",
        CourtEmailAdditionalInformation(courtId = courtId, emailId = emailId, source = source),
        occurredAt,
        "A court email has been inserted",
      ),
    )
  }

  fun sendCourtRegisterEmailAmendedEvent(courtId: String, emailId: Long, occurredAt: Instant, source: String = "DPS") {
    publishCourtContactEvent("email", "amended", courtId, emailId, occurredAt, source)
  }

  fun sendCourtRegisterEmailDeletedEvent(courtId: String, emailId: Long, occurredAt: Instant, source: String = "DPS") {
    publishCourtContactEvent("email", "deleted", courtId, emailId, occurredAt, source)
  }

  fun sendCourtRegisterAddressInsertedEvent(courtId: String, addressId: Long, occurredAt: Instant, source: String = "DPS") {
    publishCourtContactEvent("address", "inserted", courtId, addressId, occurredAt, source)
  }

  fun sendCourtRegisterAddressAmendedEvent(courtId: String, addressId: Long, occurredAt: Instant, source: String = "DPS") {
    publishCourtContactEvent("address", "amended", courtId, addressId, occurredAt, source)
  }

  fun sendCourtRegisterAddressDeletedEvent(courtId: String, addressId: Long, occurredAt: Instant, source: String = "DPS") {
    publishCourtContactEvent("address", "deleted", courtId, addressId, occurredAt, source)
  }

  fun sendCourtRegisterPhoneInsertedEvent(courtId: String, phoneId: Long, occurredAt: Instant, source: String = "DPS") {
    publishCourtContactEvent("phone", "inserted", courtId, phoneId, occurredAt, source)
  }

  fun sendCourtRegisterPhoneAmendedEvent(courtId: String, phoneId: Long, occurredAt: Instant, source: String = "DPS") {
    publishCourtContactEvent("phone", "amended", courtId, phoneId, occurredAt, source)
  }

  fun sendCourtRegisterPhoneDeletedEvent(courtId: String, phoneId: Long, occurredAt: Instant, source: String = "DPS") {
    publishCourtContactEvent("phone", "deleted", courtId, phoneId, occurredAt, source)
  }

  private fun publishCourtContactEvent(type: String, action: String, courtId: String, childId: Long, occurredAt: Instant, source: String) {
    val additionalInformation = when (type) {
      "email" -> CourtEmailAdditionalInformation(courtId, childId, source)
      "address" -> CourtAddressAdditionalInformation(courtId, childId, source)
      "phone" -> CourtPhoneAdditionalInformation(courtId, childId, source)
      else -> throw IllegalArgumentException("Unknown court contact detail type: $type")
    }
    val description = when (type) {
      "email" -> "A court email has been $action"
      "address" -> "A court address has been $action"
      "phone" -> "A court phone number has been $action"
      else -> throw IllegalArgumentException("Unknown court contact detail type: $type")
    }
    publishToDomainEventsTopic(
      HMPPSCourtDomainEvent("register.court.$type.$action", additionalInformation, occurredAt, description),
    )
  }

  fun sendCourtRegisterInsertedEvent(courtId: String, occurredAt: Instant, source: String = "DPS") {
    publishToDomainEventsTopic(
      HMPPSCourtDomainEvent(
        "register.court.inserted",
        CourtAdditionalInformation(courtId = courtId, source = source),
        occurredAt,
        "A court has been inserted",
      ),
    )
  }

  fun sendCourtRegisterDeletedEvent(courtId: String, occurredAt: Instant, source: String = "DPS") {
    publishToDomainEventsTopic(
      HMPPSCourtDomainEvent(
        "register.court.deleted",
        CourtAdditionalInformation(courtId = courtId, source = source),
        occurredAt,
        "A court has been deleted",
      ),
    )
  }

  fun sendAgencyRegisterInsertedEvent(agencyId: String, occurredAt: Instant, source: String = "DPS") {
    publishToDomainEventsTopic(
      HMPPSAgencyDomainEvent(
        "register.agency.inserted",
        AgencyAdditionalInformation(agencyId = agencyId, source = source),
        occurredAt,
        "An agency has been inserted",
      ),
    )
  }

  fun sendAgencyRegisterAmendedEvent(agencyId: String, occurredAt: Instant, source: String = "DPS") {
    publishToDomainEventsTopic(
      HMPPSAgencyDomainEvent(
        "register.agency.amended",
        AgencyAdditionalInformation(agencyId = agencyId, source = source),
        occurredAt,
        "An agency has been updated",
      ),
    )
  }

  fun sendAgencyRegisterDeletedEvent(agencyId: String, occurredAt: Instant, source: String = "DPS") {
    publishToDomainEventsTopic(
      HMPPSAgencyDomainEvent(
        "register.agency.deleted",
        AgencyAdditionalInformation(agencyId = agencyId, source = source),
        occurredAt,
        "An agency has been deleted",
      ),
    )
  }

  fun sendAgencyRegisterEmailInsertedEvent(agencyId: String, emailId: Long, occurredAt: Instant, source: String = "DPS") {
    publishAgencyContactEvent("email", "inserted", agencyId, emailId, occurredAt, source)
  }

  fun sendAgencyRegisterEmailAmendedEvent(agencyId: String, emailId: Long, occurredAt: Instant, source: String = "DPS") {
    publishAgencyContactEvent("email", "amended", agencyId, emailId, occurredAt, source)
  }

  fun sendAgencyRegisterEmailDeletedEvent(agencyId: String, emailId: Long, occurredAt: Instant, source: String = "DPS") {
    publishAgencyContactEvent("email", "deleted", agencyId, emailId, occurredAt, source)
  }

  fun sendAgencyRegisterAddressInsertedEvent(agencyId: String, addressId: Long, occurredAt: Instant, source: String = "DPS") {
    publishAgencyContactEvent("address", "inserted", agencyId, addressId, occurredAt, source)
  }

  fun sendAgencyRegisterAddressAmendedEvent(agencyId: String, addressId: Long, occurredAt: Instant, source: String = "DPS") {
    publishAgencyContactEvent("address", "amended", agencyId, addressId, occurredAt, source)
  }

  fun sendAgencyRegisterAddressDeletedEvent(agencyId: String, addressId: Long, occurredAt: Instant, source: String = "DPS") {
    publishAgencyContactEvent("address", "deleted", agencyId, addressId, occurredAt, source)
  }

  fun sendAgencyRegisterPhoneInsertedEvent(agencyId: String, phoneId: Long, occurredAt: Instant, source: String = "DPS") {
    publishAgencyContactEvent("phone", "inserted", agencyId, phoneId, occurredAt, source)
  }

  fun sendAgencyRegisterPhoneAmendedEvent(agencyId: String, phoneId: Long, occurredAt: Instant, source: String = "DPS") {
    publishAgencyContactEvent("phone", "amended", agencyId, phoneId, occurredAt, source)
  }

  fun sendAgencyRegisterPhoneDeletedEvent(agencyId: String, phoneId: Long, occurredAt: Instant, source: String = "DPS") {
    publishAgencyContactEvent("phone", "deleted", agencyId, phoneId, occurredAt, source)
  }

  private fun publishAgencyContactEvent(type: String, action: String, agencyId: String, childId: Long, occurredAt: Instant, source: String) {
    val additionalInformation = when (type) {
      "email" -> AgencyEmailAdditionalInformation(agencyId, childId, source)
      "address" -> AgencyAddressAdditionalInformation(agencyId, childId, source)
      "phone" -> AgencyPhoneAdditionalInformation(agencyId, childId, source)
      else -> throw IllegalArgumentException("Unknown agency contact detail type: $type")
    }
    val description = when (type) {
      "email" -> "An agency email has been $action"
      "address" -> "An agency address has been $action"
      "phone" -> "An agency phone number has been $action"
      else -> throw IllegalArgumentException("Unknown agency contact detail type: $type")
    }
    publishToDomainEventsTopic(
      HMPPSAgencyDomainEvent("register.agency.$type.$action", additionalInformation, occurredAt, description),
    )
  }

  fun sendHospitalRegisterInsertedEvent(hospitalId: String, occurredAt: Instant, source: String = "DPS") {
    publishToDomainEventsTopic(
      HMPPSHospitalDomainEvent(
        "register.hospital.inserted",
        HospitalAdditionalInformation(hospitalId = hospitalId, source = source),
        occurredAt,
        "A hospital has been inserted",
      ),
    )
  }

  fun sendHospitalRegisterAmendedEvent(hospitalId: String, occurredAt: Instant, source: String = "DPS") {
    publishToDomainEventsTopic(
      HMPPSHospitalDomainEvent(
        "register.hospital.amended",
        HospitalAdditionalInformation(hospitalId = hospitalId, source = source),
        occurredAt,
        "A hospital has been updated",
      ),
    )
  }

  fun sendHospitalRegisterDeletedEvent(hospitalId: String, occurredAt: Instant, source: String = "DPS") {
    publishToDomainEventsTopic(
      HMPPSHospitalDomainEvent(
        "register.hospital.deleted",
        HospitalAdditionalInformation(hospitalId = hospitalId, source = source),
        occurredAt,
        "A hospital has been deleted",
      ),
    )
  }

  fun sendHospitalRegisterAddressInsertedEvent(hospitalId: String, addressId: Long, occurredAt: Instant, source: String = "DPS") {
    publishHospitalContactEvent("address", "inserted", hospitalId, addressId, occurredAt, source)
  }

  fun sendHospitalRegisterAddressAmendedEvent(hospitalId: String, addressId: Long, occurredAt: Instant, source: String = "DPS") {
    publishHospitalContactEvent("address", "amended", hospitalId, addressId, occurredAt, source)
  }

  fun sendHospitalRegisterAddressDeletedEvent(hospitalId: String, addressId: Long, occurredAt: Instant, source: String = "DPS") {
    publishHospitalContactEvent("address", "deleted", hospitalId, addressId, occurredAt, source)
  }

  fun sendHospitalRegisterPhoneInsertedEvent(hospitalId: String, phoneId: Long, occurredAt: Instant, source: String = "DPS") {
    publishHospitalContactEvent("phone", "inserted", hospitalId, phoneId, occurredAt, source)
  }

  fun sendHospitalRegisterPhoneAmendedEvent(hospitalId: String, phoneId: Long, occurredAt: Instant, source: String = "DPS") {
    publishHospitalContactEvent("phone", "amended", hospitalId, phoneId, occurredAt, source)
  }

  fun sendHospitalRegisterPhoneDeletedEvent(hospitalId: String, phoneId: Long, occurredAt: Instant, source: String = "DPS") {
    publishHospitalContactEvent("phone", "deleted", hospitalId, phoneId, occurredAt, source)
  }

  private fun publishHospitalContactEvent(type: String, action: String, hospitalId: String, childId: Long, occurredAt: Instant, source: String) {
    val additionalInformation = when (type) {
      "address" -> HospitalAddressAdditionalInformation(hospitalId, childId, source)
      "phone" -> HospitalPhoneAdditionalInformation(hospitalId, childId, source)
      else -> throw IllegalArgumentException("Unknown hospital contact detail type: $type")
    }
    val description = when (type) {
      "address" -> "A hospital address has been $action"
      "phone" -> "A hospital phone number has been $action"
      else -> throw IllegalArgumentException("Unknown hospital contact detail type: $type")
    }
    publishToDomainEventsTopic(
      HMPPSHospitalDomainEvent("register.hospital.$type.$action", additionalInformation, occurredAt, description),
    )
  }

  fun sendPoliceCustodySuiteRegisterInsertedEvent(policeCustodySuiteId: String, occurredAt: Instant, source: String = "DPS") {
    publishToDomainEventsTopic(
      HMPPSPoliceCustodySuiteDomainEvent(
        "register.policecustodysuite.inserted",
        PoliceCustodySuiteAdditionalInformation(policeCustodySuiteId = policeCustodySuiteId, source = source),
        occurredAt,
        "A police custody suite has been inserted",
      ),
    )
  }

  fun sendPoliceCustodySuiteRegisterAmendedEvent(policeCustodySuiteId: String, occurredAt: Instant, source: String = "DPS") {
    publishToDomainEventsTopic(
      HMPPSPoliceCustodySuiteDomainEvent(
        "register.policecustodysuite.amended",
        PoliceCustodySuiteAdditionalInformation(policeCustodySuiteId = policeCustodySuiteId, source = source),
        occurredAt,
        "A police custody suite has been updated",
      ),
    )
  }

  fun sendPoliceCustodySuiteRegisterDeletedEvent(policeCustodySuiteId: String, occurredAt: Instant, source: String = "DPS") {
    publishToDomainEventsTopic(
      HMPPSPoliceCustodySuiteDomainEvent(
        "register.policecustodysuite.deleted",
        PoliceCustodySuiteAdditionalInformation(policeCustodySuiteId = policeCustodySuiteId, source = source),
        occurredAt,
        "A police custody suite has been deleted",
      ),
    )
  }

  fun sendPoliceCustodySuiteRegisterEmailInsertedEvent(policeCustodySuiteId: String, emailId: Long, occurredAt: Instant, source: String = "DPS") {
    publishPoliceCustodySuiteContactEvent("email", "inserted", policeCustodySuiteId, emailId, occurredAt, source)
  }

  fun sendPoliceCustodySuiteRegisterEmailAmendedEvent(policeCustodySuiteId: String, emailId: Long, occurredAt: Instant, source: String = "DPS") {
    publishPoliceCustodySuiteContactEvent("email", "amended", policeCustodySuiteId, emailId, occurredAt, source)
  }

  fun sendPoliceCustodySuiteRegisterEmailDeletedEvent(policeCustodySuiteId: String, emailId: Long, occurredAt: Instant, source: String = "DPS") {
    publishPoliceCustodySuiteContactEvent("email", "deleted", policeCustodySuiteId, emailId, occurredAt, source)
  }

  fun sendPoliceCustodySuiteRegisterAddressInsertedEvent(policeCustodySuiteId: String, addressId: Long, occurredAt: Instant, source: String = "DPS") {
    publishPoliceCustodySuiteContactEvent("address", "inserted", policeCustodySuiteId, addressId, occurredAt, source)
  }

  fun sendPoliceCustodySuiteRegisterAddressAmendedEvent(policeCustodySuiteId: String, addressId: Long, occurredAt: Instant, source: String = "DPS") {
    publishPoliceCustodySuiteContactEvent("address", "amended", policeCustodySuiteId, addressId, occurredAt, source)
  }

  fun sendPoliceCustodySuiteRegisterAddressDeletedEvent(policeCustodySuiteId: String, addressId: Long, occurredAt: Instant, source: String = "DPS") {
    publishPoliceCustodySuiteContactEvent("address", "deleted", policeCustodySuiteId, addressId, occurredAt, source)
  }

  fun sendPoliceCustodySuiteRegisterPhoneInsertedEvent(policeCustodySuiteId: String, phoneId: Long, occurredAt: Instant, source: String = "DPS") {
    publishPoliceCustodySuiteContactEvent("phone", "inserted", policeCustodySuiteId, phoneId, occurredAt, source)
  }

  fun sendPoliceCustodySuiteRegisterPhoneAmendedEvent(policeCustodySuiteId: String, phoneId: Long, occurredAt: Instant, source: String = "DPS") {
    publishPoliceCustodySuiteContactEvent("phone", "amended", policeCustodySuiteId, phoneId, occurredAt, source)
  }

  fun sendPoliceCustodySuiteRegisterPhoneDeletedEvent(policeCustodySuiteId: String, phoneId: Long, occurredAt: Instant, source: String = "DPS") {
    publishPoliceCustodySuiteContactEvent("phone", "deleted", policeCustodySuiteId, phoneId, occurredAt, source)
  }

  private fun publishPoliceCustodySuiteContactEvent(type: String, action: String, policeCustodySuiteId: String, childId: Long, occurredAt: Instant, source: String) {
    val additionalInformation = when (type) {
      "email" -> PoliceCustodySuiteEmailAdditionalInformation(policeCustodySuiteId, childId, source)
      "address" -> PoliceCustodySuiteAddressAdditionalInformation(policeCustodySuiteId, childId, source)
      "phone" -> PoliceCustodySuitePhoneAdditionalInformation(policeCustodySuiteId, childId, source)
      else -> throw IllegalArgumentException("Unknown police custody suite contact detail type: $type")
    }
    val description = when (type) {
      "email" -> "A police custody suite email has been $action"
      "address" -> "A police custody suite address has been $action"
      "phone" -> "A police custody suite phone number has been $action"
      else -> throw IllegalArgumentException("Unknown police custody suite contact detail type: $type")
    }
    publishToDomainEventsTopic(
      HMPPSPoliceCustodySuiteDomainEvent("register.policecustodysuite.$type.$action", additionalInformation, occurredAt, description),
    )
  }

  fun sendProbationOfficeRegisterInsertedEvent(probationOfficeId: String, occurredAt: Instant, source: String = "DPS") {
    publishToDomainEventsTopic(
      HMPPSProbationOfficeDomainEvent(
        "register.probationoffice.inserted",
        ProbationOfficeAdditionalInformation(probationOfficeId = probationOfficeId, source = source),
        occurredAt,
        "A probation office has been inserted",
      ),
    )
  }

  fun sendProbationOfficeRegisterAmendedEvent(probationOfficeId: String, occurredAt: Instant, source: String = "DPS") {
    publishToDomainEventsTopic(
      HMPPSProbationOfficeDomainEvent(
        "register.probationoffice.amended",
        ProbationOfficeAdditionalInformation(probationOfficeId = probationOfficeId, source = source),
        occurredAt,
        "A probation office has been updated",
      ),
    )
  }

  fun sendProbationOfficeRegisterDeletedEvent(probationOfficeId: String, occurredAt: Instant, source: String = "DPS") {
    publishToDomainEventsTopic(
      HMPPSProbationOfficeDomainEvent(
        "register.probationoffice.deleted",
        ProbationOfficeAdditionalInformation(probationOfficeId = probationOfficeId, source = source),
        occurredAt,
        "A probation office has been deleted",
      ),
    )
  }

  fun sendProbationOfficeRegisterEmailInsertedEvent(probationOfficeId: String, emailId: Long, occurredAt: Instant, source: String = "DPS") {
    publishProbationOfficeContactEvent("email", "inserted", probationOfficeId, emailId, occurredAt, source)
  }

  fun sendProbationOfficeRegisterEmailAmendedEvent(probationOfficeId: String, emailId: Long, occurredAt: Instant, source: String = "DPS") {
    publishProbationOfficeContactEvent("email", "amended", probationOfficeId, emailId, occurredAt, source)
  }

  fun sendProbationOfficeRegisterEmailDeletedEvent(probationOfficeId: String, emailId: Long, occurredAt: Instant, source: String = "DPS") {
    publishProbationOfficeContactEvent("email", "deleted", probationOfficeId, emailId, occurredAt, source)
  }

  fun sendProbationOfficeRegisterAddressInsertedEvent(probationOfficeId: String, addressId: Long, occurredAt: Instant, source: String = "DPS") {
    publishProbationOfficeContactEvent("address", "inserted", probationOfficeId, addressId, occurredAt, source)
  }

  fun sendProbationOfficeRegisterAddressAmendedEvent(probationOfficeId: String, addressId: Long, occurredAt: Instant, source: String = "DPS") {
    publishProbationOfficeContactEvent("address", "amended", probationOfficeId, addressId, occurredAt, source)
  }

  fun sendProbationOfficeRegisterAddressDeletedEvent(probationOfficeId: String, addressId: Long, occurredAt: Instant, source: String = "DPS") {
    publishProbationOfficeContactEvent("address", "deleted", probationOfficeId, addressId, occurredAt, source)
  }

  fun sendProbationOfficeRegisterPhoneInsertedEvent(probationOfficeId: String, phoneId: Long, occurredAt: Instant, source: String = "DPS") {
    publishProbationOfficeContactEvent("phone", "inserted", probationOfficeId, phoneId, occurredAt, source)
  }

  fun sendProbationOfficeRegisterPhoneAmendedEvent(probationOfficeId: String, phoneId: Long, occurredAt: Instant, source: String = "DPS") {
    publishProbationOfficeContactEvent("phone", "amended", probationOfficeId, phoneId, occurredAt, source)
  }

  fun sendProbationOfficeRegisterPhoneDeletedEvent(probationOfficeId: String, phoneId: Long, occurredAt: Instant, source: String = "DPS") {
    publishProbationOfficeContactEvent("phone", "deleted", probationOfficeId, phoneId, occurredAt, source)
  }

  private fun publishProbationOfficeContactEvent(type: String, action: String, probationOfficeId: String, childId: Long, occurredAt: Instant, source: String) {
    val additionalInformation = when (type) {
      "email" -> ProbationOfficeEmailAdditionalInformation(probationOfficeId, childId, source)
      "address" -> ProbationOfficeAddressAdditionalInformation(probationOfficeId, childId, source)
      "phone" -> ProbationOfficePhoneAdditionalInformation(probationOfficeId, childId, source)
      else -> throw IllegalArgumentException("Unknown probation office contact detail type: $type")
    }
    val description = when (type) {
      "email" -> "A probation office email has been $action"
      "address" -> "A probation office address has been $action"
      "phone" -> "A probation office phone number has been $action"
      else -> throw IllegalArgumentException("Unknown probation office contact detail type: $type")
    }
    publishToDomainEventsTopic(
      HMPPSProbationOfficeDomainEvent("register.probationoffice.$type.$action", additionalInformation, occurredAt, description),
    )
  }

  fun sendApprovedPremisesRegisterInsertedEvent(approvedPremisesId: String, occurredAt: Instant, source: String = "DPS") {
    publishToDomainEventsTopic(
      HMPPSApprovedPremisesDomainEvent(
        "register.approvedpremises.inserted",
        ApprovedPremisesAdditionalInformation(approvedPremisesId = approvedPremisesId, source = source),
        occurredAt,
        "An approved premises has been inserted",
      ),
    )
  }

  fun sendApprovedPremisesRegisterAmendedEvent(approvedPremisesId: String, occurredAt: Instant, source: String = "DPS") {
    publishToDomainEventsTopic(
      HMPPSApprovedPremisesDomainEvent(
        "register.approvedpremises.amended",
        ApprovedPremisesAdditionalInformation(approvedPremisesId = approvedPremisesId, source = source),
        occurredAt,
        "An approved premises has been updated",
      ),
    )
  }

  fun sendApprovedPremisesRegisterDeletedEvent(approvedPremisesId: String, occurredAt: Instant, source: String = "DPS") {
    publishToDomainEventsTopic(
      HMPPSApprovedPremisesDomainEvent(
        "register.approvedpremises.deleted",
        ApprovedPremisesAdditionalInformation(approvedPremisesId = approvedPremisesId, source = source),
        occurredAt,
        "An approved premises has been deleted",
      ),
    )
  }

  fun sendApprovedPremisesRegisterEmailInsertedEvent(approvedPremisesId: String, emailId: Long, occurredAt: Instant, source: String = "DPS") {
    publishApprovedPremisesContactEvent("email", "inserted", approvedPremisesId, emailId, occurredAt, source)
  }

  fun sendApprovedPremisesRegisterEmailAmendedEvent(approvedPremisesId: String, emailId: Long, occurredAt: Instant, source: String = "DPS") {
    publishApprovedPremisesContactEvent("email", "amended", approvedPremisesId, emailId, occurredAt, source)
  }

  fun sendApprovedPremisesRegisterEmailDeletedEvent(approvedPremisesId: String, emailId: Long, occurredAt: Instant, source: String = "DPS") {
    publishApprovedPremisesContactEvent("email", "deleted", approvedPremisesId, emailId, occurredAt, source)
  }

  fun sendApprovedPremisesRegisterAddressInsertedEvent(approvedPremisesId: String, addressId: Long, occurredAt: Instant, source: String = "DPS") {
    publishApprovedPremisesContactEvent("address", "inserted", approvedPremisesId, addressId, occurredAt, source)
  }

  fun sendApprovedPremisesRegisterAddressAmendedEvent(approvedPremisesId: String, addressId: Long, occurredAt: Instant, source: String = "DPS") {
    publishApprovedPremisesContactEvent("address", "amended", approvedPremisesId, addressId, occurredAt, source)
  }

  fun sendApprovedPremisesRegisterAddressDeletedEvent(approvedPremisesId: String, addressId: Long, occurredAt: Instant, source: String = "DPS") {
    publishApprovedPremisesContactEvent("address", "deleted", approvedPremisesId, addressId, occurredAt, source)
  }

  fun sendApprovedPremisesRegisterPhoneInsertedEvent(approvedPremisesId: String, phoneId: Long, occurredAt: Instant, source: String = "DPS") {
    publishApprovedPremisesContactEvent("phone", "inserted", approvedPremisesId, phoneId, occurredAt, source)
  }

  fun sendApprovedPremisesRegisterPhoneAmendedEvent(approvedPremisesId: String, phoneId: Long, occurredAt: Instant, source: String = "DPS") {
    publishApprovedPremisesContactEvent("phone", "amended", approvedPremisesId, phoneId, occurredAt, source)
  }

  fun sendApprovedPremisesRegisterPhoneDeletedEvent(approvedPremisesId: String, phoneId: Long, occurredAt: Instant, source: String = "DPS") {
    publishApprovedPremisesContactEvent("phone", "deleted", approvedPremisesId, phoneId, occurredAt, source)
  }

  private fun publishApprovedPremisesContactEvent(type: String, action: String, approvedPremisesId: String, childId: Long, occurredAt: Instant, source: String) {
    val additionalInformation = when (type) {
      "email" -> ApprovedPremisesEmailAdditionalInformation(approvedPremisesId, childId, source)
      "address" -> ApprovedPremisesAddressAdditionalInformation(approvedPremisesId, childId, source)
      "phone" -> ApprovedPremisesPhoneAdditionalInformation(approvedPremisesId, childId, source)
      else -> throw IllegalArgumentException("Unknown approved premises contact detail type: $type")
    }
    val description = when (type) {
      "email" -> "An approved premises email has been $action"
      "address" -> "An approved premises address has been $action"
      "phone" -> "An approved premises phone number has been $action"
      else -> throw IllegalArgumentException("Unknown approved premises contact detail type: $type")
    }
    publishToDomainEventsTopic(
      HMPPSApprovedPremisesDomainEvent("register.approvedpremises.$type.$action", additionalInformation, occurredAt, description),
    )
  }

  private fun publishToDomainEventsTopic(payload: HMPPSDomainEvent) {
    log.debug("Event {} for id {}", payload.eventType, payload.additionalInformation.prisonId)
    publish(payload.eventType, payload)
  }

  private fun publishToDomainEventsTopic(payload: HMPPSCourtDomainEvent) {
    log.debug("Event {} for id {}", payload.eventType, payload.additionalInformation.courtId)
    publish(payload.eventType, payload)
  }

  private fun publishToDomainEventsTopic(payload: HMPPSAgencyDomainEvent) {
    log.debug("Event {} for id {}", payload.eventType, payload.additionalInformation.agencyId)
    publish(payload.eventType, payload)
  }

  private fun publishToDomainEventsTopic(payload: HMPPSHospitalDomainEvent) {
    log.debug("Event {} for id {}", payload.eventType, payload.additionalInformation.hospitalId)
    publish(payload.eventType, payload)
  }

  private fun publishToDomainEventsTopic(payload: HMPPSPoliceCustodySuiteDomainEvent) {
    log.debug("Event {} for id {}", payload.eventType, payload.additionalInformation.policeCustodySuiteId)
    publish(payload.eventType, payload)
  }

  private fun publishToDomainEventsTopic(payload: HMPPSProbationOfficeDomainEvent) {
    log.debug("Event {} for id {}", payload.eventType, payload.additionalInformation.probationOfficeId)
    publish(payload.eventType, payload)
  }

  private fun publishToDomainEventsTopic(payload: HMPPSApprovedPremisesDomainEvent) {
    log.debug("Event {} for id {}", payload.eventType, payload.additionalInformation.approvedPremisesId)
    publish(payload.eventType, payload)
  }

  private fun publish(eventType: String, payload: Any) {
    domaineventsTopicClient.publish(
      PublishRequest.builder()
        .topicArn(domaineventsTopic.arn)
        .message(objectMapper.writeValueAsString(payload))
        .messageAttributes(
          mapOf(
            "eventType" to MessageAttributeValue.builder().dataType("String").stringValue(eventType).build(),
          ),
        )
        .build()
        .also { log.info("Published event $eventType to domainevents topic") },
    )
  }
}

data class AdditionalInformation(
  val prisonId: String,
)

open class SourcedAdditionalInformation(
  open val source: String,
)

open class CourtAdditionalInformation(
  open val courtId: String,
  override val source: String = "DPS",
) : SourcedAdditionalInformation(source)

class CourtEmailAdditionalInformation(
  override val courtId: String,
  val emailId: Long,
  override val source: String = "DPS",
) : CourtAdditionalInformation(courtId = courtId, source = source)

class CourtAddressAdditionalInformation(
  override val courtId: String,
  val addressId: Long,
  override val source: String = "DPS",
) : CourtAdditionalInformation(courtId, source)

class CourtPhoneAdditionalInformation(
  override val courtId: String,
  val phoneId: Long,
  override val source: String = "DPS",
) : CourtAdditionalInformation(courtId, source)

open class AgencyAdditionalInformation(
  open val agencyId: String,
  override val source: String = "DPS",
) : SourcedAdditionalInformation(source)

class AgencyEmailAdditionalInformation(
  override val agencyId: String,
  val emailId: Long,
  override val source: String = "DPS",
) : AgencyAdditionalInformation(agencyId, source)

class AgencyAddressAdditionalInformation(
  override val agencyId: String,
  val addressId: Long,
  override val source: String = "DPS",
) : AgencyAdditionalInformation(agencyId, source)

class AgencyPhoneAdditionalInformation(
  override val agencyId: String,
  val phoneId: Long,
  override val source: String = "DPS",
) : AgencyAdditionalInformation(agencyId, source)

open class HospitalAdditionalInformation(
  open val hospitalId: String,
  override val source: String = "DPS",
) : SourcedAdditionalInformation(source)

class HospitalAddressAdditionalInformation(
  override val hospitalId: String,
  val addressId: Long,
  override val source: String = "DPS",
) : HospitalAdditionalInformation(hospitalId, source)

class HospitalPhoneAdditionalInformation(
  override val hospitalId: String,
  val phoneId: Long,
  override val source: String = "DPS",
) : HospitalAdditionalInformation(hospitalId, source)

open class PoliceCustodySuiteAdditionalInformation(
  open val policeCustodySuiteId: String,
  override val source: String = "DPS",
) : SourcedAdditionalInformation(source)

class PoliceCustodySuiteEmailAdditionalInformation(
  override val policeCustodySuiteId: String,
  val emailId: Long,
  override val source: String = "DPS",
) : PoliceCustodySuiteAdditionalInformation(policeCustodySuiteId, source)

class PoliceCustodySuiteAddressAdditionalInformation(
  override val policeCustodySuiteId: String,
  val addressId: Long,
  override val source: String = "DPS",
) : PoliceCustodySuiteAdditionalInformation(policeCustodySuiteId, source)

class PoliceCustodySuitePhoneAdditionalInformation(
  override val policeCustodySuiteId: String,
  val phoneId: Long,
  override val source: String = "DPS",
) : PoliceCustodySuiteAdditionalInformation(policeCustodySuiteId, source)

open class ProbationOfficeAdditionalInformation(
  open val probationOfficeId: String,
  override val source: String = "DPS",
) : SourcedAdditionalInformation(source)

class ProbationOfficeEmailAdditionalInformation(
  override val probationOfficeId: String,
  val emailId: Long,
  override val source: String = "DPS",
) : ProbationOfficeAdditionalInformation(probationOfficeId, source)

class ProbationOfficeAddressAdditionalInformation(
  override val probationOfficeId: String,
  val addressId: Long,
  override val source: String = "DPS",
) : ProbationOfficeAdditionalInformation(probationOfficeId, source)

class ProbationOfficePhoneAdditionalInformation(
  override val probationOfficeId: String,
  val phoneId: Long,
  override val source: String = "DPS",
) : ProbationOfficeAdditionalInformation(probationOfficeId, source)

open class ApprovedPremisesAdditionalInformation(
  open val approvedPremisesId: String,
  override val source: String = "DPS",
) : SourcedAdditionalInformation(source)

class ApprovedPremisesEmailAdditionalInformation(
  override val approvedPremisesId: String,
  val emailId: Long,
  override val source: String = "DPS",
) : ApprovedPremisesAdditionalInformation(approvedPremisesId, source)

class ApprovedPremisesAddressAdditionalInformation(
  override val approvedPremisesId: String,
  val addressId: Long,
  override val source: String = "DPS",
) : ApprovedPremisesAdditionalInformation(approvedPremisesId, source)

class ApprovedPremisesPhoneAdditionalInformation(
  override val approvedPremisesId: String,
  val phoneId: Long,
  override val source: String = "DPS",
) : ApprovedPremisesAdditionalInformation(approvedPremisesId, source)

data class HMPPSDomainEvent(
  val eventType: String,
  val additionalInformation: AdditionalInformation,
  val version: Int,
  val occurredAt: String,
  val description: String,
) {
  constructor(
    eventType: String,
    additionalInformation: AdditionalInformation,
    occurredAt: Instant,
    description: String,
  ) : this(
    eventType,
    additionalInformation,
    1,
    occurredAt.toOffsetDateFormat(),
    description,
  )
}

data class HMPPSCourtDomainEvent(
  val eventType: String,
  val additionalInformation: CourtAdditionalInformation,
  val version: Int,
  val occurredAt: String,
  val description: String,
) {
  constructor(
    eventType: String,
    additionalInformation: CourtAdditionalInformation,
    occurredAt: Instant,
    description: String,
  ) : this(
    eventType,
    additionalInformation,
    1,
    occurredAt.toOffsetDateFormat(),
    description,
  )
}

data class HMPPSAgencyDomainEvent(
  val eventType: String,
  val additionalInformation: AgencyAdditionalInformation,
  val version: Int,
  val occurredAt: String,
  val description: String,
) {
  constructor(
    eventType: String,
    additionalInformation: AgencyAdditionalInformation,
    occurredAt: Instant,
    description: String,
  ) : this(
    eventType,
    additionalInformation,
    1,
    occurredAt.toOffsetDateFormat(),
    description,
  )
}

data class HMPPSHospitalDomainEvent(
  val eventType: String,
  val additionalInformation: HospitalAdditionalInformation,
  val version: Int,
  val occurredAt: String,
  val description: String,
) {
  constructor(
    eventType: String,
    additionalInformation: HospitalAdditionalInformation,
    occurredAt: Instant,
    description: String,
  ) : this(
    eventType,
    additionalInformation,
    1,
    occurredAt.toOffsetDateFormat(),
    description,
  )
}

data class HMPPSPoliceCustodySuiteDomainEvent(
  val eventType: String,
  val additionalInformation: PoliceCustodySuiteAdditionalInformation,
  val version: Int,
  val occurredAt: String,
  val description: String,
) {
  constructor(
    eventType: String,
    additionalInformation: PoliceCustodySuiteAdditionalInformation,
    occurredAt: Instant,
    description: String,
  ) : this(
    eventType,
    additionalInformation,
    1,
    occurredAt.toOffsetDateFormat(),
    description,
  )
}

data class HMPPSProbationOfficeDomainEvent(
  val eventType: String,
  val additionalInformation: ProbationOfficeAdditionalInformation,
  val version: Int,
  val occurredAt: String,
  val description: String,
) {
  constructor(
    eventType: String,
    additionalInformation: ProbationOfficeAdditionalInformation,
    occurredAt: Instant,
    description: String,
  ) : this(
    eventType,
    additionalInformation,
    1,
    occurredAt.toOffsetDateFormat(),
    description,
  )
}

data class HMPPSApprovedPremisesDomainEvent(
  val eventType: String,
  val additionalInformation: ApprovedPremisesAdditionalInformation,
  val version: Int,
  val occurredAt: String,
  val description: String,
) {
  constructor(
    eventType: String,
    additionalInformation: ApprovedPremisesAdditionalInformation,
    occurredAt: Instant,
    description: String,
  ) : this(
    eventType,
    additionalInformation,
    1,
    occurredAt.toOffsetDateFormat(),
    description,
  )
}
fun Instant.toOffsetDateFormat(): String = atZone(ZoneId.of("Europe/London")).toOffsetDateTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
