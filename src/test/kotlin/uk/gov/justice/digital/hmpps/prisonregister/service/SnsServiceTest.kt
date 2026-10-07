package uk.gov.justice.digital.hmpps.prisonregister.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsTopic
import java.time.Instant
import java.util.stream.Stream

class SnsServiceTest {
  private val hmppsQueueService: HmppsQueueService = mock()
  private val snsClient: SnsAsyncClient = mock()
  private val topic = HmppsTopic("domainevents", "arn:aws:sns:eu-west-2:123456789012:domainevents", snsClient)
  private val objectMapper = ObjectMapper()
  private val snsService = SnsService(hmppsQueueService, objectMapper)

  @ParameterizedTest(name = "{0}")
  @MethodSource("domainEventCases")
  fun `publishes register event with the supplied attributes as JSON`(eventCase: DomainEventCase) {
    whenever(hmppsQueueService.findByTopicId("domainevents")).thenReturn(topic)
    val occurredAt = Instant.parse("2026-10-06T13:30:00Z")
    val source = "TEST_SOURCE"
    val childId = 12345L

    eventCase.publish(snsService, eventCase.parentId, childId, occurredAt, source)

    val requestCaptor = argumentCaptor<PublishRequest>()
    verify(snsClient).publish(requestCaptor.capture())

    val message = objectMapper.readTree(requestCaptor.firstValue.message())
    val additionalInformation = message["additionalInformation"]
    assertThat(message["eventType"].asText()).isEqualTo(eventCase.eventType)
    assertThat(additionalInformation[eventCase.parentIdField].asText()).isEqualTo(eventCase.parentId)
    assertThat(additionalInformation["source"].asText()).isEqualTo(source)
    eventCase.childIdField?.let {
      assertThat(additionalInformation[it].asLong()).isEqualTo(childId)
    }
    assertThat(message["occurredAt"].asText()).isEqualTo("2026-10-06T14:30:00+01:00")
  }

  data class DomainEventCase(
    val name: String,
    val eventType: String,
    val parentIdField: String,
    val parentId: String,
    val childIdField: String? = null,
    val publish: (SnsService, String, Long, Instant, String) -> Unit,
  ) {
    override fun toString() = name
  }

  companion object {
    @JvmStatic
    fun domainEventCases(): Stream<DomainEventCase> {
      val cases = listOf(
        DomainEventCase("court amended", "register.court.amended", "courtId", "COURT1", publish = { service, parent, _, at, source -> service.sendCourtRegisterAmendedEvent(parent, at, source) }),
        DomainEventCase("court inserted", "register.court.inserted", "courtId", "COURT1", publish = { service, parent, _, at, source -> service.sendCourtRegisterInsertedEvent(parent, at, source) }),
        DomainEventCase("court deleted", "register.court.deleted", "courtId", "COURT1", publish = { service, parent, _, at, source -> service.sendCourtRegisterDeletedEvent(parent, at, source) }),
        DomainEventCase("court email inserted", "register.court.email.inserted", "courtId", "COURT1", "emailId", { service, parent, child, at, source -> service.sendCourtRegisterEmailInsertedEvent(parent, child, at, source) }),
        DomainEventCase("court email amended", "register.court.email.amended", "courtId", "COURT1", "emailId", { service, parent, child, at, source -> service.sendCourtRegisterEmailAmendedEvent(parent, child, at, source) }),
        DomainEventCase("court email deleted", "register.court.email.deleted", "courtId", "COURT1", "emailId", { service, parent, child, at, source -> service.sendCourtRegisterEmailDeletedEvent(parent, child, at, source) }),
        DomainEventCase("court address inserted", "register.court.address.inserted", "courtId", "COURT1", "addressId", { service, parent, child, at, source -> service.sendCourtRegisterAddressInsertedEvent(parent, child, at, source) }),
        DomainEventCase("court address amended", "register.court.address.amended", "courtId", "COURT1", "addressId", { service, parent, child, at, source -> service.sendCourtRegisterAddressAmendedEvent(parent, child, at, source) }),
        DomainEventCase("court address deleted", "register.court.address.deleted", "courtId", "COURT1", "addressId", { service, parent, child, at, source -> service.sendCourtRegisterAddressDeletedEvent(parent, child, at, source) }),
        DomainEventCase("court phone inserted", "register.court.phone.inserted", "courtId", "COURT1", "phoneId", { service, parent, child, at, source -> service.sendCourtRegisterPhoneInsertedEvent(parent, child, at, source) }),
        DomainEventCase("court phone amended", "register.court.phone.amended", "courtId", "COURT1", "phoneId", { service, parent, child, at, source -> service.sendCourtRegisterPhoneAmendedEvent(parent, child, at, source) }),
        DomainEventCase("court phone deleted", "register.court.phone.deleted", "courtId", "COURT1", "phoneId", { service, parent, child, at, source -> service.sendCourtRegisterPhoneDeletedEvent(parent, child, at, source) }),

        DomainEventCase("agency inserted", "register.agency.inserted", "agencyId", "AGENCY1", publish = { service, parent, _, at, source -> service.sendAgencyRegisterInsertedEvent(parent, at, source) }),
        DomainEventCase("agency amended", "register.agency.amended", "agencyId", "AGENCY1", publish = { service, parent, _, at, source -> service.sendAgencyRegisterAmendedEvent(parent, at, source) }),
        DomainEventCase("agency deleted", "register.agency.deleted", "agencyId", "AGENCY1", publish = { service, parent, _, at, source -> service.sendAgencyRegisterDeletedEvent(parent, at, source) }),
        DomainEventCase("agency email inserted", "register.agency.email.inserted", "agencyId", "AGENCY1", "emailId", { service, parent, child, at, source -> service.sendAgencyRegisterEmailInsertedEvent(parent, child, at, source) }),
        DomainEventCase("agency email amended", "register.agency.email.amended", "agencyId", "AGENCY1", "emailId", { service, parent, child, at, source -> service.sendAgencyRegisterEmailAmendedEvent(parent, child, at, source) }),
        DomainEventCase("agency email deleted", "register.agency.email.deleted", "agencyId", "AGENCY1", "emailId", { service, parent, child, at, source -> service.sendAgencyRegisterEmailDeletedEvent(parent, child, at, source) }),
        DomainEventCase("agency address inserted", "register.agency.address.inserted", "agencyId", "AGENCY1", "addressId", { service, parent, child, at, source -> service.sendAgencyRegisterAddressInsertedEvent(parent, child, at, source) }),
        DomainEventCase("agency address amended", "register.agency.address.amended", "agencyId", "AGENCY1", "addressId", { service, parent, child, at, source -> service.sendAgencyRegisterAddressAmendedEvent(parent, child, at, source) }),
        DomainEventCase("agency address deleted", "register.agency.address.deleted", "agencyId", "AGENCY1", "addressId", { service, parent, child, at, source -> service.sendAgencyRegisterAddressDeletedEvent(parent, child, at, source) }),
        DomainEventCase("agency phone inserted", "register.agency.phone.inserted", "agencyId", "AGENCY1", "phoneId", { service, parent, child, at, source -> service.sendAgencyRegisterPhoneInsertedEvent(parent, child, at, source) }),
        DomainEventCase("agency phone amended", "register.agency.phone.amended", "agencyId", "AGENCY1", "phoneId", { service, parent, child, at, source -> service.sendAgencyRegisterPhoneAmendedEvent(parent, child, at, source) }),
        DomainEventCase("agency phone deleted", "register.agency.phone.deleted", "agencyId", "AGENCY1", "phoneId", { service, parent, child, at, source -> service.sendAgencyRegisterPhoneDeletedEvent(parent, child, at, source) }),

        DomainEventCase("hospital inserted", "register.hospital.inserted", "hospitalId", "HOSPITAL1", publish = { service, parent, _, at, source -> service.sendHospitalRegisterInsertedEvent(parent, at, source) }),
        DomainEventCase("hospital amended", "register.hospital.amended", "hospitalId", "HOSPITAL1", publish = { service, parent, _, at, source -> service.sendHospitalRegisterAmendedEvent(parent, at, source) }),
        DomainEventCase("hospital deleted", "register.hospital.deleted", "hospitalId", "HOSPITAL1", publish = { service, parent, _, at, source -> service.sendHospitalRegisterDeletedEvent(parent, at, source) }),
        DomainEventCase("hospital address inserted", "register.hospital.address.inserted", "hospitalId", "HOSPITAL1", "addressId", { service, parent, child, at, source -> service.sendHospitalRegisterAddressInsertedEvent(parent, child, at, source) }),
        DomainEventCase("hospital address amended", "register.hospital.address.amended", "hospitalId", "HOSPITAL1", "addressId", { service, parent, child, at, source -> service.sendHospitalRegisterAddressAmendedEvent(parent, child, at, source) }),
        DomainEventCase("hospital address deleted", "register.hospital.address.deleted", "hospitalId", "HOSPITAL1", "addressId", { service, parent, child, at, source -> service.sendHospitalRegisterAddressDeletedEvent(parent, child, at, source) }),
        DomainEventCase("hospital phone inserted", "register.hospital.phone.inserted", "hospitalId", "HOSPITAL1", "phoneId", { service, parent, child, at, source -> service.sendHospitalRegisterPhoneInsertedEvent(parent, child, at, source) }),
        DomainEventCase("hospital phone amended", "register.hospital.phone.amended", "hospitalId", "HOSPITAL1", "phoneId", { service, parent, child, at, source -> service.sendHospitalRegisterPhoneAmendedEvent(parent, child, at, source) }),
        DomainEventCase("hospital phone deleted", "register.hospital.phone.deleted", "hospitalId", "HOSPITAL1", "phoneId", { service, parent, child, at, source -> service.sendHospitalRegisterPhoneDeletedEvent(parent, child, at, source) }),

        DomainEventCase("police custody suite inserted", "register.policecustodysuite.inserted", "policeCustodySuiteId", "PCS1", publish = { service, parent, _, at, source -> service.sendPoliceCustodySuiteRegisterInsertedEvent(parent, at, source) }),
        DomainEventCase("police custody suite amended", "register.policecustodysuite.amended", "policeCustodySuiteId", "PCS1", publish = { service, parent, _, at, source -> service.sendPoliceCustodySuiteRegisterAmendedEvent(parent, at, source) }),
        DomainEventCase("police custody suite deleted", "register.policecustodysuite.deleted", "policeCustodySuiteId", "PCS1", publish = { service, parent, _, at, source -> service.sendPoliceCustodySuiteRegisterDeletedEvent(parent, at, source) }),
        DomainEventCase("police custody suite email inserted", "register.policecustodysuite.email.inserted", "policeCustodySuiteId", "PCS1", "emailId", { service, parent, child, at, source -> service.sendPoliceCustodySuiteRegisterEmailInsertedEvent(parent, child, at, source) }),
        DomainEventCase("police custody suite email amended", "register.policecustodysuite.email.amended", "policeCustodySuiteId", "PCS1", "emailId", { service, parent, child, at, source -> service.sendPoliceCustodySuiteRegisterEmailAmendedEvent(parent, child, at, source) }),
        DomainEventCase("police custody suite email deleted", "register.policecustodysuite.email.deleted", "policeCustodySuiteId", "PCS1", "emailId", { service, parent, child, at, source -> service.sendPoliceCustodySuiteRegisterEmailDeletedEvent(parent, child, at, source) }),
        DomainEventCase("police custody suite address inserted", "register.policecustodysuite.address.inserted", "policeCustodySuiteId", "PCS1", "addressId", { service, parent, child, at, source -> service.sendPoliceCustodySuiteRegisterAddressInsertedEvent(parent, child, at, source) }),
        DomainEventCase("police custody suite address amended", "register.policecustodysuite.address.amended", "policeCustodySuiteId", "PCS1", "addressId", { service, parent, child, at, source -> service.sendPoliceCustodySuiteRegisterAddressAmendedEvent(parent, child, at, source) }),
        DomainEventCase("police custody suite address deleted", "register.policecustodysuite.address.deleted", "policeCustodySuiteId", "PCS1", "addressId", { service, parent, child, at, source -> service.sendPoliceCustodySuiteRegisterAddressDeletedEvent(parent, child, at, source) }),
        DomainEventCase("police custody suite phone inserted", "register.policecustodysuite.phone.inserted", "policeCustodySuiteId", "PCS1", "phoneId", { service, parent, child, at, source -> service.sendPoliceCustodySuiteRegisterPhoneInsertedEvent(parent, child, at, source) }),
        DomainEventCase("police custody suite phone amended", "register.policecustodysuite.phone.amended", "policeCustodySuiteId", "PCS1", "phoneId", { service, parent, child, at, source -> service.sendPoliceCustodySuiteRegisterPhoneAmendedEvent(parent, child, at, source) }),
        DomainEventCase("police custody suite phone deleted", "register.policecustodysuite.phone.deleted", "policeCustodySuiteId", "PCS1", "phoneId", { service, parent, child, at, source -> service.sendPoliceCustodySuiteRegisterPhoneDeletedEvent(parent, child, at, source) }),

        DomainEventCase("probation office inserted", "register.probationoffice.inserted", "probationOfficeId", "PROB1", publish = { service, parent, _, at, source -> service.sendProbationOfficeRegisterInsertedEvent(parent, at, source) }),
        DomainEventCase("probation office amended", "register.probationoffice.amended", "probationOfficeId", "PROB1", publish = { service, parent, _, at, source -> service.sendProbationOfficeRegisterAmendedEvent(parent, at, source) }),
        DomainEventCase("probation office deleted", "register.probationoffice.deleted", "probationOfficeId", "PROB1", publish = { service, parent, _, at, source -> service.sendProbationOfficeRegisterDeletedEvent(parent, at, source) }),
        DomainEventCase("probation office email inserted", "register.probationoffice.email.inserted", "probationOfficeId", "PROB1", "emailId", { service, parent, child, at, source -> service.sendProbationOfficeRegisterEmailInsertedEvent(parent, child, at, source) }),
        DomainEventCase("probation office email amended", "register.probationoffice.email.amended", "probationOfficeId", "PROB1", "emailId", { service, parent, child, at, source -> service.sendProbationOfficeRegisterEmailAmendedEvent(parent, child, at, source) }),
        DomainEventCase("probation office email deleted", "register.probationoffice.email.deleted", "probationOfficeId", "PROB1", "emailId", { service, parent, child, at, source -> service.sendProbationOfficeRegisterEmailDeletedEvent(parent, child, at, source) }),
        DomainEventCase("probation office address inserted", "register.probationoffice.address.inserted", "probationOfficeId", "PROB1", "addressId", { service, parent, child, at, source -> service.sendProbationOfficeRegisterAddressInsertedEvent(parent, child, at, source) }),
        DomainEventCase("probation office address amended", "register.probationoffice.address.amended", "probationOfficeId", "PROB1", "addressId", { service, parent, child, at, source -> service.sendProbationOfficeRegisterAddressAmendedEvent(parent, child, at, source) }),
        DomainEventCase("probation office address deleted", "register.probationoffice.address.deleted", "probationOfficeId", "PROB1", "addressId", { service, parent, child, at, source -> service.sendProbationOfficeRegisterAddressDeletedEvent(parent, child, at, source) }),
        DomainEventCase("probation office phone inserted", "register.probationoffice.phone.inserted", "probationOfficeId", "PROB1", "phoneId", { service, parent, child, at, source -> service.sendProbationOfficeRegisterPhoneInsertedEvent(parent, child, at, source) }),
        DomainEventCase("probation office phone amended", "register.probationoffice.phone.amended", "probationOfficeId", "PROB1", "phoneId", { service, parent, child, at, source -> service.sendProbationOfficeRegisterPhoneAmendedEvent(parent, child, at, source) }),
        DomainEventCase("probation office phone deleted", "register.probationoffice.phone.deleted", "probationOfficeId", "PROB1", "phoneId", { service, parent, child, at, source -> service.sendProbationOfficeRegisterPhoneDeletedEvent(parent, child, at, source) }),

        DomainEventCase("approved premises inserted", "register.approvedpremises.inserted", "approvedPremisesId", "APP1", publish = { service, parent, _, at, source -> service.sendApprovedPremisesRegisterInsertedEvent(parent, at, source) }),
        DomainEventCase("approved premises amended", "register.approvedpremises.amended", "approvedPremisesId", "APP1", publish = { service, parent, _, at, source -> service.sendApprovedPremisesRegisterAmendedEvent(parent, at, source) }),
        DomainEventCase("approved premises deleted", "register.approvedpremises.deleted", "approvedPremisesId", "APP1", publish = { service, parent, _, at, source -> service.sendApprovedPremisesRegisterDeletedEvent(parent, at, source) }),
        DomainEventCase("approved premises email inserted", "register.approvedpremises.email.inserted", "approvedPremisesId", "APP1", "emailId", { service, parent, child, at, source -> service.sendApprovedPremisesRegisterEmailInsertedEvent(parent, child, at, source) }),
        DomainEventCase("approved premises email amended", "register.approvedpremises.email.amended", "approvedPremisesId", "APP1", "emailId", { service, parent, child, at, source -> service.sendApprovedPremisesRegisterEmailAmendedEvent(parent, child, at, source) }),
        DomainEventCase("approved premises email deleted", "register.approvedpremises.email.deleted", "approvedPremisesId", "APP1", "emailId", { service, parent, child, at, source -> service.sendApprovedPremisesRegisterEmailDeletedEvent(parent, child, at, source) }),
        DomainEventCase("approved premises address inserted", "register.approvedpremises.address.inserted", "approvedPremisesId", "APP1", "addressId", { service, parent, child, at, source -> service.sendApprovedPremisesRegisterAddressInsertedEvent(parent, child, at, source) }),
        DomainEventCase("approved premises address amended", "register.approvedpremises.address.amended", "approvedPremisesId", "APP1", "addressId", { service, parent, child, at, source -> service.sendApprovedPremisesRegisterAddressAmendedEvent(parent, child, at, source) }),
        DomainEventCase("approved premises address deleted", "register.approvedpremises.address.deleted", "approvedPremisesId", "APP1", "addressId", { service, parent, child, at, source -> service.sendApprovedPremisesRegisterAddressDeletedEvent(parent, child, at, source) }),
        DomainEventCase("approved premises phone inserted", "register.approvedpremises.phone.inserted", "approvedPremisesId", "APP1", "phoneId", { service, parent, child, at, source -> service.sendApprovedPremisesRegisterPhoneInsertedEvent(parent, child, at, source) }),
        DomainEventCase("approved premises phone amended", "register.approvedpremises.phone.amended", "approvedPremisesId", "APP1", "phoneId", { service, parent, child, at, source -> service.sendApprovedPremisesRegisterPhoneAmendedEvent(parent, child, at, source) }),
        DomainEventCase("approved premises phone deleted", "register.approvedpremises.phone.deleted", "approvedPremisesId", "APP1", "phoneId", { service, parent, child, at, source -> service.sendApprovedPremisesRegisterPhoneDeletedEvent(parent, child, at, source) }),
      )
      return Stream.of(*cases.toTypedArray())
    }
  }
}
