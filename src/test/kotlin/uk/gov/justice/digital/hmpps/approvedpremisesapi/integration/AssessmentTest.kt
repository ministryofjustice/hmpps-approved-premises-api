package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.TemporaryAccommodationApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.TemporaryAccommodationAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseAccessFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockUserAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AssessmentTransformer
import java.time.LocalDate

@Suppress("ReturnCount", "CyclomaticComplexMethod")
class AssessmentTest : IntegrationTestBase() {
  @Autowired
  lateinit var assessmentTransformer: AssessmentTransformer

  sealed interface ExpectedResponse {
    data class OK(val expectedAssessmentSummaries: List<AssessmentSummary>) : ExpectedResponse
    data class Error(val status: HttpStatus, val errorDetail: String) : ExpectedResponse
  }

  @Test
  fun `Get assessment by ID without JWT returns 401`() {
    webTestClient.get()
      .uri("/assessments/6966902f-9b7e-4fc7-96c4-b54ec02d16c9")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Get assessment by ID returns 403 when Offender is LAO and user does not have LAO qualification or pass the LAO check`() {
    givenAUser { userEntity, jwt ->
      givenAnOffender(
        offenderDetailsConfigBlock = {
          withCurrentExclusion(true)
        },
      ) { offenderDetails, inmateDetails ->

        val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
          withCrn(offenderDetails.otherIds.crn)
          withCreatedByUser(userEntity)
        }

        val assessment = approvedPremisesAssessmentEntityFactory.produceAndPersist {
          withAllocatedToUser(userEntity)
          withApplication(application)
        }

        webTestClient.get()
          .uri("/assessments/${assessment.id}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }
  }

  @Test
  fun `Get assessment by ID returns 200 when Offender is LAO and user does not have LAO qualification but does pass the LAO check`() {
    givenAUser { userEntity, jwt ->
      givenAnOffender(
        offenderDetailsConfigBlock = {
          withCurrentExclusion(true)
        },
      ) { offenderDetails, inmateDetails ->

        apDeliusContextMockUserAccess(
          CaseAccessFactory()
            .withCrn(offenderDetails.otherIds.crn)
            .withUserExcluded(false)
            .withUserRestricted(false)
            .produce(),
          userEntity.deliusUsername,
        )

        val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
          withCrn(offenderDetails.otherIds.crn)
          withCreatedByUser(userEntity)
        }

        val assessment = approvedPremisesAssessmentEntityFactory.produceAndPersist {
          withAllocatedToUser(userEntity)
          withApplication(application)
        }

        webTestClient.get()
          .uri("/assessments/${assessment.id}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              assessmentTransformer.transformJpaToApi(
                assessment,
                PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetails),
              ),
            ),
          )
      }
    }
  }

  @Test
  fun `Get Temporary Accommodation assessment by ID returns 200 with summary data transformed correctly`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->

        val application = produceAndPersistTemporaryAccommodationApplication(offenderDetails.otherIds.crn, userEntity) {
          withArrivalDate(LocalDate.now().minusDays(100))
          withPersonReleaseDate(LocalDate.now().minusDays(100))
        }

        val assessment =
          produceAndPersistTemporaryAccommodationAssessmentEntity(userEntity, application) {
            withSummaryData("{\"num\":50,\"text\":\"Hello world!\"}")
          }

        webTestClient.get()
          .uri("/assessments/${assessment.id}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("$.summaryData.num").isEqualTo(50)
          .jsonPath("$.summaryData.text").isEqualTo("Hello world!")
      }
    }
  }

  @Test
  fun `GET temporary accommodation assessment contains updated release date and accommodation required from date`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR, UserRole.CAS3_REPORTER)) { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->

        val originalDate = LocalDate.now().plusDays(10)

        val application = produceAndPersistTemporaryAccommodationApplication(offenderDetails.otherIds.crn, userEntity)

        val assessment =
          produceAndPersistTemporaryAccommodationAssessmentEntity(userEntity, application) {
            withAccommodationRequiredFromDate(originalDate)
            withReleaseDate(originalDate)
          }

        webTestClient.get()
          .uri("/assessments/${assessment.id}")
          .header("Authorization", "Bearer $jwt")
          .header("X-service-name", "temporary-accommodation")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("$.releaseDate").isEqualTo(originalDate.toString())
          .jsonPath("$.accommodationRequiredFromDate").isEqualTo(originalDate.toString())
      }
    }
  }

  @Test
  fun `GET temporary accommodation assessment contains original release date and accommodation required from date`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR, UserRole.CAS3_REPORTER)) { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->

        var releaseDate = LocalDate.now().minusDays(1)
        var accommodationDate = LocalDate.now().plusDays(1)
        val application = produceAndPersistTemporaryAccommodationApplication(offenderDetails.otherIds.crn, userEntity) {
          withPersonReleaseDate(releaseDate)
          withArrivalDate(accommodationDate)
        }

        val assessment =
          produceAndPersistTemporaryAccommodationAssessmentEntity(userEntity, application) {
            withAccommodationRequiredFromDate(null)
            withReleaseDate(null)
          }

        webTestClient.get()
          .uri("/assessments/${assessment.id}")
          .header("Authorization", "Bearer $jwt")
          .header("X-service-name", "temporary-accommodation")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("$.releaseDate").isEqualTo(releaseDate.toString())
          .jsonPath("$.accommodationRequiredFromDate").isEqualTo(accommodationDate.toString())
      }
    }
  }

  private fun produceAndPersistTemporaryAccommodationAssessmentEntity(
    user: UserEntity,
    application: TemporaryAccommodationApplicationEntity,
    nonDefaultFields: TemporaryAccommodationAssessmentEntityFactory.() -> Unit = {},
  ): TemporaryAccommodationAssessmentEntity {
    val produceAndPersist = temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
      withAllocatedToUser(user)
      withApplication(application)
      withReleaseDate(null)
      withAccommodationRequiredFromDate(null)
      nonDefaultFields()
    }
    return produceAndPersist
  }

  private fun produceAndPersistTemporaryAccommodationApplication(
    crn: String,
    user: UserEntity,
    nonDefaultFields: TemporaryAccommodationApplicationEntityFactory.() -> Unit = {},
  ): TemporaryAccommodationApplicationEntity = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
    withCrn(crn)
    withCreatedByUser(user)
    withProbationRegion(user.probationRegion)
    nonDefaultFields()
  }
}
