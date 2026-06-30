package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.AssessmentAppealedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AppealDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AppealEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1AppealDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.SaveCas1DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.isWithinTheLastMinute
import java.time.LocalDate
import java.util.UUID

class Cas1AppealCas1DomainEventServiceTest {

  private val domainEventService = mockk<Cas1DomainEventService>()
  private val apDeliusContextApiClient = mockk<ApDeliusContextApiClient>()
  private val applicationUrlTemplate = mockk<UrlTemplate>()
  private val applicationAppealUrlTemplate = mockk<UrlTemplate>()

  private val service = Cas1AppealDomainEventService(
    domainEventService,
    apDeliusContextApiClient,
    applicationUrlTemplate,
    applicationAppealUrlTemplate,
  )

  private val probationRegion = ProbationRegionEntityFactory()
    .withApArea(
      ApAreaEntityFactory()
        .produce(),
    )
    .produce()

  private val createdByUser = UserEntityFactory()
    .withProbationRegion(probationRegion)
    .produce()

  private val staffUserDetails = StaffDetailFactory.staffDetail(deliusUsername = createdByUser.deliusUsername)

  private val application = ApprovedPremisesApplicationEntityFactory()
    .withCreatedByUser(createdByUser)
    .produce()

  private val assessment = ApprovedPremisesAssessmentEntityFactory()
    .withApplication(application)
    .produce()

  private val appealId = UUID.randomUUID()

  val now = LocalDate.now()

  @BeforeEach
  fun setupMocks() {
    every { domainEventService.saveAssessmentAppealedEvent(any()) } just Runs
    every { apDeliusContextApiClient.getStaffDetail(createdByUser.deliusUsername) } returns ClientResult.Success(
      HttpStatus.OK,
      staffUserDetails,
    )

    every { applicationUrlTemplate.resolve(any(), any()) } returns "http://frontend/applications/${application.id}"
    every { applicationAppealUrlTemplate.resolve(any()) } returns "http://frontend/applications/${application.id}/appeals/$appealId"
  }

  @Test
  fun `appealRecordCreated creates domain event`() {
    service.appealRecordCreated(
      AppealEntityFactory()
        .withApplication(application)
        .withAssessment(assessment)
        .withAppealDate(now)
        .withDecision(AppealDecision.accepted)
        .withCreatedBy(createdByUser)
        .withAppealDetail("Some information about why the appeal is being made")
        .withDecisionDetail("Some information about the decision made")
        .produce(),
    )

    val domainEventSlot = slot<SaveCas1DomainEvent<AssessmentAppealedEnvelope>>()
    verify(exactly = 1) {
      domainEventService.saveAssessmentAppealedEvent(capture(domainEventSlot))
    }

    val domainEvent = domainEventSlot.captured
    assertCommonFields(domainEvent)
    assertThat(domainEvent.data.eventDetails.personReference.noms).isEqualTo(application.nomsNumber)
  }

  @Test
  fun `appealRecordCreated noms is optional`() {
    application.nomsNumber = null

    service.appealRecordCreated(
      AppealEntityFactory()
        .withApplication(application)
        .withAssessment(assessment)
        .withAppealDate(now)
        .withDecision(AppealDecision.accepted)
        .withCreatedBy(createdByUser)
        .withAppealDetail("Some information about why the appeal is being made")
        .withDecisionDetail("Some information about the decision made")
        .produce(),
    )

    val domainEventSlot = slot<SaveCas1DomainEvent<AssessmentAppealedEnvelope>>()
    verify(exactly = 1) {
      domainEventService.saveAssessmentAppealedEvent(capture(domainEventSlot))
    }

    val domainEvent = domainEventSlot.captured
    assertCommonFields(domainEvent)
    assertThat(domainEvent.data.eventDetails.personReference.noms).isEqualTo("Unknown NOMS Number")
  }

  private fun assertCommonFields(domainEvent: SaveCas1DomainEvent<AssessmentAppealedEnvelope>) {
    assertThat(domainEvent.applicationId).isEqualTo(application.id)
    assertThat(domainEvent.assessmentId).isNull()
    assertThat(domainEvent.crn).isEqualTo(application.crn)
    assertThat(domainEvent.occurredAt).isWithinTheLastMinute()

    val eventDetails = domainEvent.data.eventDetails
    assertThat(eventDetails.applicationId).isEqualTo(application.id)
    assertThat(eventDetails.applicationUrl).isEqualTo("http://frontend/applications/${application.id}")
    assertThat(eventDetails.appealUrl).startsWith("http://frontend/applications/${application.id}")
    assertThat(eventDetails.personReference.crn).isEqualTo(application.crn)
    assertThat(eventDetails.deliusEventNumber).isEqualTo(application.eventNumber)
    assertThat(eventDetails.createdAt).isWithinTheLastMinute()
    assertThat(eventDetails.createdBy).isEqualTo(staffUserDetails.toStaffMember())
    assertThat(eventDetails.appealDetail).isEqualTo("Some information about why the appeal is being made")
    assertThat(eventDetails.decision).isEqualTo(uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.AppealDecision.accepted)
    assertThat(eventDetails.decisionDetail).isEqualTo("Some information about the decision made")
  }
}
