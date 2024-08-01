package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.seed

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed.SeedTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1ApplicationUserDetailsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.CsvBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1WithdrawPlacementRequestSeedSeedCsvRow
import java.time.LocalDate
import java.time.OffsetDateTime

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class SeedCas1WithdrawPlacementRequestsTest : SeedTestBase() {

  @Test
  fun `Errors if placement request not linked to placement application`() {
    `Given a User` { applicant, _ ->
      `Given an Offender` { offenderDetails, _ ->
        val (application, _) = createApplicationAndAssessment(applicant, applicant, offenderDetails)

        val placementRequest = createPlacementRequest(application)
        withCsv(
          "valid-csv",
          rowsToCsv(
            listOf(
              Cas1WithdrawPlacementRequestSeedSeedCsvRow(
                placementRequest.id,
                PlacementRequestWithdrawalReason.DUPLICATE_PLACEMENT_REQUEST,
              ),
            ),
          ),
        )

        seedService.seedData(SeedFileType.approvedPremisesWithdrawPlacementRequest, "valid-csv.csv")

        assertThat(logEntries).anyMatch {
          it.level == "error" &&
            it.message == "Error on row 1:" &&
            it.throwable != null &&
            it.throwable.message == "Withdraw placement request seed job should only be used for placement_requests linked to a placement_application"
        }
      }
    }
  }

  @Test
  fun `Withdraws requested placement request, leaving others unmodified`() {
    `Given a User` { applicant, _ ->
      `Given an Offender` { offenderDetails, _ ->
        val (application, _) = createApplicationAndAssessment(applicant, applicant, offenderDetails)

        val placementApplication = createPlacementApplication(application, listOf(LocalDate.now() to 2))
        val placementRequest1 = createPlacementRequest(application) {
          withExpectedArrival(LocalDate.of(2023, 1, 1))
          withPlacementApplication(placementApplication)
        }
        val placementRequest2 = createPlacementRequest(application) {
          withExpectedArrival(LocalDate.of(2023, 1, 10))
          withPlacementApplication(placementApplication)
        }
        val placementRequest3 = createPlacementRequest(application) {
          withExpectedArrival(LocalDate.of(2023, 1, 20))
          withPlacementApplication(placementApplication)
        }

        withCsv(
          "valid-csv",
          rowsToCsv(
            listOf(
              Cas1WithdrawPlacementRequestSeedSeedCsvRow(
                placementRequest1.id,
                PlacementRequestWithdrawalReason.DUPLICATE_PLACEMENT_REQUEST,
              ),
              Cas1WithdrawPlacementRequestSeedSeedCsvRow(
                placementRequest3.id,
                PlacementRequestWithdrawalReason.ERROR_IN_PLACEMENT_REQUEST,
              ),
            ),
          ),
        )

        seedService.seedData(SeedFileType.approvedPremisesWithdrawPlacementRequest, "valid-csv.csv")

        assertPlacementApplicationNotWithdrawn(placementApplication)

        assertPlacementRequestWithdrawn(placementRequest1, PlacementRequestWithdrawalReason.DUPLICATE_PLACEMENT_REQUEST)
        assertPlacementRequestNotWithdrawn(placementRequest2)
        assertPlacementRequestWithdrawn(placementRequest3, PlacementRequestWithdrawalReason.ERROR_IN_PLACEMENT_REQUEST)

        val cruEmail = application.apArea!!.emailAddress!!
        emailAsserter.assertEmailsRequestedCount(2)
        assertMatchRequestWithdrawnEmail(cruEmail, placementRequest1)
        assertMatchRequestWithdrawnEmail(cruEmail, placementRequest3)

        val notes = applicationTimelineNoteRepository.findApplicationTimelineNoteEntitiesByApplicationId(application.id)
        assertThat(notes).hasSize(2)
        assertThat(notes)
          .extracting("body")
          .contains(
            "The Match Request for arrival date Sunday 1 January 2023 has been withdrawn by Application Support " +
              "as the CRU has indicated that it is no longer required. Withdrawal reason is 'Duplicate placement request'",
          )
        assertThat(notes)
          .extracting("body")
          .contains(
            "The Match Request for arrival date Friday 20 January 2023 has been withdrawn by Application Support " +
              "as the CRU has indicated that it is no longer required. Withdrawal reason is 'Error in placement request'",
          )
      }
    }
  }

  private fun assertMatchRequestWithdrawnEmail(emailAddress: String, placementRequest: PlacementRequestEntity) =
    emailAsserter.assertEmailRequested(
      emailAddress,
      notifyConfig.templates.matchRequestWithdrawnV2,
      mapOf("startDate" to placementRequest.expectedArrival.toString()),
    )

  private fun assertPlacementApplicationNotWithdrawn(placementApplication: PlacementApplicationEntity) {
    val updatedPlacementApplication = placementApplicationRepository.findByIdOrNull(placementApplication.id)!!
    Assertions.assertThat(updatedPlacementApplication.isWithdrawn).isFalse()
    Assertions.assertThat(updatedPlacementApplication.withdrawalReason).isNull()
  }

  private fun assertPlacementRequestWithdrawn(placementRequest: PlacementRequestEntity, reason: PlacementRequestWithdrawalReason) {
    val updatedPlacementRequest = placementRequestRepository.findByIdOrNull(placementRequest.id)!!
    Assertions.assertThat(updatedPlacementRequest.isWithdrawn).isEqualTo(true)
    Assertions.assertThat(updatedPlacementRequest.withdrawalReason).isEqualTo(reason)
  }

  private fun assertPlacementRequestNotWithdrawn(placementRequest: PlacementRequestEntity) {
    val updatedPlacementRequest = placementRequestRepository.findByIdOrNull(placementRequest.id)!!
    Assertions.assertThat(updatedPlacementRequest.isWithdrawn).isEqualTo(false)
  }

  @SuppressWarnings("LongParameterList")
  private fun createApplicationAndAssessment(
    applicant: UserEntity,
    assignee: UserEntity,
    offenderDetails: OffenderDetailSummary,
    assessmentSubmitted: Boolean = true,
    assessmentAllocatedTo: UserEntity? = null,
    caseManager: Cas1ApplicationUserDetailsEntity? = null,
  ): Pair<ApprovedPremisesApplicationEntity, AssessmentEntity> {
    val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
    }

    val assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
      withAddedAt(OffsetDateTime.now())
    }

    val apArea = apAreaEntityFactory.produceAndPersist {
      withEmailAddress("apAreaEmail@test.com")
    }

    val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withCrn(offenderDetails.otherIds.crn)
      withCreatedByUser(applicant)
      withApplicationSchema(applicationSchema)
      withSubmittedAt(OffsetDateTime.now())
      withApArea(apArea)
      withReleaseType("licence")
      withCaseManagerUserDetails(caseManager)
      withCaseManagerIsNotApplicant(caseManager != null)
    }

    val assessment = approvedPremisesAssessmentEntityFactory.produceAndPersist {
      withAllocatedToUser(assignee)
      withApplication(application)
      withAssessmentSchema(assessmentSchema)
      withSubmittedAt(if (assessmentSubmitted) OffsetDateTime.now() else null)
      withAllocatedToUser(assessmentAllocatedTo)
    }

    assessment.schemaUpToDate = true
    application.assessments.add(assessment)

    return Pair(application, assessment)
  }

  private fun createPlacementApplication(
    application: ApprovedPremisesApplicationEntity,
    placementDates: List<Pair<LocalDate, Int>> = emptyList(),
    isSubmitted: Boolean = true,
    configuration: (PlacementApplicationEntityFactory.() -> Unit)? = null,
  ): PlacementApplicationEntity {
    val placementApplication = placementApplicationFactory.produceAndPersist {
      withCreatedByUser(application.createdByUser)
      withApplication(application)
      withSchemaVersion(
        approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
        },
      )
      withSubmittedAt(if (isSubmitted) OffsetDateTime.now() else null)
      withDecision(if (isSubmitted) PlacementApplicationDecision.ACCEPTED else null)
      withPlacementType(PlacementType.ADDITIONAL_PLACEMENT)
      configuration?.invoke(this)
    }

    if (isSubmitted) {
      val dates = placementDates.map { (start, duration) ->
        placementDateFactory.produceAndPersist {
          withPlacementApplication(placementApplication)
          withExpectedArrival(start)
          withDuration(duration)
        }
      }
      placementApplication.placementDates.addAll(dates)
    }

    return placementApplication
  }

  private fun createPlacementRequest(
    application: ApprovedPremisesApplicationEntity,
    configuration: (PlacementRequestEntityFactory.() -> Unit)? = null,
  ) =
    placementRequestFactory.produceAndPersist {
      val assessment = application.assessments[0]

      val placementRequirements = placementRequirementsFactory.produceAndPersist {
        withApplication(application)
        withAssessment(assessment)
        withPostcodeDistrict(postCodeDistrictFactory.produceAndPersist())
        withDesirableCriteria(
          characteristicEntityFactory.produceAndPersistMultiple(5),
        )
        withEssentialCriteria(
          characteristicEntityFactory.produceAndPersistMultiple(3),
        )
      }

      withAllocatedToUser(application.createdByUser)
      withApplication(application)
      withAssessment(assessment)
      withPlacementRequirements(placementRequirements)
      configuration?.invoke(this)
    }

  private fun rowsToCsv(rows: List<Cas1WithdrawPlacementRequestSeedSeedCsvRow>): String {
    val builder = CsvBuilder()
      .withUnquotedFields(
        "placement_request_id",
        "withdrawal_reason",
      )
      .newRow()

    rows.forEach {
      builder
        .withQuotedField(it.placementRequestId.toString())
        .withQuotedField(it.withdrawalReason.name)
        .newRow()
    }

    return builder.build()
  }
}
