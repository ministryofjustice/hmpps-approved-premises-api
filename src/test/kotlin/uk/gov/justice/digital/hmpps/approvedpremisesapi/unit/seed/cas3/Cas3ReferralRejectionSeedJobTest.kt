package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.seed.cas3

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReferralRejectionReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReferralRejectionReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas3.Cas3ReferralRejectionSeedCsvRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas3.Cas3ReferralRejectionSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringLowerCase
import java.time.OffsetDateTime
import java.util.UUID

class Cas3ReferralRejectionSeedJobTest {
  private val assessmentRepository = mockk<AssessmentRepository>()
  private val referralRejectionReasonRepository = mockk<ReferralRejectionReasonRepository>()

  private val seedJob = Cas3ReferralRejectionSeedJob(
    assessmentRepository = assessmentRepository,
    referralRejectionReasonRepository = referralRejectionReasonRepository,
  )

  @Test
  fun `When the assessment is not Temporary Accommodation assessment expect error`() {
    val assessmentId = UUID.randomUUID()

    every { assessmentRepository.findByIdOrNull(assessmentId) } returns
      ApprovedPremisesAssessmentEntityFactory()
        .withApplication(
          ApprovedPremisesApplicationEntityFactory()
            .withDefaults()
            .produce(),
        )
        .produce()

    assertThatThrownBy {
      seedJob.processRow(Cas3ReferralRejectionSeedCsvRow(assessmentId, "rejection reason", null, false))
    }.hasMessage("Assessment with id $assessmentId is not a temporary accommodation assessment")
  }

  @Test
  fun `When an assessment doesn't exist expect error`() {
    val assessmentId = UUID.randomUUID()

    every { assessmentRepository.findByIdOrNull(assessmentId) } returns null

    assertThatThrownBy {
      seedJob.processRow(Cas3ReferralRejectionSeedCsvRow(assessmentId, "rejection reason", randomStringLowerCase(20), false))
    }.hasMessage("Assessment with id $assessmentId not found")
  }

  @Test
  fun `When the application has been allocated expect error`() {
    val assessmentId = UUID.randomUUID()

    every { assessmentRepository.findByIdOrNull(assessmentId) } returns
      TemporaryAccommodationAssessmentEntityFactory()
        .withApplication(
          TemporaryAccommodationApplicationEntityFactory()
            .withDefaults()
            .produce(),
        )
        .withReallocatedAt(OffsetDateTime.now())
        .produce()

    assertThatThrownBy {
      seedJob.processRow(Cas3ReferralRejectionSeedCsvRow(assessmentId, "rejection reason", null, false))
    }.hasMessage("The application has been reallocated, this assessment is read only")
  }

  @Test
  fun `When the rejection reason doesn't exist expect error`() {
    val assessmentId = UUID.randomUUID()
    val notExistRejectionReason = "not exist rejection reason"

    every { assessmentRepository.findByIdOrNull(assessmentId) } returns
      TemporaryAccommodationAssessmentEntityFactory()
        .withApplication(
          TemporaryAccommodationApplicationEntityFactory()
            .withDefaults()
            .produce(),
        )
        .produce()

    every { referralRejectionReasonRepository.findByNameAndActive(notExistRejectionReason, ServiceName.temporaryAccommodation.value) } returns null

    assertThatThrownBy {
      seedJob.processRow(Cas3ReferralRejectionSeedCsvRow(assessmentId, notExistRejectionReason, null, false))
    }.hasMessage("Rejection reason $notExistRejectionReason not found")
  }

  @Test
  fun `When save an assessment and an exception happened expect logging error`() {
    val assessmentId = UUID.randomUUID()
    val assessment = TemporaryAccommodationAssessmentEntityFactory()
      .withApplication(
        TemporaryAccommodationApplicationEntityFactory()
          .withDefaults()
          .produce(),
      )
      .produce()

    every { assessmentRepository.findByIdOrNull(assessmentId) } returns assessment

    every { referralRejectionReasonRepository.findByNameAndActive("rejection reason", ServiceName.temporaryAccommodation.value) } returns
      ReferralRejectionReasonEntity(UUID.randomUUID(), "rejection reason", true, ServiceName.temporaryAccommodation.value, 1)

    every { assessmentRepository.save(any()) } throws RuntimeException("Failed to update assessment with id $assessmentId")

    assertThatThrownBy {
      seedJob.processRow(Cas3ReferralRejectionSeedCsvRow(assessmentId, "rejection reason", null, false))
    }.hasMessage("Failed to update assessment with id $assessmentId")
  }
}
