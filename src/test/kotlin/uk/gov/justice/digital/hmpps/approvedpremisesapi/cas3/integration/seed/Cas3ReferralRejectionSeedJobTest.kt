package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.seed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.seed.Cas3ReferralRejectionSeedCsvRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.seed.CsvBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed.SeedTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReferralHistorySystemNoteType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringLowerCase
import java.time.OffsetDateTime

class Cas3ReferralRejectionSeedJobTest : SeedTestBase() {
  @Test
  fun `Reject an assessment update the assessment status to Rejected`() {
    val user = givenAUser().first

    val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
      withProbationRegion(user.probationRegion)
      withCreatedByUser(user)
      withSubmittedAt(OffsetDateTime.now().minusDays(10))
    }

    val assessment = temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
      withApplication(application)
      withAllocatedToUser(user)
    }

    val rejectedReason = "CAS1/AP alternative suitable accommodation provided"
    val rejectedReasonDetail = randomStringLowerCase(30)

    seed(
      SeedFileType.temporaryAccommodationReferralRejection,
      rowsToCsv(listOf(Cas3ReferralRejectionSeedCsvRow(assessment.id, rejectedReason, rejectedReasonDetail, false, user.deliusUsername))),
    )

    val persistedAssessment = temporaryAccommodationAssessmentRepository.findByIdOrNull(assessment.id)!!
    assertThat(persistedAssessment).isNotNull
    assertThat(persistedAssessment.decision).isEqualTo(AssessmentDecision.REJECTED)
    assertThat(persistedAssessment.completedAt).isNull()
    assertThat(persistedAssessment.referralRejectionReason?.name).isEqualTo(rejectedReason)
    assertThat(persistedAssessment.referralRejectionReasonDetail).isEqualTo(rejectedReasonDetail)
    assertThat(persistedAssessment.isWithdrawn).isFalse()

    assertThat(logEntries).noneMatch { it.level == "error" }

    val notes = assessmentReferralSystemNoteRepository.findAll().filter { it.assessment.id == assessment.id }
    assertThat(notes).hasSize(1)
    assertThat(notes[0].type).isEqualTo(ReferralHistorySystemNoteType.REJECTED)
    assertThat(notes[0].createdByUser.id).isEqualTo(user.id)
  }

  @Test
  fun `If any row fails no assessments are rejected and no notes are added`() {
    val user = givenAUser().first

    val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
      withProbationRegion(user.probationRegion)
      withCreatedByUser(user)
      withSubmittedAt(OffsetDateTime.now().minusDays(10))
    }

    val assessment = temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
      withApplication(application)
      withAllocatedToUser(user)
      withDecision(null)
    }

    val otherAssessment = temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
      withApplication(application)
      withAllocatedToUser(user)
    }

    val validReason = "CAS1/AP alternative suitable accommodation provided"
    val invalidReason = "invalid rejection reason"
    val detail = "rejection reason detail"

    seed(
      SeedFileType.temporaryAccommodationReferralRejection,
      rowsToCsv(
        listOf(
          Cas3ReferralRejectionSeedCsvRow(assessment.id, validReason, detail, false, user.deliusUsername),
          Cas3ReferralRejectionSeedCsvRow(otherAssessment.id, invalidReason, detail, false, user.deliusUsername),
        ),
      ),
    )

    assertError(2, "Rejection reason $invalidReason not found")

    val persistedAssessment = temporaryAccommodationAssessmentRepository.findByIdOrNull(assessment.id)!!
    assertThat(persistedAssessment.decision).isNull()
    assertThat(persistedAssessment.submittedAt).isNull()
    assertThat(persistedAssessment.referralRejectionReason).isNull()

    assertThat(assessmentReferralSystemNoteRepository.findAll().filter { it.assessment.id == assessment.id }).isEmpty()
  }

  private fun rowsToCsv(rows: List<Cas3ReferralRejectionSeedCsvRow>): String {
    val builder = CsvBuilder()
      .withUnquotedFields(
        "assessment_id",
        "rejection_reason",
        "rejection_reason_detail",
        "is_withdrawn",
        "delius_username",
      )
      .newRow()

    rows.forEach {
      builder
        .withQuotedFields(it.assessmentId, it.rejectionReason, it.rejectionReasonDetail!!, it.isWithdrawn, it.deliusUsername)
        .newRow()
    }

    return builder.build()
  }
}
