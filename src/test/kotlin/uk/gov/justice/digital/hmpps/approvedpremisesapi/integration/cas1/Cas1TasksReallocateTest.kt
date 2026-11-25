package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewReallocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Reallocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.Cas1NotifyTemplates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnAssessmentForApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class Cas1TasksReallocateTest : IntegrationTestBase() {
  @Autowired
  lateinit var userTransformer: UserTransformer

  @BeforeEach
  fun stubBankHolidaysApi() {
    govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()
  }

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER"], mode = EnumSource.Mode.EXCLUDE)
  fun `Return 400 bad request for users with roles other than CAS1_CRU_MEMBER`(userRole: UserRole) {
    val (_, jwt) = givenAUser(roles = listOf(userRole))
    webTestClient.post()
      .uri("/cas1/tasks/assessment/9c7abdf6-fd39-4670-9704-98a5bbfec95e/allocations")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewReallocation(
          userId = UUID.randomUUID(),
        ),
      )
      .exchange()
      .expectStatus()
      .isBadRequest
  }

  @Test
  fun `Reallocate application to different assessor without JWT returns 401`() {
    webTestClient.post()
      .uri("/cas1/tasks/assessment/9c7abdf6-fd39-4670-9704-98a5bbfec95e/allocations")
      .bodyValue(
        NewReallocation(
          userId = UUID.randomUUID(),
        ),
      )
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Reallocate application to different assessor without CAS1_CRU_MEMBER role returns 403`() {
    givenAUser { _, jwt ->
      webTestClient.post()
        .uri("/cas1/tasks/assessment/9c7abdf6-fd39-4670-9704-98a5bbfec95e/allocations")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.approvedPremises.value)
        .bodyValue(
          NewReallocation(
            userId = UUID.randomUUID(),
          ),
        )
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Test
  fun `Reallocate assessment to different assessor returns 201, creates new assessment, deallocates old one, sends emails`() {
    givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { _, jwt ->
      givenAUser(roles = listOf(UserRole.CAS1_ASSESSOR)) { currentlyAllocatedUser, _ ->
        givenAUser(
          roles = listOf(UserRole.CAS1_ASSESSOR),
        ) { assigneeUser, _ ->
          givenAnOffender { offenderDetails, _ ->
            givenAnAssessmentForApprovedPremises(
              allocatedToUser = currentlyAllocatedUser,
              createdByUser = currentlyAllocatedUser,
              crn = offenderDetails.otherIds.crn,
            ) { existingAssessment, application ->

              webTestClient.post()
                .uri("/cas1/tasks/assessment/${existingAssessment.id}/allocations")
                .header("Authorization", "Bearer $jwt")
                .header("X-Service-Name", ServiceName.approvedPremises.value)
                .bodyValue(
                  NewReallocation(
                    userId = assigneeUser.id,
                  ),
                )
                .exchange()
                .expectStatus()
                .isCreated
                .expectBody()
                .json(
                  jsonMapper.writeValueAsString(
                    Reallocation(
                      user = userTransformer.transformJpaToApi(
                        assigneeUser,
                        ServiceName.approvedPremises,
                      ) as ApprovedPremisesUser,
                      taskType = TaskType.assessment,
                    ),
                  ),
                )

              val assessments = approvedPremisesAssessmentRepository.findAll()

              assertThat(assessments.first { it.id == existingAssessment.id }.reallocatedAt).isNotNull
              assertThat(assessments)
                .anyMatch { it.application.id == application.id && it.allocatedToUser!!.id == assigneeUser.id }

              emailAsserter.assertEmailsRequestedCount(2)
              emailAsserter.assertEmailRequested(currentlyAllocatedUser.email!!, Cas1NotifyTemplates.ASSESSMENT_DEALLOCATED)
              emailAsserter.assertEmailRequested(assigneeUser.email!!, Cas1NotifyTemplates.ASSESSMENT_ALLOCATED)
            }
          }
        }
      }
    }
  }

  @Test
  fun `Reallocate assessment to different assessor returns an error if the assessment has already been allocated`() {
    givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { _, jwt ->
      givenAUser(roles = listOf(UserRole.CAS1_ASSESSOR)) { user, _ ->
        givenAUser(
          roles = listOf(UserRole.CAS1_ASSESSOR),
        ) { assigneeUser, _ ->
          givenAnOffender { offenderDetails, _ ->
            givenAnAssessmentForApprovedPremises(
              allocatedToUser = user,
              createdByUser = user,
              crn = offenderDetails.otherIds.crn,
              reallocated = true,
            ) { existingAssessment, application ->

              webTestClient.post()
                .uri("/cas1/tasks/assessment/${existingAssessment.id}/allocations")
                .header("Authorization", "Bearer $jwt")
                .header("X-Service-Name", ServiceName.approvedPremises.value)
                .bodyValue(
                  NewReallocation(
                    userId = assigneeUser.id,
                  ),
                )
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody()
                .jsonPath("title").isEqualTo("Conflict")
                .jsonPath("status").isEqualTo(409)
                .jsonPath("detail")
                .isEqualTo("This assessment has already been reallocated: ${existingAssessment.id}")
            }
          }
        }
      }
    }
  }

  @Test
  fun `Reallocating a placement application to different assessor returns 201, creates new placement application, deallocates old one`() {
    givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { _, jwt ->
      givenAUser { user, _ ->
        givenAUser(
          roles = listOf(UserRole.CAS1_ASSESSOR),
        ) { assigneeUser, _ ->
          givenAnOffender { offenderDetails, _ ->
            givenAPlacementApplication(
              createdByUser = user,
              allocatedToUser = user,
              crn = offenderDetails.otherIds.crn,
              submittedAt = OffsetDateTime.now(),
              expectedArrival = LocalDate.of(2012, 1, 1),
              requestedDuration = 15,
            ) { placementApplication ->
              webTestClient.post()
                .uri("/cas1/tasks/placement-application/${placementApplication.id}/allocations")
                .header("Authorization", "Bearer $jwt")
                .header("X-Service-Name", ServiceName.approvedPremises.value)
                .bodyValue(
                  NewReallocation(
                    userId = assigneeUser.id,
                  ),
                )
                .exchange()
                .expectStatus()
                .isCreated
                .expectBody()
                .json(
                  jsonMapper.writeValueAsString(
                    Reallocation(
                      user = userTransformer.transformJpaToApi(
                        assigneeUser,
                        ServiceName.approvedPremises,
                      ) as ApprovedPremisesUser,
                      taskType = TaskType.placementApplication,
                    ),
                  ),
                )

              val placementApplications = placementApplicationRepository.findAll()
              val allocatedPlacementApplication =
                placementApplications.find { it.allocatedToUser!!.id == assigneeUser.id }

              assertThat(placementApplications.first { it.id == placementApplication.id }.reallocatedAt).isNotNull
              assertThat(allocatedPlacementApplication).isNotNull

              assertThat(allocatedPlacementApplication!!.expectedArrival).isEqualTo(LocalDate.of(2012, 1, 1))
              assertThat(allocatedPlacementApplication.requestedDuration).isEqualTo(15)
            }
          }
        }
      }
    }
  }
}
