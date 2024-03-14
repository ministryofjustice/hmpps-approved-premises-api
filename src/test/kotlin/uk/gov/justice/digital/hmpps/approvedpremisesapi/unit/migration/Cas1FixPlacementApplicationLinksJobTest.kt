package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.migration

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementDateEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequirementsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision.ACCEPTED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.Cas1FixPlacementApplicationLinksJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import java.time.LocalDate
import java.time.OffsetDateTime
import javax.persistence.EntityManager
import kotlin.math.log

class Cas1FixPlacementApplicationLinksJobTest {

  private val placementApplicationRepository = mockk<PlacementApplicationRepository>()
  private val applicationRepository = mockk<ApplicationRepository>()
  private val placementRequestRepository = mockk<PlacementRequestRepository>()
  private val entityManager = mockk<EntityManager>()
  private val transactionTemplate = mockk<TransactionTemplate>()

  private val service = Cas1FixPlacementApplicationLinksJob(
    placementApplicationRepository,
    applicationRepository,
    placementRequestRepository,
    entityManager,
    transactionTemplate,
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

  private val applicationWithNoArrivalDate = ApprovedPremisesApplicationEntityFactory()
    .withCreatedByUser(createdByUser)
    .withArrivalDate(null)
    .produce()


  private val applicationWithArrivalDate = ApprovedPremisesApplicationEntityFactory()
    .withCreatedByUser(createdByUser)
    .withArrivalDate(OffsetDateTime.now())
    .produce()

  private val assessment = ApprovedPremisesAssessmentEntityFactory()
    .withApplication(applicationWithNoArrivalDate)
    .produce()

  val placementRequirements = PlacementRequirementsEntityFactory()
    .withApplication(applicationWithNoArrivalDate)
    .withAssessment(assessment)
    .produce()

  val logger = mockk<Logger>()

  @BeforeEach
  fun setupLogger() {
    service.log = logger
    every { logger.debug(any()) } returns Unit
    every { logger.error(any()) } returns Unit
  }

  @Test
  fun `nothing to do`() {
    every { applicationRepository.findByIdOrNull(applicationWithNoArrivalDate.id) } returns applicationWithNoArrivalDate
    every { placementApplicationRepository.findByApplication(applicationWithNoArrivalDate) } returns emptyList()

    service.updateApplication(applicationWithNoArrivalDate.id)

    verify { placementRequestRepository wasNot Called }
  }

  @Test
  fun `match placement app with single date to single placement request`() {

    every { applicationRepository.findByIdOrNull(applicationWithNoArrivalDate.id) } returns applicationWithNoArrivalDate

    val placementAppWithSingleDate1 = placementApp()
    addDates(placementAppWithSingleDate1, LocalDate.of(2020,1,2),10)

    val placementRequest1 = placementRequestForDates(LocalDate.of(2020,1,2),10)
    applicationWithNoArrivalDate.placementRequests.add(placementRequest1)

    every { placementApplicationRepository.findByApplication(applicationWithNoArrivalDate) } returns listOf(placementAppWithSingleDate1)
    every { placementRequestRepository.save(any()) } answers { it.invocation.args[0] as PlacementRequestEntity }

    service.updateApplication(applicationWithNoArrivalDate.id)

    verify {
      placementRequestRepository.save(
        match {
          it.id == placementRequest1.id &&
          it.placementApplication == placementAppWithSingleDate1
        }
      )
    }

    assertNoErrorsLogged()
  }

  @Test
  fun `match multiple placement apps with single date to placement requests`() {
    every { applicationRepository.findByIdOrNull(applicationWithNoArrivalDate.id) } returns applicationWithNoArrivalDate

    val placementAppWithSingleDate1 = placementApp()
    addDates(placementAppWithSingleDate1, LocalDate.of(2020,1,2), 10)

    val placementAppWithSingleDate2 = placementApp()
    addDates(placementAppWithSingleDate2, LocalDate.of(2020,2,3), 20)

    val placementRequest1 = placementRequestForDates(LocalDate.of(2020,1,2),10)
    applicationWithNoArrivalDate.placementRequests.add(placementRequest1)

    val placementRequest2 = placementRequestForDates(LocalDate.of(2020,2,3),20)
    applicationWithNoArrivalDate.placementRequests.add(placementRequest2)

    every { placementApplicationRepository.findByApplication(applicationWithNoArrivalDate) } returns listOf(placementAppWithSingleDate1,placementAppWithSingleDate2)
    every { placementRequestRepository.save(any()) } answers { it.invocation.args[0] as PlacementRequestEntity }

    service.updateApplication(applicationWithNoArrivalDate.id)

    verify {
      placementRequestRepository.save(
        match {
          it.id == placementRequest1.id &&
            it.placementApplication == placementAppWithSingleDate1
        }
      )
    }

    verify {
      placementRequestRepository.save(
        match {
          it.id == placementRequest2.id &&
            it.placementApplication == placementAppWithSingleDate2
        }
      )
    }

    assertNoErrorsLogged()
  }

  @Test
  fun `match single placement apps with multiple dates to placement requests`() {
    every { applicationRepository.findByIdOrNull(applicationWithNoArrivalDate.id) } returns applicationWithNoArrivalDate

    val placementAppWithMultipleDate1 = placementApp()
    addDates(placementAppWithMultipleDate1, LocalDate.of(2020,1,2), 10)
    addDates(placementAppWithMultipleDate1, LocalDate.of(2020,2,3), 20)

    val placementRequest1 = placementRequestForDates(LocalDate.of(2020,1,2),10)
    applicationWithNoArrivalDate.placementRequests.add(placementRequest1)

    val placementRequest2 = placementRequestForDates(LocalDate.of(2020,2,3),20)
    applicationWithNoArrivalDate.placementRequests.add(placementRequest2)

    every { placementApplicationRepository.findByApplication(applicationWithNoArrivalDate) } returns listOf(placementAppWithMultipleDate1)
    every { placementRequestRepository.save(any()) } answers { it.invocation.args[0] as PlacementRequestEntity }

    service.updateApplication(applicationWithNoArrivalDate.id)

    verify {
      placementRequestRepository.save(
        match {
          it.id == placementRequest1.id &&
            it.placementApplication == placementAppWithMultipleDate1
        }
      )
    }

    verify {
      placementRequestRepository.save(
        match {
          it.id == placementRequest2.id &&
            it.placementApplication == placementAppWithMultipleDate1
        }
      )
    }

    assertNoErrorsLogged()
  }

  @Test
  fun `match placement app with single date to single placement request, ignore placement apps that aren't accepted`() {
    every { applicationRepository.findByIdOrNull(applicationWithNoArrivalDate.id) } returns applicationWithNoArrivalDate

    val placementAppWithSingleDate1 = placementApp()
    addDates(placementAppWithSingleDate1, LocalDate.of(2020,1,2),10)

    val placementAppWithoutDecisionIsIgnored = placementApp(decision = null)
    addDates(placementAppWithoutDecisionIsIgnored, LocalDate.of(2020,1,2),10)

    val placementRequest1 = placementRequestForDates(LocalDate.of(2020,1,2),10)
    applicationWithNoArrivalDate.placementRequests.add(placementRequest1)

    every { placementApplicationRepository.findByApplication(applicationWithNoArrivalDate) } returns listOf(placementAppWithSingleDate1,placementAppWithoutDecisionIsIgnored)
    every { placementRequestRepository.save(any()) } answers { it.invocation.args[0] as PlacementRequestEntity }

    service.updateApplication(applicationWithNoArrivalDate.id)

    verify {
      placementRequestRepository.save(
        match {
          it.id == placementRequest1.id &&
            it.placementApplication == placementAppWithSingleDate1
        }
      )
    }

    assertNoErrorsLogged()
  }

  @Test
  fun `error if placement application has a decision date, because any application with this populated should already be correctly linked to placement requests`() {
    every { applicationRepository.findByIdOrNull(applicationWithNoArrivalDate.id) } returns applicationWithNoArrivalDate

    val placementAppWithDecisionDate = placementApp(decisionDate = OffsetDateTime.now())
    addDates(placementAppWithDecisionDate, LocalDate.of(2020,1,2),10)

    val placementRequest1 = placementRequestForDates(LocalDate.of(2020,1,2),10)
    applicationWithNoArrivalDate.placementRequests.add(placementRequest1)

    every { placementApplicationRepository.findByApplication(applicationWithNoArrivalDate) } returns listOf(placementAppWithDecisionDate)

    service.updateApplication(applicationWithNoArrivalDate.id)
    verify { logger.error("Should not of received placement application with a decision date as this indicates it was created after we started placement applications to placement requests. Placement applications are [${placementAppWithDecisionDate.id} for date 2020-01-02 and duration 10]") }
  }

  @Test
  fun `error if no corresponding placement request found`() {
    every { applicationRepository.findByIdOrNull(applicationWithNoArrivalDate.id) } returns applicationWithNoArrivalDate

    val placementApp = placementApp()
    addDates(placementApp, LocalDate.of(2020,1,2),10)

    every { placementApplicationRepository.findByApplication(applicationWithNoArrivalDate) } returns listOf(placementApp)

    service.updateApplication(applicationWithNoArrivalDate.id)

    verify { logger.error("No placement request found for placement app ${placementApp.id} for date 2020-01-02 and duration 10") }
  }

  @Test
  fun `error if multiple potential placement requests found`() {
    every { applicationRepository.findByIdOrNull(applicationWithNoArrivalDate.id) } returns applicationWithNoArrivalDate

    val placementApp = placementApp()
    addDates(placementApp, LocalDate.of(2020,1,2), 10)

    val placementRequest1 = placementRequestForDates(LocalDate.of(2020,1,2),10)
    applicationWithNoArrivalDate.placementRequests.add(placementRequest1)

    val placementRequest2 = placementRequestForDates(LocalDate.of(2020,1,2),10)
    applicationWithNoArrivalDate.placementRequests.add(placementRequest2)

    every { placementApplicationRepository.findByApplication(applicationWithNoArrivalDate) } returns listOf(placementApp)

    service.updateApplication(applicationWithNoArrivalDate.id)

    verify { logger.error("More than one potential placement request found for placement app ${placementApp.id} for date 2020-01-02 and duration 10") }
  }

  @Test
  fun `error if application has no initial date and unmatched placement requests exist`() {
    every { applicationRepository.findByIdOrNull(applicationWithNoArrivalDate.id) } returns applicationWithNoArrivalDate

    val placementApp = placementApp()
    addDates(placementApp, LocalDate.of(2020,1,2), 10)

    val placementRequest1 = placementRequestForDates(LocalDate.of(2020,1,2),10)
    applicationWithNoArrivalDate.placementRequests.add(placementRequest1)

    val placementRequest2 = placementRequestForDates(LocalDate.of(2020,2,3),20)
    applicationWithNoArrivalDate.placementRequests.add(placementRequest2)

    every { placementApplicationRepository.findByApplication(applicationWithNoArrivalDate) } returns listOf(placementApp)

    service.updateApplication(applicationWithNoArrivalDate.id)

    verify { logger.error("Application ${applicationWithNoArrivalDate.id} does not have an arrival date, yet there are unmatched placement requests") }
  }

  @Test
  fun `error if application has an initial date and no unmatched placement requests exist after mapping`() {
    every { applicationRepository.findByIdOrNull(applicationWithArrivalDate.id) } returns applicationWithArrivalDate

    val placementApp = placementApp(application = applicationWithArrivalDate)
    addDates(placementApp, LocalDate.of(2020,1,2), 10, )

    val placementRequest1 = placementRequestForDates(LocalDate.of(2020,1,2),10, application = applicationWithArrivalDate)
    applicationWithArrivalDate.placementRequests.add(placementRequest1)

    every { placementApplicationRepository.findByApplication(applicationWithArrivalDate) } returns listOf(placementApp)

    service.updateApplication(applicationWithArrivalDate.id)

    verify { logger.error("Application ${applicationWithArrivalDate.id} has an arrival date set but after matching to placement apps, no placement requests remain") }
  }

  // TODO: no placement requests remain after mapping for app with initial date
  // TODO: add one integration test

  private fun assertNoErrorsLogged() {
    verify(exactly = 0) { logger.error(any()) }
  }

  private fun placementApp(
    application: ApprovedPremisesApplicationEntity = applicationWithNoArrivalDate,
    decision: PlacementApplicationDecision? = ACCEPTED,
    decisionDate: OffsetDateTime? = null
  ) =
    PlacementApplicationEntityFactory()
      .withDefaults()
      .withApplication(application)
      .withDecision(decision)
      .withDecisionMadeAt(decisionDate)
      .produce()

  private fun placementRequestForDates(
    start: LocalDate,
    duration: Int,
    application: ApprovedPremisesApplicationEntity = applicationWithNoArrivalDate,
  ) = PlacementRequestEntityFactory()
    .withPlacementRequirements(placementRequirements)
    .withApplication(application)
    .withAssessment(assessment)
    .withPlacementApplication(null)
    .withExpectedArrival(start)
    .withDuration(duration)
    .produce()

  private fun addDates(placementApp: PlacementApplicationEntity, start: LocalDate, duration: Int) {
    placementApp.placementDates.add(
      PlacementDateEntityFactory()
        .withPlacementApplication(placementApp)
        .withExpectedArrival(start)
        .withDuration(duration)
        .produce()
    )
  }

}