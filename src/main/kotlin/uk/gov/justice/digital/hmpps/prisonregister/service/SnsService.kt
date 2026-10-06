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
  val source: String,
)

open class CourtAdditionalInformation(
  val courtId: String,
  source: String = "DPS",
) : SourcedAdditionalInformation(source)

class CourtEmailAdditionalInformation(
  courtId: String,
  val emailId: Long,
  source: String = "DPS",
) : CourtAdditionalInformation(courtId = courtId, source = source)

class AgencyAdditionalInformation(
  val agencyId: String,
  source: String = "DPS",
) : SourcedAdditionalInformation(source)

class HospitalAdditionalInformation(
  val hospitalId: String,
  source: String = "DPS",
) : SourcedAdditionalInformation(source)

class PoliceCustodySuiteAdditionalInformation(
  val policeCustodySuiteId: String,
  source: String = "DPS",
) : SourcedAdditionalInformation(source)

class ProbationOfficeAdditionalInformation(
  val probationOfficeId: String,
  source: String = "DPS",
) : SourcedAdditionalInformation(source)

class ApprovedPremisesAdditionalInformation(
  val approvedPremisesId: String,
  source: String = "DPS",
) : SourcedAdditionalInformation(source)

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
