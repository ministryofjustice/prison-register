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

  fun sendCourtRegisterAmendedEvent(courtId: String, occurredAt: Instant) {
    publishToDomainEventsTopic(
      HMPPSCourtDomainEvent(
        "register.court.amended",
        CourtAdditionalInformation(courtId),
        occurredAt,
        "A court has been updated",
      ),
    )
  }

  fun sendCourtRegisterInsertedEvent(courtId: String, occurredAt: Instant) {
    publishToDomainEventsTopic(
      HMPPSCourtDomainEvent(
        "register.court.inserted",
        CourtAdditionalInformation(courtId),
        occurredAt,
        "A court has been inserted",
      ),
    )
  }

  fun sendCourtRegisterDeletedEvent(courtId: String, occurredAt: Instant) {
    publishToDomainEventsTopic(
      HMPPSCourtDomainEvent(
        "register.court.deleted",
        CourtAdditionalInformation(courtId),
        occurredAt,
        "A court has been deleted",
      ),
    )
  }

  fun sendAgencyRegisterInsertedEvent(agencyId: String, occurredAt: Instant) {
    publishToDomainEventsTopic(
      HMPPSAgencyDomainEvent(
        "register.agency.inserted",
        AgencyAdditionalInformation(agencyId),
        occurredAt,
        "An agency has been inserted",
      ),
    )
  }

  fun sendAgencyRegisterAmendedEvent(agencyId: String, occurredAt: Instant) {
    publishToDomainEventsTopic(
      HMPPSAgencyDomainEvent(
        "register.agency.amended",
        AgencyAdditionalInformation(agencyId),
        occurredAt,
        "An agency has been updated",
      ),
    )
  }

  fun sendAgencyRegisterDeletedEvent(agencyId: String, occurredAt: Instant) {
    publishToDomainEventsTopic(
      HMPPSAgencyDomainEvent(
        "register.agency.deleted",
        AgencyAdditionalInformation(agencyId),
        occurredAt,
        "An agency has been deleted",
      ),
    )
  }

  fun sendHospitalRegisterInsertedEvent(hospitalId: String, occurredAt: Instant) {
    publishToDomainEventsTopic(
      HMPPSHospitalDomainEvent(
        "register.hospital.inserted",
        HospitalAdditionalInformation(hospitalId),
        occurredAt,
        "A hospital has been inserted",
      ),
    )
  }

  fun sendHospitalRegisterAmendedEvent(hospitalId: String, occurredAt: Instant) {
    publishToDomainEventsTopic(
      HMPPSHospitalDomainEvent(
        "register.hospital.amended",
        HospitalAdditionalInformation(hospitalId),
        occurredAt,
        "A hospital has been updated",
      ),
    )
  }

  fun sendHospitalRegisterDeletedEvent(hospitalId: String, occurredAt: Instant) {
    publishToDomainEventsTopic(
      HMPPSHospitalDomainEvent(
        "register.hospital.deleted",
        HospitalAdditionalInformation(hospitalId),
        occurredAt,
        "A hospital has been deleted",
      ),
    )
  }

  fun sendPoliceCustodySuiteRegisterInsertedEvent(policeCustodySuiteId: String, occurredAt: Instant) {
    publishToDomainEventsTopic(
      HMPPSPoliceCustodySuiteDomainEvent(
        "register.policecustodysuite.inserted",
        PoliceCustodySuiteAdditionalInformation(policeCustodySuiteId),
        occurredAt,
        "A police custody suite has been inserted",
      ),
    )
  }

  fun sendPoliceCustodySuiteRegisterAmendedEvent(policeCustodySuiteId: String, occurredAt: Instant) {
    publishToDomainEventsTopic(
      HMPPSPoliceCustodySuiteDomainEvent(
        "register.policecustodysuite.amended",
        PoliceCustodySuiteAdditionalInformation(policeCustodySuiteId),
        occurredAt,
        "A police custody suite has been updated",
      ),
    )
  }

  fun sendPoliceCustodySuiteRegisterDeletedEvent(policeCustodySuiteId: String, occurredAt: Instant) {
    publishToDomainEventsTopic(
      HMPPSPoliceCustodySuiteDomainEvent(
        "register.policecustodysuite.deleted",
        PoliceCustodySuiteAdditionalInformation(policeCustodySuiteId),
        occurredAt,
        "A police custody suite has been deleted",
      ),
    )
  }

  fun sendProbationOfficeRegisterInsertedEvent(probationOfficeId: String, occurredAt: Instant) {
    publishToDomainEventsTopic(
      HMPPSProbationOfficeDomainEvent(
        "register.probationoffice.inserted",
        ProbationOfficeAdditionalInformation(probationOfficeId),
        occurredAt,
        "A probation office has been inserted",
      ),
    )
  }

  fun sendProbationOfficeRegisterAmendedEvent(probationOfficeId: String, occurredAt: Instant) {
    publishToDomainEventsTopic(
      HMPPSProbationOfficeDomainEvent(
        "register.probationoffice.amended",
        ProbationOfficeAdditionalInformation(probationOfficeId),
        occurredAt,
        "A probation office has been updated",
      ),
    )
  }

  fun sendProbationOfficeRegisterDeletedEvent(probationOfficeId: String, occurredAt: Instant) {
    publishToDomainEventsTopic(
      HMPPSProbationOfficeDomainEvent(
        "register.probationoffice.deleted",
        ProbationOfficeAdditionalInformation(probationOfficeId),
        occurredAt,
        "A probation office has been deleted",
      ),
    )
  }

  fun sendApprovedPremiseRegisterInsertedEvent(approvedPremiseId: String, occurredAt: Instant) {
    publishToDomainEventsTopic(
      HMPPSApprovedPremiseDomainEvent(
        "register.approvedpremise.inserted",
        ApprovedPremiseAdditionalInformation(approvedPremiseId),
        occurredAt,
        "An approved premise has been inserted",
      ),
    )
  }

  fun sendApprovedPremiseRegisterAmendedEvent(approvedPremiseId: String, occurredAt: Instant) {
    publishToDomainEventsTopic(
      HMPPSApprovedPremiseDomainEvent(
        "register.approvedpremise.amended",
        ApprovedPremiseAdditionalInformation(approvedPremiseId),
        occurredAt,
        "An approved premise has been updated",
      ),
    )
  }

  fun sendApprovedPremiseRegisterDeletedEvent(approvedPremiseId: String, occurredAt: Instant) {
    publishToDomainEventsTopic(
      HMPPSApprovedPremiseDomainEvent(
        "register.approvedpremise.deleted",
        ApprovedPremiseAdditionalInformation(approvedPremiseId),
        occurredAt,
        "An approved premise has been deleted",
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

  private fun publishToDomainEventsTopic(payload: HMPPSApprovedPremiseDomainEvent) {
    log.debug("Event {} for id {}", payload.eventType, payload.additionalInformation.approvedPremiseId)
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

data class CourtAdditionalInformation(
  val courtId: String,
)

data class AgencyAdditionalInformation(
  val agencyId: String,
)

data class HospitalAdditionalInformation(
  val hospitalId: String,
)

data class PoliceCustodySuiteAdditionalInformation(
  val policeCustodySuiteId: String,
)

data class ProbationOfficeAdditionalInformation(
  val probationOfficeId: String,
)

data class ApprovedPremiseAdditionalInformation(
  val approvedPremiseId: String,
)

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

data class HMPPSApprovedPremiseDomainEvent(
  val eventType: String,
  val additionalInformation: ApprovedPremiseAdditionalInformation,
  val version: Int,
  val occurredAt: String,
  val description: String,
) {
  constructor(
    eventType: String,
    additionalInformation: ApprovedPremiseAdditionalInformation,
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
