package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentAcceptance
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementDates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequirements
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.ProbationArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReferralHistorySystemNoteType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate

@Suppress("ReturnCount", "CyclomaticComplexMethod")
class AssessmentTest : IntegrationTestBase() {
  sealed interface ExpectedResponse {
    data class OK(val expectedAssessmentSummaries: List<AssessmentSummary>) : ExpectedResponse
    data class Error(val status: HttpStatus, val errorDetail: String) : ExpectedResponse
  }

  @Nested
  inner class AcceptAssessment {

    @Test
    fun `Accept assessment without JWT returns 401`() {
      val placementDates = PlacementDates(
        expectedArrival = LocalDate.now(),
        duration = 12,
      )

      val placementRequirements = PlacementRequirements(
        type = ApType.normal,
        location = "B74",
        radius = 50,
        essentialCriteria = listOf(PlacementCriteria.isRecoveryFocussed, PlacementCriteria.hasEnSuite),
        desirableCriteria = listOf(PlacementCriteria.isCatered, PlacementCriteria.acceptsSexOffenders),
      )

      webTestClient.post()
        .uri("/assessments/6966902f-9b7e-4fc7-96c4-b54ec02d16c9/acceptance")
        .bodyValue(
          AssessmentAcceptance(
            document = "{}",
            requirements = placementRequirements,
            placementDates = placementDates,
            notes = "Some Notes",
          ),
        )
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Accept assessment returns 200, persists decision and add system notes`() {
      givenAUser(
        staffDetail = StaffDetailFactory.staffDetail(
          probationArea = ProbationArea(
            code = "N21",
            description = randomStringMultiCaseWithNumbers(10),
          ),
        ),
        roles = listOf(UserRole.CAS3_ASSESSOR),
      ) { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->

          val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
            withProbationRegion(userEntity.probationRegion)
            withCrn(offenderDetails.otherIds.crn)
            withCreatedByUser(userEntity)
          }

          val assessment = temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
            withAllocatedToUser(userEntity)
            withApplication(application)
          }

          webTestClient.post()
            .uri("/assessments/${assessment.id}/acceptance")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              AssessmentAcceptance(
                document = mapOf("document" to "value"),
              ),
            )
            .exchange()
            .expectStatus()
            .isOk

          val persistedAssessment = temporaryAccommodationAssessmentRepository.findByIdOrNull(assessment.id)!!
          assertThat(persistedAssessment.decision).isEqualTo(AssessmentDecision.ACCEPTED)
          assertThat(persistedAssessment.document).isEqualTo("{\"document\":\"value\"}")
          assertThat(persistedAssessment.submittedAt).isNotNull
          assertThat(persistedAssessment.completedAt).isNull()

          val systemNotes = assessmentReferralSystemNoteRepository.findAll().first { it.assessment.id == assessment.id }
          assertThat(systemNotes).isNotNull
          assertThat(systemNotes.type).isEqualTo(ReferralHistorySystemNoteType.READY_TO_PLACE)
        }
      }
    }
  }
}
