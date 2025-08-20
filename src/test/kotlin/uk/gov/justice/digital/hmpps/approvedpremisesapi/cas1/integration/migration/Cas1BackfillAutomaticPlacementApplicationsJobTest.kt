package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.integration.migration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAPlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationPlaceholderEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJobService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementRequestDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawableEntityType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawalContext
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawalTriggeredByUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.roundNanosToMillisToAccountForLossOfPrecisionInPostgres
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.OffsetDateTime.now
import java.util.UUID

class Cas1BackfillAutomaticPlacementApplicationsJobTest : IntegrationTestBase() {
  @Autowired
  lateinit var migrationJobService: MigrationJobService

  @Autowired
  lateinit var placementRequestDomainEventService: Cas1PlacementRequestDomainEventService

  @SuppressWarnings("LongMethod")
  @Test
  fun `all variants`() {
    // this represents the typical migration case
    val application1HasSingleInitialPr = givenACas1Application(
      submittedAt = now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres(),
      arrivalDate = OffsetDateTime.parse("2020-02-04T00:00:00.000Z"),
      duration = 1,
    )

    val application1Placeholder = placementApplicationPlaceholderRepository.save(
      PlacementApplicationPlaceholderEntity(
        id = UUID.randomUUID(),
        application1HasSingleInitialPr,
        submittedAt = now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres(),
        expectedArrivalDate = now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres(),
        archived = false,
      ),
    )

    val (application1AssessmentAllocatedTo, _) = givenAUser()
    val (application1Pr1, _) = givenAPlacementRequest(
      placementRequestCreatedAt = now().minusDays(1).roundNanosToMillisToAccountForLossOfPrecisionInPostgres(),
      application = application1HasSingleInitialPr,
      expectedArrival = LocalDate.parse("2020-02-05"),
      duration = 2,
      assessmentAllocatedTo = application1AssessmentAllocatedTo,
    )

    val application2ArchivedPlaceholderUseRandomId = givenACas1Application(
      submittedAt = now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres(),
      arrivalDate = OffsetDateTime.parse("2020-02-06T00:00:00.000Z"),
      duration = 2,
    )

    val application2Placeholder = placementApplicationPlaceholderRepository.save(
      PlacementApplicationPlaceholderEntity(
        id = UUID.randomUUID(),
        application2ArchivedPlaceholderUseRandomId,
        submittedAt = now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres(),
        expectedArrivalDate = now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres(),
        archived = true,
      ),
    )

    val (application2Pr1, _) = givenAPlacementRequest(
      placementRequestCreatedAt = now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres(),
      application = application2ArchivedPlaceholderUseRandomId,
      expectedArrival = LocalDate.parse("2020-02-07"),
      duration = 3,
    )

    // application with multiple initial prs and placeholder
    // this shouldn't happen but there are around 40 of these for
    // old applications, most likely due to bugs
    val application3HasMultipleInitialPrs = givenACas1Application(
      submittedAt = now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres(),
      arrivalDate = OffsetDateTime.parse("2020-02-04T00:00:00.000Z"),
      duration = 5,
    )

    val application3Placeholder = placementApplicationPlaceholderRepository.save(
      PlacementApplicationPlaceholderEntity(
        id = UUID.randomUUID(),
        application3HasMultipleInitialPrs,
        submittedAt = now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres(),
        expectedArrivalDate = now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres(),
        archived = false,
      ),
    )

    val (application3AssessmentAllocatedTo, _) = givenAUser()
    val (application3Pr1, _) = givenAPlacementRequest(
      placementRequestCreatedAt = OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres(),
      application = application3HasMultipleInitialPrs,
      expectedArrival = LocalDate.parse("2020-02-10"),
      duration = 7,
      assessmentAllocatedTo = application3AssessmentAllocatedTo,
    )
    val (application3Pr2, _) = givenAPlacementRequest(
      placementRequestCreatedAt = now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres(),
      application = application3HasMultipleInitialPrs,
      expectedArrival = LocalDate.parse("2020-02-20"),
      duration = 8,
      assessmentAllocatedTo = application3AssessmentAllocatedTo,
    )

    // application with no date/duration defined, with multiple initial prs
    // these shouldn't exist, but there are 155 of these
    val application3HasMultipleInitialPrsAndNoArrivalDate = givenACas1Application(
      submittedAt = now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres(),
      arrivalDate = null,
      duration = null,
    )

    val application4Placeholder = placementApplicationPlaceholderRepository.save(
      PlacementApplicationPlaceholderEntity(
        id = UUID.randomUUID(),
        application3HasMultipleInitialPrsAndNoArrivalDate,
        submittedAt = now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres(),
        expectedArrivalDate = now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres(),
        archived = false,
      ),
    )

    val (application4AssessmentAllocatedTo, _) = givenAUser()
    val (application4Pr1, _) = givenAPlacementRequest(
      placementRequestCreatedAt = now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres(),
      application = application3HasMultipleInitialPrsAndNoArrivalDate,
      expectedArrival = LocalDate.parse("2020-03-10"),
      duration = 17,
      assessmentAllocatedTo = application4AssessmentAllocatedTo,
    )
    val (application4Pr2, _) = givenAPlacementRequest(
      placementRequestCreatedAt = now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres(),
      application = application3HasMultipleInitialPrsAndNoArrivalDate,
      expectedArrival = LocalDate.parse("2020-03-20"),
      duration = 18,
      assessmentAllocatedTo = application4AssessmentAllocatedTo,
    )

    // withdrawn placement request
    val application5HasSingleWithdrawnInitialPr = givenACas1Application(
      submittedAt = now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres(),
      arrivalDate = OffsetDateTime.parse("2020-05-04T00:01:00.000+01:00"),
      duration = 20,
    )

    val application5Placeholder = placementApplicationPlaceholderRepository.save(
      PlacementApplicationPlaceholderEntity(
        id = UUID.randomUUID(),
        application5HasSingleWithdrawnInitialPr,
        submittedAt = now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres(),
        expectedArrivalDate = now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres(),
        archived = false,
      ),
    )

    val (application5AssessmentAllocatedTo, _) = givenAUser()
    val (application5Pr1, _) = givenAPlacementRequest(
      placementRequestCreatedAt = now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres(),
      application = application5HasSingleWithdrawnInitialPr,
      expectedArrival = LocalDate.parse("2022-07-08"),
      duration = 67,
      assessmentAllocatedTo = application5AssessmentAllocatedTo,
      isWithdrawn = true,
      withdrawalReason = PlacementRequestWithdrawalReason.NO_CAPACITY_DUE_TO_PLACEMENT_PRIORITISATION,
    )

    val (withdrawingUser, _) = givenAUser()
    placementRequestDomainEventService.placementRequestWithdrawn(
      application5Pr1,
      withdrawalContext = WithdrawalContext(
        WithdrawalTriggeredByUser(withdrawingUser),
        WithdrawableEntityType.PlacementRequest,
        application5Pr1.id,
      ),
    )

    // application with one pr linked to a pa (do nothing case)
    val application6AlreadyHasPlacementAppDoNothing = givenACas1Application(
      submittedAt = now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres(),
      arrivalDate = OffsetDateTime.parse("2020-06-04T00:01:00.000+01:00"),
      duration = 99,
    )

    val application6PlacementApp = givenAPlacementApplication()
    val (application6Pr1, _) = givenAPlacementRequest(
      placementRequestCreatedAt = now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres(),
      application = application6AlreadyHasPlacementAppDoNothing,
      expectedArrival = LocalDate.parse("2022-07-08"),
      duration = 67,
      placementApplication = application6PlacementApp,
    )

    migrationJobService.runMigrationJob(MigrationJobType.cas1BackfillAutomaticPlacementApplications)

    val application1Pa1 = placementRequestRepository.findByIdOrNull(application1Pr1.id)!!.placementApplication
    assertThat(application1Pa1).isNotNull()
    assertThat(application1Pa1!!.id).isEqualTo(application1Placeholder.id)
    assertThat(application1Pa1.backfilledAutomatic).isTrue
    assertThat(application1Pa1.expectedArrival).isEqualTo(LocalDate.parse("2020-02-05"))
    assertThat(application1Pa1.requestedDuration).isEqualTo(1)
    assertThat(application1Pa1.authorisedDuration).isEqualTo(2)
    assertThat(application1Pa1.placementType).isEqualTo(PlacementType.AUTOMATIC)
    assertThat(application1Pa1.application.id).isEqualTo(application1HasSingleInitialPr.id)
    assertThat(application1Pa1.createdByUser.id).isEqualTo(application1HasSingleInitialPr.createdByUser.id)
    assertThat(application1Pa1.createdAt).isEqualTo(application1HasSingleInitialPr.createdAt)
    assertThat(application1Pa1.automatic).isTrue
    assertThat(application1Pa1.data).isNull()
    assertThat(application1Pa1.document).isNull()
    assertThat(application1Pa1.submittedAt).isEqualTo(application1HasSingleInitialPr.submittedAt)
    assertThat(application1Pa1.decision).isEqualTo(PlacementApplicationDecision.ACCEPTED)
    assertThat(application1Pa1.decisionMadeAt).isEqualTo(application1Pr1.createdAt)
    assertThat(application1Pa1.withdrawalReason).isNull()
    assertThat(application1Pa1.isWithdrawn).isEqualTo(false)
    assertThat(application1Pa1.submittedAt).isNotNull
    assertThat(application1Pa1.dueAt).isNull()
    assertThat(application1Pa1.allocatedToUser!!.id).isEqualTo(application1AssessmentAllocatedTo.id)
    assertThat(application1Pa1.allocatedAt).isEqualTo(application1Pr1.assessment.allocatedAt)
    assertThat(application1Pa1.reallocatedAt).isNull()

    assertThat(placementApplicationPlaceholderRepository.findByIdOrNull(application1Placeholder.id)!!.archived).isTrue

    val application2Pa1 = placementRequestRepository.findByIdOrNull(application2Pr1.id)!!.placementApplication
    assertThat(application2Pa1).isNotNull()
    assertThat(application2Pa1!!.id).isNotNull().isNotEqualTo(application2Placeholder.id)

    assertThat(placementApplicationPlaceholderRepository.findByIdOrNull(application2Placeholder.id)!!.archived).isTrue

    val application3Pa1 = placementRequestRepository.findByIdOrNull(application3Pr1.id)!!.placementApplication
    assertThat(application3Pa1).isNotNull()
    assertThat(application3Pa1!!.id).isEqualTo(application3Placeholder.id)
    assertThat(application3Pa1.backfilledAutomatic).isTrue
    assertThat(application3Pa1.expectedArrival).isEqualTo(LocalDate.parse("2020-02-10"))
    assertThat(application3Pa1.requestedDuration).isEqualTo(5)
    assertThat(application3Pa1.authorisedDuration).isEqualTo(7)
    assertThat(application3Pa1.placementType).isEqualTo(PlacementType.AUTOMATIC)
    assertThat(application3Pa1.application.id).isEqualTo(application3HasMultipleInitialPrs.id)
    assertThat(application3Pa1.createdByUser.id).isEqualTo(application3HasMultipleInitialPrs.createdByUser.id)
    assertThat(application3Pa1.createdAt).isEqualTo(application3HasMultipleInitialPrs.createdAt)
    assertThat(application3Pa1.automatic).isTrue
    assertThat(application3Pa1.data).isNull()
    assertThat(application3Pa1.document).isNull()
    assertThat(application3Pa1.submittedAt).isEqualTo(application3HasMultipleInitialPrs.submittedAt)
    assertThat(application3Pa1.decision).isEqualTo(PlacementApplicationDecision.ACCEPTED)
    assertThat(application3Pa1.decisionMadeAt).isEqualTo(application3Pr1.createdAt)
    assertThat(application3Pa1.withdrawalReason).isNull()
    assertThat(application3Pa1.isWithdrawn).isEqualTo(false)
    assertThat(application3Pa1.submittedAt).isNotNull
    assertThat(application3Pa1.dueAt).isNull()
    assertThat(application3Pa1.allocatedToUser!!.id).isEqualTo(application3AssessmentAllocatedTo.id)
    assertThat(application3Pa1.allocatedAt).isEqualTo(application3Pr1.assessment.allocatedAt)
    assertThat(application3Pa1.reallocatedAt).isNull()

    val application3Pa2 = placementRequestRepository.findByIdOrNull(application3Pr2.id)!!.placementApplication
    assertThat(application3Pa2).isNotNull()
    assertThat(application3Pa2!!.id).isNotNull.isNotEqualTo(application3Placeholder.id)
    assertThat(application3Pa2.backfilledAutomatic).isTrue
    assertThat(application3Pa2.expectedArrival).isEqualTo(LocalDate.parse("2020-02-20"))
    assertThat(application3Pa2.requestedDuration).isEqualTo(5)
    assertThat(application3Pa2.authorisedDuration).isEqualTo(8)
    assertThat(application3Pa2.placementType).isEqualTo(PlacementType.AUTOMATIC)
    assertThat(application3Pa2.application.id).isEqualTo(application3HasMultipleInitialPrs.id)
    assertThat(application3Pa2.createdByUser.id).isEqualTo(application3HasMultipleInitialPrs.createdByUser.id)
    assertThat(application3Pa2.createdAt).isEqualTo(application3HasMultipleInitialPrs.createdAt)
    assertThat(application3Pa2.automatic).isTrue
    assertThat(application3Pa2.data).isNull()
    assertThat(application3Pa2.document).isNull()
    assertThat(application3Pa2.submittedAt).isEqualTo(application3HasMultipleInitialPrs.submittedAt)
    assertThat(application3Pa2.decision).isEqualTo(PlacementApplicationDecision.ACCEPTED)
    assertThat(application3Pa2.decisionMadeAt).isEqualTo(application3Pr2.createdAt)
    assertThat(application3Pa2.withdrawalReason).isNull()
    assertThat(application3Pa2.isWithdrawn).isEqualTo(false)
    assertThat(application3Pa2.submittedAt).isNotNull
    assertThat(application3Pa2.dueAt).isNull()
    assertThat(application3Pa2.allocatedToUser!!.id).isEqualTo(application3AssessmentAllocatedTo.id)
    assertThat(application3Pa2.allocatedAt).isEqualTo(application3Pr2.assessment.allocatedAt)
    assertThat(application3Pa2.reallocatedAt).isNull()

    assertThat(placementApplicationPlaceholderRepository.findByIdOrNull(application3Placeholder.id)!!.archived).isTrue

    val application4Pa1 = placementRequestRepository.findByIdOrNull(application4Pr1.id)!!.placementApplication
    assertThat(application4Pa1).isNotNull()
    assertThat(application4Pa1!!.id).isEqualTo(application4Placeholder.id)
    assertThat(application4Pa1.backfilledAutomatic).isTrue
    assertThat(application4Pa1.expectedArrival).isEqualTo(LocalDate.parse("2020-03-10"))
    assertThat(application4Pa1.requestedDuration).isEqualTo(17)
    assertThat(application4Pa1.authorisedDuration).isEqualTo(17)
    assertThat(application4Pa1.placementType).isEqualTo(PlacementType.AUTOMATIC)
    assertThat(application4Pa1.application.id).isEqualTo(application3HasMultipleInitialPrsAndNoArrivalDate.id)
    assertThat(application4Pa1.createdByUser.id).isEqualTo(application3HasMultipleInitialPrsAndNoArrivalDate.createdByUser.id)
    assertThat(application4Pa1.createdAt).isEqualTo(application3HasMultipleInitialPrsAndNoArrivalDate.createdAt)
    assertThat(application4Pa1.automatic).isTrue
    assertThat(application4Pa1.data).isNull()
    assertThat(application4Pa1.document).isNull()
    assertThat(application4Pa1.submittedAt).isEqualTo(application3HasMultipleInitialPrsAndNoArrivalDate.submittedAt)
    assertThat(application4Pa1.decision).isEqualTo(PlacementApplicationDecision.ACCEPTED)
    assertThat(application4Pa1.decisionMadeAt).isEqualTo(application4Pr1.createdAt)
    assertThat(application4Pa1.withdrawalReason).isNull()
    assertThat(application4Pa1.isWithdrawn).isEqualTo(false)
    assertThat(application4Pa1.submittedAt).isNotNull
    assertThat(application4Pa1.dueAt).isNull()
    assertThat(application4Pa1.allocatedToUser!!.id).isEqualTo(application4AssessmentAllocatedTo.id)
    assertThat(application4Pa1.allocatedAt).isEqualTo(application4Pr1.assessment.allocatedAt)
    assertThat(application4Pa1.reallocatedAt).isNull()

    val application4Pa2 = placementRequestRepository.findByIdOrNull(application4Pr2.id)!!.placementApplication
    assertThat(application4Pa2).isNotNull()
    assertThat(application4Pa2!!.id).isNotNull.isNotEqualTo(application4Placeholder.id)
    assertThat(application4Pa2.backfilledAutomatic).isTrue
    assertThat(application4Pa2.expectedArrival).isEqualTo(LocalDate.parse("2020-03-20"))
    assertThat(application4Pa2.requestedDuration).isEqualTo(18)
    assertThat(application4Pa2.authorisedDuration).isEqualTo(18)
    assertThat(application4Pa2.placementType).isEqualTo(PlacementType.AUTOMATIC)
    assertThat(application4Pa2.application.id).isEqualTo(application3HasMultipleInitialPrsAndNoArrivalDate.id)
    assertThat(application4Pa2.createdByUser.id).isEqualTo(application3HasMultipleInitialPrsAndNoArrivalDate.createdByUser.id)
    assertThat(application4Pa2.createdAt).isEqualTo(application3HasMultipleInitialPrsAndNoArrivalDate.createdAt)
    assertThat(application4Pa2.automatic).isTrue
    assertThat(application4Pa2.data).isNull()
    assertThat(application4Pa2.document).isNull()
    assertThat(application4Pa2.submittedAt).isEqualTo(application3HasMultipleInitialPrsAndNoArrivalDate.submittedAt)
    assertThat(application4Pa2.decision).isEqualTo(PlacementApplicationDecision.ACCEPTED)
    assertThat(application4Pa2.decisionMadeAt).isEqualTo(application4Pr2.createdAt)
    assertThat(application4Pa2.withdrawalReason).isNull()
    assertThat(application4Pa2.isWithdrawn).isEqualTo(false)
    assertThat(application4Pa2.submittedAt).isNotNull
    assertThat(application4Pa2.dueAt).isNull()
    assertThat(application4Pa2.allocatedToUser!!.id).isEqualTo(application4AssessmentAllocatedTo.id)
    assertThat(application4Pa2.allocatedAt).isEqualTo(application4Pr2.assessment.allocatedAt)
    assertThat(application4Pa2.reallocatedAt).isNull()

    assertThat(placementApplicationPlaceholderRepository.findByIdOrNull(application4Placeholder.id)!!.archived).isTrue

    val application5Pa1 = placementRequestRepository.findByIdOrNull(application5Pr1.id)!!.placementApplication
    assertThat(application5Pa1).isNotNull()
    assertThat(application5Pa1!!.id).isEqualTo(application5Placeholder.id)
    assertThat(application5Pa1.backfilledAutomatic).isTrue
    assertThat(application5Pa1.expectedArrival).isEqualTo(LocalDate.parse("2022-07-08"))
    assertThat(application5Pa1.requestedDuration).isEqualTo(20)
    assertThat(application5Pa1.authorisedDuration).isEqualTo(67)
    assertThat(application5Pa1.placementType).isEqualTo(PlacementType.AUTOMATIC)
    assertThat(application5Pa1.application.id).isEqualTo(application5HasSingleWithdrawnInitialPr.id)
    assertThat(application5Pa1.createdByUser.id).isEqualTo(application5HasSingleWithdrawnInitialPr.createdByUser.id)
    assertThat(application5Pa1.createdAt).isEqualTo(application5HasSingleWithdrawnInitialPr.createdAt)
    assertThat(application5Pa1.automatic).isTrue
    assertThat(application5Pa1.data).isNull()
    assertThat(application5Pa1.document).isNull()
    assertThat(application5Pa1.submittedAt).isEqualTo(application5HasSingleWithdrawnInitialPr.submittedAt)
    assertThat(application5Pa1.decision).isEqualTo(PlacementApplicationDecision.ACCEPTED)
    assertThat(application5Pa1.decisionMadeAt).isEqualTo(application5Pr1.createdAt)
    assertThat(application5Pa1.withdrawalReason).isEqualTo(PlacementApplicationWithdrawalReason.NO_CAPACITY_DUE_TO_PLACEMENT_PRIORITISATION)
    assertThat(application5Pa1.isWithdrawn).isEqualTo(true)
    assertThat(application5Pa1.submittedAt).isNotNull
    assertThat(application5Pa1.dueAt).isNull()
    assertThat(application5Pa1.allocatedToUser!!.id).isEqualTo(application5AssessmentAllocatedTo.id)
    assertThat(application5Pa1.allocatedAt).isEqualTo(application5Pr1.assessment.allocatedAt)
    assertThat(application5Pa1.reallocatedAt).isNull()

    domainEventAsserter.assertDomainEventOfTypeStored(
      application5Pr1.application.id,
      DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_WITHDRAWN,
    )

    assertThat(placementApplicationPlaceholderRepository.findByIdOrNull(application5Placeholder.id)!!.archived).isTrue

    val application6PlacementAppUpdated = placementRequestRepository.findByIdOrNull(application6Pr1.id)!!.placementApplication
    assertThat(application6PlacementAppUpdated).isNotNull()
    assertThat(application6PlacementAppUpdated!!.id).isEqualTo(application6PlacementApp.id)
    assertThat(application6PlacementAppUpdated.backfilledAutomatic).isFalse
  }
}
