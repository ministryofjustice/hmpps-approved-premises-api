package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Assessment for Approved Premises`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Assessment for Temporary Accommodation`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentClarificationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainAssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import java.time.OffsetDateTime

class AssessmentSummaryQueryTest : IntegrationTestBase() {
  @Autowired
  lateinit var realAssessmentRepository: AssessmentRepository

  @Test
  fun `assessment summary query works as described when not restricted to user`() {
    `Given a User` { user, _ ->
      `Given an Assessment for Approved Premises`(user, user, reallocated = true) { _, _ -> }
      `Given an Assessment for Approved Premises`(user, user) { apAssessment, _ ->
        val expectedApAssessmentNote = earliestUnansweredClarificationNote(apAssessment, user)
        `Given an Assessment for Temporary Accommodation`(user, user) { taAssessment, _ ->
          val expectedTaAssessmentNote = earliestUnansweredClarificationNote(taAssessment, user)

          val results: List<DomainAssessmentSummary> = realAssessmentRepository.findAllAssessmentSummariesNotReallocated()

          assertThat(results.size).isEqualTo(2)

          results.forEach {
            when (it.id) {
              apAssessment.id -> assertForAssessmentSummary(it, apAssessment, expectedApAssessmentNote.createdAt)
              taAssessment.id -> assertForAssessmentSummary(it, taAssessment, expectedTaAssessmentNote.createdAt)
              else -> fail()
            }
          }
        }
      }
    }
  }

  @Test
  fun `assessment summary query works as described when restricted to one user`() {
    `Given a User` { user1, _ ->
      `Given a User` { user2, _ ->
        `Given an Assessment for Approved Premises`(user2, user2) { _, _ -> }
        `Given an Assessment for Approved Premises`(user1, user1, reallocated = true) { _, _ -> }
        `Given an Assessment for Approved Premises`(user1, user1) { apAssessment, _ ->
          val expectedApAssessmentNote = earliestUnansweredClarificationNote(apAssessment, user1)

          `Given an Assessment for Temporary Accommodation`(user2, user2) { taAssessment, _ ->
            earliestUnansweredClarificationNote(taAssessment, user2)

            val results: List<DomainAssessmentSummary> = realAssessmentRepository.findAllAssessmentSummariesNotReallocated(user1.id.toString())

            assertThat(results.size).isEqualTo(1)
            assertForAssessmentSummary(results[0], apAssessment, expectedApAssessmentNote.createdAt)
          }
        }
      }
    }
  }

  private fun assertForAssessmentSummary(summary: DomainAssessmentSummary, assessment: AssessmentEntity, dateOfInfoRequest: OffsetDateTime) {
    assertThat(summary.id).isEqualTo(assessment.id)
    val application = assessment.application
    assertThat(summary.applicationId).isEqualTo(application.id)
    assertThat(summary.createdAt.toInstant()).isEqualTo(assessment.createdAt.toInstant())
    assertThat(summary.dateOfInfoRequest?.toInstant()).isEqualTo(dateOfInfoRequest.toInstant())
    assertThat(summary.completed).isEqualTo(assessment.decision != null)
    assertThat(summary.crn).isEqualTo(application.crn)
    when (application) {
      is ApprovedPremisesApplicationEntity -> {
        assertThat(summary.type).isEqualTo("approved-premises")
        assertThat(summary.arrivalDate?.toInstant()).isEqualTo(application.arrivalDate?.toInstant())
        assertThat(summary.riskRatings).isEqualTo("""{"roshRisks":{"status":"NotFound","value":null},"mappa":{"status":"NotFound","value":null},"tier":{"status":"NotFound","value":null},"flags":{"status":"NotFound","value":null}}""")
      }

      is TemporaryAccommodationApplicationEntity -> {
        assertThat(summary.type).isEqualTo("temporary-accommodation")
        assertThat(summary.riskRatings).isNull()
      }
    }
  }

  private fun earliestUnansweredClarificationNote(assessment: AssessmentEntity, user: UserEntity): AssessmentClarificationNoteEntity {
    assessmentClarificationNoteEntityFactory.produceAndPersistMultiple(2) {
      withAssessment(assessment)
      withCreatedBy(user)
      withCreatedAt(OffsetDateTime.now().randomDateTimeBefore(maxDays = 7))
      withResponse("Response")
    }

    assessmentClarificationNoteEntityFactory.produceAndPersist {
      withAssessment(assessment)
      withCreatedBy(user)
      withCreatedAt(OffsetDateTime.now().minusDays(3))
    }

    val earliestUnansweredClarificationNote = assessmentClarificationNoteEntityFactory.produceAndPersist {
      withAssessment(assessment)
      withCreatedBy(user)
      withCreatedAt(OffsetDateTime.now().minusDays(4).randomDateTimeBefore(maxDays = 2))
    }
    return earliestUnansweredClarificationNote
  }
}
