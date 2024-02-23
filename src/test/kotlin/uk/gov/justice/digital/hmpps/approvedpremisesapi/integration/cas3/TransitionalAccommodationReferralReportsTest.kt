package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas3

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.ExcessiveColumns.Remove
import org.jetbrains.kotlinx.dataframe.api.convertTo
import org.jetbrains.kotlinx.dataframe.api.toList
import org.jetbrains.kotlinx.dataframe.io.readExcel
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.NullSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentStatus.cas3InReview
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentStatus.cas3ReadyToPlace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentStatus.cas3Unallocated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseAccessFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.ApDeliusContext_addResponseToUserAccessCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision.ACCEPTED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision.REJECTED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS3_ASSESSOR
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS3_REPORTER
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskWithStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RoshRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.TransitionalAccommodationReferralReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateAfter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.roundNanosToMillisToAccountForLossOfPrecisionInPostgres
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class TransitionalAccommodationReferralReportsTest : IntegrationTestBase() {

  @Test
  fun `Get CAS3 referral report returns 403 Forbidden if user does not have CAS3 role`() {
    `Given a User` { _, jwt ->
      webTestClient.get()
        .uri("/cas3/reports/referrals?year=2023&month=4")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Test
  fun `Get CAS3 referral report returns 403 Forbidden if user role is CAS3_ASSESSOR role and the region is not allowed region`() {
    `Given a User`(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
      webTestClient.get()
        .uri("/cas3/reports/referrals?year=2023&month=4&probationRegionId=${UUID.randomUUID()}")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Test
  fun `Get CAS3 referral report OK response if user role is CAS3_ASSESSOR and requested regionId is allowed region`() {
    `Given a User`(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
      webTestClient.get()
        .uri("/cas3/reports/referrals?year=2023&month=4&probationRegionId=${user.probationRegion.id}")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isOk
    }
  }

  @Test
  fun `Get CAS3 referral report returns OK response if user is CAS3_REPORTER and allow access to all region when no region is requested `() {
    `Given a User`(roles = listOf(CAS3_REPORTER)) { _, jwt ->
      webTestClient.get()
        .uri("/cas3/reports/referrals?year=2024&month=1")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isOk
    }
  }

  @Test
  fun `Get CAS3 referral report returns OK response if user is CAS3_REPORTER the request region not matched to user region`() {
    `Given a User`(roles = listOf(CAS3_REPORTER)) { user, jwt ->
      webTestClient.get()
        .uri("/cas3/reports/referrals?year=2024&month=1&probationRegionId=${UUID.randomUUID()}")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isOk
    }
  }

  @Test
  fun `Get CAS3 referral report returns 403 Forbidden if a user does not have the CAS3_ASSESSOR role`() {
    `Given a User` { user, jwt ->
      webTestClient.get()
        .uri("/cas3/reports/referrals?year=2023&month=4&probationRegionId=${user.probationRegion.id}")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Test
  fun `Get CAS3 referral report returns 400 if month is provided and not within 1-12`() {
    `Given a User`(roles = listOf(CAS3_REPORTER)) { _, jwt ->
      webTestClient.get()
        .uri("/cas3/reports/referrals?year=2023&month=-1")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("$.detail").isEqualTo("month must be between 1 and 12")
    }
  }

  @ParameterizedTest
  @EnumSource
  @NullSource
  fun `Get CAS3 referral report successfully with single matching referral in the report with different 'assessmentDecision'`(assessmentDecision: AssessmentDecision?) {
    `Given a User`(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
      `Given an Offender` { offenderDetails, _ ->

        val applicationSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
        }

        val assessmentSchema = temporaryAccommodationAssessmentJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
          withAddedAt(OffsetDateTime.now())
        }

        val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
          withCrn(offenderDetails.otherIds.crn)
          withCreatedByUser(user)
          withProbationRegion(user.probationRegion)
          withApplicationSchema(applicationSchema)
          withArrivalDate(LocalDate.now().randomDateAfter(14))
          withSubmittedAt(LocalDate.parse("2024-01-01").atStartOfDay().atOffset(ZoneOffset.UTC))
          withCreatedAt(OffsetDateTime.now())
          withDutyToReferLocalAuthorityAreaName("London")
          withDutyToReferSubmissionDate(LocalDate.now())
          withHasHistoryOfArson(true)
          withIsDutyToReferSubmitted(true)
          withIsRegisteredSexOffender(true)
          withNeedsAccessibleProperty(true)
          withRiskRatings {
            withRoshRisks(
              RiskWithStatus(
                value = RoshRisks(
                  overallRisk = "High",
                  riskToChildren = "Medium",
                  riskToPublic = "Low",
                  riskToKnownAdult = "High",
                  riskToStaff = "High",
                  lastUpdated = null,
                ),
              ),
            )
          }
          withPrisonNameAtReferral("HM Hounslow")
          withPersonReleaseDate(LocalDate.now())
        }

        val assessment = temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
          withApplication(application)
          withAssessmentSchema(assessmentSchema)
          withDecision(assessmentDecision)
          withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
          assessmentDecision?.let { withSubmittedAt(OffsetDateTime.now()) }
          withRejectionRationale(if (REJECTED.name == assessmentDecision?.name) "some reason" else null)
        }
        assessment.schemaUpToDate = true

        val caseSummary = CaseSummaryFactory()
          .fromOffenderDetails(offenderDetails)
          .withPnc(offenderDetails.otherIds.pncNumber)
          .produce()

        ApDeliusContext_addResponseToUserAccessCall(
          CaseAccessFactory()
            .withCrn(offenderDetails.otherIds.crn)
            .produce(),
          user.deliusUsername,
        )

        webTestClient.get()
          .uri("/cas3/reports/referrals?year=2024&month=1&probationRegionId=${user.probationRegion.id}")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .consumeWith {
            val actual = DataFrame
              .readExcel(it.responseBody!!.inputStream())
              .convertTo<TransitionalAccommodationReferralReportRow>(Remove)
              .toList()

            assertThat(actual.size).isEqualTo(1)
            assertCorrectPersonDetail(caseSummary, actual[0])
            assertCorrectReferralDetails(assessment, actual[0])
          }
      }
    }
  }

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = ["CAS3_ASSESSOR", "CAS3_REPORTER"])
  fun `Get CAS3 referral successfully with multiple referrals in the report and filter by start and endDate period`(userRole: UserRole) {
    `Given a User`(roles = listOf(userRole)) { user, jwt ->
      `Given an Offender` { offenderDetails, _ ->
        val assessmentInReview = createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, cas3InReview, LocalDate.parse("2024-01-01"))
        val assessmentUnAllocated = createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, cas3Unallocated, LocalDate.parse("2024-01-31"))
        val assessmentReadyToPlace = createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, cas3ReadyToPlace, LocalDate.parse("2024-01-15"))
        createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, cas3ReadyToPlace, LocalDate.parse("2024-02-15"))
        createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, cas3ReadyToPlace, LocalDate.parse("2023-12-15"))

        val caseSummary = CaseSummaryFactory()
          .fromOffenderDetails(offenderDetails)
          .withPnc(offenderDetails.otherIds.pncNumber)
          .produce()

        ApDeliusContext_addResponseToUserAccessCall(
          CaseAccessFactory()
            .withCrn(offenderDetails.otherIds.crn)
            .produce(),
          user.deliusUsername,
        )

        webTestClient.get()
          .uri("/cas3/reports/referrals?year=2024&month=1&probationRegionId=${user.probationRegion.id}")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .consumeWith { res ->
            val actual = DataFrame
              .readExcel(res.responseBody!!.inputStream())
              .convertTo<TransitionalAccommodationReferralReportRow>(Remove)
              .toList()

            assertThat(actual.size).isEqualTo(3)
            assertCorrectPersonDetail(caseSummary, actual[0])
            assertCorrectReferralDetails(assessmentInReview, actual.find { it.referralId == assessmentInReview.application.id.toString() }!!)
            assertCorrectReferralDetails(assessmentUnAllocated, actual.find { it.referralId == assessmentUnAllocated.application.id.toString() }!!)
            assertCorrectReferralDetails(assessmentReadyToPlace, actual.find { it.referralId == assessmentReadyToPlace.application.id.toString() }!!)
          }
      }
    }
  }

  @Test
  fun `Get CAS3 referral successfully with referral has been offered with booking`() {
    `Given a User`(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
      `Given an Offender` { offenderDetails, _ ->

        val (premises, application) = createReferralAndAssessment(user, offenderDetails)

        bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn(offenderDetails.otherIds.crn)
          withArrivalDate(LocalDate.of(2024, 1, 1))
          withDepartureDate(LocalDate.of(2024, 1, 1))
          withApplication(application)
        }

        val caseSummary = CaseSummaryFactory()
          .fromOffenderDetails(offenderDetails)
          .withPnc(offenderDetails.otherIds.pncNumber)
          .produce()

        ApDeliusContext_addResponseToUserAccessCall(
          CaseAccessFactory()
            .withCrn(offenderDetails.otherIds.crn)
            .produce(),
          user.deliusUsername,
        )

        webTestClient.get()
          .uri("/cas3/reports/referrals?year=2024&month=1&probationRegionId=${user.probationRegion.id}")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .consumeWith {
            val actual = DataFrame
              .readExcel(it.responseBody!!.inputStream())
              .convertTo<TransitionalAccommodationReferralReportRow>(Remove)
              .toList()

            assertThat(actual.size).isEqualTo(1)
            assertCorrectPersonDetail(caseSummary, actual[0])
            assertThat(actual[0].bookingOffered).isTrue()
          }
      }
    }
  }

  @Test
  fun `Get CAS3 referral report successfully with multiple assessment for single referral`() {
    `Given a User`(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
      `Given an Offender` { offenderDetails, _ ->

        val (premises, application) = createReferralAndAssessment(user, offenderDetails)

        val applicationSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
        }
        temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
          withApplication(application)
          withAssessmentSchema(applicationSchema)
          withDecision(REJECTED)
          withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
          withSubmittedAt(OffsetDateTime.now())
        }

        bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn(offenderDetails.otherIds.crn)
          withArrivalDate(LocalDate.of(2024, 1, 1))
          withDepartureDate(LocalDate.of(2024, 1, 1))
          withApplication(application)
        }

        val caseSummary = CaseSummaryFactory()
          .fromOffenderDetails(offenderDetails)
          .withPnc(offenderDetails.otherIds.pncNumber)
          .produce()

        ApDeliusContext_addResponseToUserAccessCall(
          CaseAccessFactory()
            .withCrn(offenderDetails.otherIds.crn)
            .produce(),
          user.deliusUsername,
        )

        webTestClient.get()
          .uri("/cas3/reports/referrals?year=2024&month=1&probationRegionId=${user.probationRegion.id}")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .consumeWith {
            val actual = DataFrame
              .readExcel(it.responseBody!!.inputStream())
              .convertTo<TransitionalAccommodationReferralReportRow>(Remove)
              .toList()

            assertThat(actual.size).isEqualTo(2)
            assertCorrectPersonDetail(caseSummary, actual[0])
            assertThat(actual[0].bookingOffered).isTrue()
          }
      }
    }
  }

  @Test
  fun `Get empty CAS3 referral report when no matching application found in DB`() {
    `Given a User`(roles = listOf(CAS3_ASSESSOR)) { user, jwt ->
      `Given an Offender` { offenderDetails, _ ->
        createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, cas3InReview, LocalDate.parse("2024-01-01"))
        createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, cas3Unallocated, LocalDate.parse("2024-01-31"))
        createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, cas3ReadyToPlace, LocalDate.parse("2024-01-15"))
        createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, cas3ReadyToPlace, LocalDate.parse("2024-02-15"))
        createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, cas3ReadyToPlace, LocalDate.parse("2023-12-15"))

        ApDeliusContext_addResponseToUserAccessCall(
          CaseAccessFactory()
            .withCrn(offenderDetails.otherIds.crn)
            .produce(),
          user.deliusUsername,
        )

        webTestClient.get()
          .uri("/cas3/reports/referrals?year=2025&month=1&probationRegionId=${user.probationRegion.id}")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .consumeWith {
            val actual = DataFrame
              .readExcel(it.responseBody!!.inputStream())
              .convertTo<TransitionalAccommodationReferralReportRow>(Remove)
              .toList()

            assertThat(actual).isEmpty()
          }
      }
    }
  }

  private fun assertCorrectPersonDetail(
    expectedCaseSummary: CaseSummary,
    actualReferralRow: TransitionalAccommodationReferralReportRow,
  ) {
    val expectedName =
      (listOf(expectedCaseSummary.name.forename) + expectedCaseSummary.name.middleNames + expectedCaseSummary.name.surname).joinToString(
        " ",
      )

    assertThat(actualReferralRow.crn).isEqualTo(expectedCaseSummary.crn)
    assertThat(actualReferralRow.gender).isEqualTo(expectedCaseSummary.gender)
    assertThat(actualReferralRow.dateOfBirth).isEqualTo(expectedCaseSummary.dateOfBirth)
    assertThat(actualReferralRow.ethnicity).isEqualTo(expectedCaseSummary.profile?.ethnicity)
    assertThat(actualReferralRow.pncNumber).isEqualTo(expectedCaseSummary.pnc)
    assertThat(actualReferralRow.personName).isEqualTo(expectedName)
  }

  private fun assertCorrectReferralDetails(
    expectedAssessment: TemporaryAccommodationAssessmentEntity,
    actualReferralReportRow: TransitionalAccommodationReferralReportRow,
  ) {
    val application = expectedAssessment.application as TemporaryAccommodationApplicationEntity
    val isAssessmentRejected = REJECTED.name == expectedAssessment.decision?.name
    val rejectedDate = if (isAssessmentRejected) expectedAssessment.submittedAt else null
    assertThat(actualReferralReportRow.referralId).isEqualTo(expectedAssessment.application.id.toString())
    assertThat(actualReferralReportRow.crn).isEqualTo(expectedAssessment.application.crn)
    assertThat(actualReferralReportRow.dutyToReferMade).isEqualTo(application.isDutyToReferSubmitted)
    assertThat(actualReferralReportRow.dateDutyToReferMade).isEqualTo(application.dutyToReferSubmissionDate)
    assertThat(actualReferralReportRow.dutyToReferLocalAuthorityAreaName).isEqualTo(application.dutyToReferLocalAuthorityAreaName)
    assertThat(actualReferralReportRow.historyOfArsonOffence).isEqualTo(application.hasHistoryOfArson)
    assertThat(actualReferralReportRow.needForAccessibleProperty).isEqualTo(application.needsAccessibleProperty)
    assertThat(actualReferralReportRow.referralDate).isEqualTo(application.createdAt.toLocalDate())
    assertThat(actualReferralReportRow.referralSubmittedDate).isEqualTo(application.submittedAt?.toLocalDate())
    assertThat(actualReferralReportRow.sexOffender).isEqualTo(application.isRegisteredSexOffender)
    assertThat(actualReferralReportRow.referralRejected).isEqualTo(isAssessmentRejected)
    assertThat(actualReferralReportRow.rejectionDate).isEqualTo(rejectedDate?.toLocalDate())
    assertThat(actualReferralReportRow.rejectionReason).isEqualTo(expectedAssessment.rejectionRationale)
    assertThat(actualReferralReportRow.accommodationRequiredDate).isEqualTo(application.arrivalDate?.toLocalDate())
    assertThat(actualReferralReportRow.prisonAtReferral).isEqualTo(application.prisonNameOnCreation)
    assertThat(actualReferralReportRow.releaseDate).isEqualTo(application.personReleaseDate)
  }

  private fun createTemporaryAccommodationAssessmentForStatus(
    user: UserEntity,
    offenderDetails: OffenderDetailSummary,
    assessmentStatus: AssessmentStatus,
    submittedDate: LocalDate,
  ): TemporaryAccommodationAssessmentEntity {
    val applicationSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
    }

    val assessmentSchema = temporaryAccommodationAssessmentJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
      withAddedAt(OffsetDateTime.now())
    }

    val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
      withCrn(offenderDetails.otherIds.crn)
      withCreatedByUser(user)
      withProbationRegion(user.probationRegion)
      withApplicationSchema(applicationSchema)
      withArrivalDate(LocalDate.now().randomDateAfter(14))
      withSubmittedAt(submittedDate.atStartOfDay().atOffset(ZoneOffset.UTC))
      withCreatedAt(OffsetDateTime.now())
      withRiskRatings { PersonRisksFactory().produce() }
    }

    val assessment = temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
      withApplication(application)
      withAssessmentSchema(assessmentSchema)
      withDecision(null)
      withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())

      when (assessmentStatus) {
        AssessmentStatus.cas3Rejected -> {
          withDecision(REJECTED)
        }
        AssessmentStatus.cas3Closed -> {
          withDecision(ACCEPTED)
          withCompletedAt(OffsetDateTime.now())
        }
        cas3ReadyToPlace -> {
          withDecision(ACCEPTED)
        }
        cas3InReview -> {
          withAllocatedToUser(user)
        }
        cas3Unallocated -> {
        }
        else -> throw IllegalArgumentException("status $assessmentStatus is not supported")
      }
    }
    assessment.schemaUpToDate = true

    return assessment
  }

  private fun createReferralAndAssessment(
    user: UserEntity,
    offenderDetails: OffenderDetailSummary,
  ): Pair<TemporaryAccommodationPremisesEntity, TemporaryAccommodationApplicationEntity> {
    val applicationSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
    }

    val assessmentSchema = temporaryAccommodationAssessmentJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
      withAddedAt(OffsetDateTime.now())
    }

    val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withProbationRegion(user.probationRegion)
    }

    val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
      withCrn(offenderDetails.otherIds.crn)
      withCreatedByUser(user)
      withProbationRegion(user.probationRegion)
      withApplicationSchema(applicationSchema)
      withArrivalDate(LocalDate.now().randomDateAfter(14))
      withSubmittedAt(LocalDate.parse("2024-01-01").atStartOfDay().atOffset(ZoneOffset.UTC))
      withCreatedAt(OffsetDateTime.now())
      withDutyToReferLocalAuthorityAreaName("London")
      withDutyToReferSubmissionDate(LocalDate.now())
      withHasHistoryOfArson(true)
      withIsDutyToReferSubmitted(true)
      withIsRegisteredSexOffender(true)
      withNeedsAccessibleProperty(true)
      withRiskRatings {
        withRoshRisks(
          RiskWithStatus(
            value = RoshRisks(
              overallRisk = "High",
              riskToChildren = "Medium",
              riskToPublic = "Low",
              riskToKnownAdult = "High",
              riskToStaff = "High",
              lastUpdated = null,
            ),
          ),
        )
      }
    }

    val assessment = temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
      withApplication(application)
      withAssessmentSchema(assessmentSchema)
      withDecision(ACCEPTED)
      withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
      withSubmittedAt(OffsetDateTime.now())
    }
    assessment.schemaUpToDate = true
    return Pair(premises, application)
  }
}
