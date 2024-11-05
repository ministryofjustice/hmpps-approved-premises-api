package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnAssessmentForApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnAssessmentForTemporaryAccommodation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentClarificationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainAssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import java.time.OffsetDateTime

class AssessmentSummaryQueryTest : IntegrationTestBase() {
  @Autowired
  lateinit var realAssessmentRepository: AssessmentRepository

  @Test
  fun `Approved Premises assessment summary query works as described when not restricted to user`() {
    givenAUser { user1, _ ->
      givenAnAssessmentForApprovedPremises(user1, user1, reallocated = true) { _, _ -> }
      givenAnAssessmentForApprovedPremises(user1, user1) { apAssessment, _ ->
        givenAnAssessmentForApprovedPremises(user1, user1, data = null) { notStartedApAssessment, _ ->
          givenAnAssessmentForTemporaryAccommodation(user1, user1) { taAssessment, _ ->
            givenAUser { user2, _ ->

              val u2Assessment = approvedPremisesAssessmentEntityFactory.produceAndPersist {
                val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
                  withApplicationSchema(apAssessment.application.schemaVersion)
                  withCreatedByUser(user2)
                  withArrivalDate(OffsetDateTime.now().minusDays(1))
                }
                withAllocatedToUser(user2)
                withAssessmentSchema(apAssessment.schemaVersion)
                withApplication(application)
                withDecision(AssessmentDecision.ACCEPTED)
              }

              val results: List<DomainAssessmentSummary> =
                realAssessmentRepository.findAllApprovedPremisesAssessmentSummariesNotReallocated().toList()

              assertThat(results.size).isEqualTo(3)

              results.forEach {
                when (it.id) {
                  u2Assessment.id -> assertForAssessmentSummary(it, u2Assessment)
                  apAssessment.id -> assertForAssessmentSummary(it, apAssessment)
                  taAssessment.id -> fail("Did not expect a Temporary Accommodation Assessment when fetching Approved Premises Assessment summaries")
                  notStartedApAssessment.id -> assertForAssessmentSummary(it, notStartedApAssessment)
                  else -> fail()
                }
              }
            }
          }
        }
      }
    }
  }

  @Test
  fun `Approved Premises assessment summary query works as described when restricted to one user`() {
    givenAUser { user1, _ ->
      givenAUser { user2, _ ->
        givenAnAssessmentForApprovedPremises(user2, user2) { _, _ -> }
        givenAnAssessmentForApprovedPremises(user1, user1, reallocated = true) { _, _ -> }
        givenAnAssessmentForApprovedPremises(user1, user1) { apAssessment, _ ->
          givenAnAssessmentForTemporaryAccommodation(user2, user2) { taAssessment, _ ->
            earliestUnansweredClarificationNote(taAssessment, user2)

            val results: List<DomainAssessmentSummary> = realAssessmentRepository.findAllApprovedPremisesAssessmentSummariesNotReallocated(user1.id.toString()).toList()

            assertThat(results.size).isEqualTo(1)
            assertForAssessmentSummary(results[0], apAssessment)
          }
        }
      }
    }
  }

  @Test
  fun `Temporary Accommodation assessment summary query returns assessments in the region`() {
    givenAUser { user1, _ ->
      givenAnAssessmentForTemporaryAccommodation(user1, user1, reallocated = true) { _, _ -> }
      givenAnAssessmentForTemporaryAccommodation(user1, user1) { taAssessment, _ ->
        givenAnAssessmentForTemporaryAccommodation(user1, user1, data = null) { notStartedTaAssessment, _ ->
          givenAnAssessmentForApprovedPremises(user1, user1) { apAssessment, _ ->
            givenAUser(probationRegion = user1.probationRegion) { user2, _ ->

              val u2Assessment = temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
                val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
                  withApplicationSchema(taAssessment.application.schemaVersion)
                  withCreatedByUser(user2)
                  withProbationRegion(user2.probationRegion)
                }
                withAllocatedToUser(user2)
                withAssessmentSchema(taAssessment.schemaVersion)
                withApplication(application)
                withDecision(AssessmentDecision.ACCEPTED)
              }

              val pageRequest = PageRequest.of(0, 10, Sort.by("arrivalDate").ascending())

              val results: Page<DomainAssessmentSummary> =
                realAssessmentRepository.findTemporaryAccommodationAssessmentSummariesForRegionAndCrnAndStatus(user1.probationRegion.id, null, emptyList(), pageRequest)

              assertThat(results.content.size).isEqualTo(3)

              results.content.forEach {
                when (it.id) {
                  u2Assessment.id -> assertForAssessmentSummary(it, u2Assessment)
                  taAssessment.id -> assertForAssessmentSummary(it, taAssessment)
                  apAssessment.id -> fail("Did not expect an Approved Premises Assessment when fetching Temporary Accommodation Assessment summaries")
                  notStartedTaAssessment.id -> assertForAssessmentSummary(it, notStartedTaAssessment)
                  else -> fail()
                }
              }
            }
          }
        }
      }
    }
  }

  @Test
  fun `Temporary Accommodation assessment summary query returns assessments in the region with specific crn`() {
    givenAUser { user1, _ ->
      givenAnAssessmentForTemporaryAccommodation(user1, user1, reallocated = true) { _, _ -> }
      givenAnAssessmentForTemporaryAccommodation(user1, user1) { taAssessment, _ ->
        givenAnAssessmentForTemporaryAccommodation(user1, user1, data = null) { notStartedTaAssessment, _ ->
          givenAnAssessmentForApprovedPremises(user1, user1) { apAssessment, _ ->
            givenAUser(probationRegion = user1.probationRegion) { user2, _ ->

              val u2Assessment = temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
                val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
                  withApplicationSchema(taAssessment.application.schemaVersion)
                  withCreatedByUser(user2)
                  withProbationRegion(user2.probationRegion)
                }
                withAllocatedToUser(user2)
                withAssessmentSchema(taAssessment.schemaVersion)
                withApplication(application)
                withDecision(AssessmentDecision.ACCEPTED)
              }
              val pageRequest = PageRequest.of(0, 10, Sort.by("arrivalDate").ascending())

              val results: Page<DomainAssessmentSummary> =
                realAssessmentRepository.findTemporaryAccommodationAssessmentSummariesForRegionAndCrnAndStatus(user1.probationRegion.id, taAssessment.application.crn, emptyList(), pageRequest)

              assertThat(results.content.size).isEqualTo(1)

              results.content.forEach {
                when (it.id) {
                  u2Assessment.id -> assertForAssessmentSummary(it, u2Assessment)
                  taAssessment.id -> assertForAssessmentSummary(it, taAssessment)
                  apAssessment.id -> fail("Did not expect an Approved Premises Assessment when fetching Temporary Accommodation Assessment summaries")
                  notStartedTaAssessment.id -> assertForAssessmentSummary(it, notStartedTaAssessment)
                  else -> fail()
                }
              }
            }
          }
        }
      }
    }
  }

  @Test
  fun `Temporary Accommodation assessment summary query returns no assessments when searched for specific status which doesn't exists`() {
    givenAUser { user1, _ ->
      givenAnAssessmentForTemporaryAccommodation(user1, user1, reallocated = true) { _, _ -> }
      givenAnAssessmentForTemporaryAccommodation(user1, user1) { taAssessment, _ ->
        givenAnAssessmentForTemporaryAccommodation(user1, user1, data = null) { notStartedTaAssessment, _ ->
          givenAnAssessmentForApprovedPremises(user1, user1) { apAssessment, _ ->
            givenAUser(probationRegion = user1.probationRegion) { user2, _ ->

              val u2Assessment = temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
                val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
                  withApplicationSchema(taAssessment.application.schemaVersion)
                  withCreatedByUser(user2)
                  withProbationRegion(user2.probationRegion)
                }
                withAllocatedToUser(user2)
                withAssessmentSchema(taAssessment.schemaVersion)
                withApplication(application)
                withDecision(AssessmentDecision.ACCEPTED)
              }
              val pageRequest = PageRequest.of(0, 10, Sort.by("arrivalDate").ascending())

              val results: Page<DomainAssessmentSummary> =
                realAssessmentRepository.findTemporaryAccommodationAssessmentSummariesForRegionAndCrnAndStatus(user1.probationRegion.id, null, listOf("IN_REVIEW"), pageRequest)

              assertThat(results.content.size).isEqualTo(0)
            }
          }
        }
      }
    }
  }

  private fun assertForAssessmentSummary(summary: DomainAssessmentSummary, assessment: AssessmentEntity) {
    assertThat(summary.id).isEqualTo(assessment.id)
    val application = assessment.application
    assertThat(summary.applicationId).isEqualTo(application.id)
    assertThat(summary.createdAt).isEqualTo(assessment.createdAt.toInstant())
    assertThat(summary.decision).isEqualTo(assessment.decision?.name)
    assertThat(summary.crn).isEqualTo(application.crn)
    when (application) {
      is ApprovedPremisesApplicationEntity -> {
        assertThat(summary.type).isEqualTo("approved-premises")
        assertThat(summary.completed).isEqualTo(assessment.decision != null)
        assertThat(summary.arrivalDate).isEqualTo(application.arrivalDate?.toInstant())
        assertThat(summary.riskRatings).isEqualTo("""{"roshRisks":{"status":"NotFound","value":null},"mappa":{"status":"NotFound","value":null},"tier":{"status":"NotFound","value":null},"flags":{"status":"NotFound","value":null}}""")
      }

      is TemporaryAccommodationApplicationEntity -> {
        assertThat(summary.type).isEqualTo("temporary-accommodation")
        assertThat(summary.completed).isEqualTo((assessment as TemporaryAccommodationAssessmentEntity).completedAt != null)
        assertThat(summary.riskRatings).isEqualTo("""{"roshRisks":{"status":"NotFound","value":null},"mappa":{"status":"NotFound","value":null},"tier":{"status":"NotFound","value":null},"flags":{"status":"NotFound","value":null}}""")
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
