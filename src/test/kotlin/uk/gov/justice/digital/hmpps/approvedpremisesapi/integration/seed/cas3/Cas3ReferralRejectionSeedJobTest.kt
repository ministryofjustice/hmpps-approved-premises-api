package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed.cas3

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed.SeedTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.CsvBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas3.Cas3ReferralRejectionSeedCsvRow
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

    val rejectedReason = "Another reason (please add)"
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
