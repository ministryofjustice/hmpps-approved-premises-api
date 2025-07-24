package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.fasterxml.jackson.core.type.TypeReference
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentTask
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationTimelinessCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewReallocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplicationTask
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Reallocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Task
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskWrapper
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.Cas1NotifyTemplates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NameFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.InitialiseDatabasePerClassTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1CruManagementArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAPlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnAssessmentForApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnAssessmentForTemporaryAccommodation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision.ACCEPTED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision.REJECTED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.UserWorkload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.TaskTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.bodyAsListOfObjects
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.math.ceil
import kotlin.random.Random

class TasksTest {


  @Nested
  inner class GetTaskTest : IntegrationTestBase() {
    @Autowired
    lateinit var taskTransformer: TaskTransformer

    @Autowired
    lateinit var userTransformer: UserTransformer

    @Test
    fun `Request without JWT returns 401`() {
      webTestClient.get()
        .uri("/tasks/assessment/f601ff2d-b1e0-4878-8731-ccfa19a2ce84")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Unknown task type for an application returns 404`() {
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { user, jwt ->
        givenAnOffender { offenderDetails, _ ->
          givenAnAssessmentForApprovedPremises(
            allocatedToUser = user,
            createdByUser = user,
            crn = offenderDetails.otherIds.crn,
          ) { _, application ->
            webTestClient.get()
              .uri("/tasks/unknown-task/${application.id}")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isNotFound
          }
        }
      }
    }

    @Test
    fun `If request is for an application only returns active users with ASSESSOR role`() {
      val (creator, _) = givenAUser()
      val (_, jwt) = givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER))
      val (assessor, _) = givenAUser(
        roles = listOf(UserRole.CAS1_ASSESSOR),
      )
      // inactive user with correct role
      givenAUser(
        roles = listOf(UserRole.CAS1_ASSESSOR),
        isActive = false,
      )
      // user with incorrect role
      givenAUser(
        roles = listOf(UserRole.CAS1_CRU_MEMBER),
      )

      givenAnOffender { offenderDetails, _ ->
        givenAnAssessmentForApprovedPremises(
          allocatedToUser = null,
          createdByUser = creator,
          crn = offenderDetails.otherIds.crn,
          dueAt = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS),
        ) { assessment, _ ->

          webTestClient.get()
            .uri("/tasks/assessment/${assessment.id}")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .json(
              objectMapper.writeValueAsString(
                TaskWrapper(
                  task = taskTransformer.transformAssessmentToTask(
                    assessment,
                    getOffenderSummaries(offenderDetails),
                  ),
                  users = listOf(
                    userTransformer.transformJpaToAPIUserWithWorkload(
                      assessor,
                      UserWorkload(
                        0,
                        0,
                        0,
                      ),
                    ),
                  ),
                ),
              ),
            )
        }
      }
    }

    @Test
    fun `If request is for an appealed application only returns users with CAS1_APPEALS_MANAGER or CAS1_ASSESSOR role`() {
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { _, jwt ->
        givenAUser(
          roles = listOf(UserRole.CAS1_REPORT_VIEWER),
        ) { user, _ ->
          givenAUser(
            roles = listOf(UserRole.CAS1_APPEALS_MANAGER),
          ) { appealsManager, _ ->
            givenAUser(
              roles = listOf(UserRole.CAS1_ASSESSOR),
            ) { assessor, _ ->
              givenAnOffender { offenderDetails, _ ->
                givenAnAssessmentForApprovedPremises(
                  allocatedToUser = user,
                  createdByUser = user,
                  crn = offenderDetails.otherIds.crn,
                  decision = AssessmentDecision.REJECTED,
                  createdFromAppeal = true,
                  dueAt = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS),
                ) { assessment, _ ->
                  webTestClient.get()
                    .uri("/tasks/assessment/${assessment.id}")
                    .header("Authorization", "Bearer $jwt")
                    .exchange()
                    .expectStatus()
                    .isOk
                    .expectBody()
                    .json(
                      objectMapper.writeValueAsString(
                        TaskWrapper(
                          task = taskTransformer.transformAssessmentToTask(
                            assessment,
                            getOffenderSummaries(offenderDetails),
                          ),
                          users = listOf(
                            userTransformer.transformJpaToAPIUserWithWorkload(
                              appealsManager,
                              UserWorkload(
                                0,
                                0,
                                0,
                              ),
                            ),
                            userTransformer.transformJpaToAPIUserWithWorkload(
                              assessor,
                              UserWorkload(
                                0,
                                0,
                                0,
                              ),
                            ),
                          ),
                        ),
                      ),
                    )
                }
              }
            }
          }
        }
      }
    }

    @Test
    fun `If request is for an appealed application returns 0 users if no users with CAS1_APPEALS_MANAGER or CAS1_ASSESSOR role`() {
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { _, jwt ->
        givenAUser(
          roles = listOf(UserRole.CAS1_REPORT_VIEWER),
        ) { janitor, _ ->
          givenAUser(
            roles = listOf(UserRole.CAS1_CRU_MEMBER),
          ) { _, _ ->
            givenAnOffender { offenderDetails, _ ->
              givenAnAssessmentForApprovedPremises(
                allocatedToUser = null,
                createdByUser = janitor,
                crn = offenderDetails.otherIds.crn,
                decision = AssessmentDecision.REJECTED,
                createdFromAppeal = true,
                dueAt = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS),
              ) { assessment, _ ->
                webTestClient.get()
                  .uri("/tasks/assessment/${assessment.id}")
                  .header("Authorization", "Bearer $jwt")
                  .exchange()
                  .expectStatus()
                  .isOk
                  .expectBody()
                  .json(
                    objectMapper.writeValueAsString(
                      TaskWrapper(
                        task = taskTransformer.transformAssessmentToTask(
                          assessment,
                          getOffenderSummaries(offenderDetails),
                        ),
                        users = emptyList(),
                      ),
                    ),
                  )
              }
            }
          }
        }
      }
    }

    @Test
    fun `If request is for a placement application that is not submitted, return not found because a task doesn't yet exist to complete`() {
      val (creatingUser, jwt) = givenAUser()

      val placementApplication = givenAPlacementApplication(
        createdByUser = creatingUser,
        allocatedToUser = creatingUser,
        crn = "cRN123",
        submittedAt = null,
      )

      webTestClient.get()
        .uri("/tasks/placement-application/${placementApplication.id}")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @Test
    fun `If request is for a placement application only returns active users with ASSESSOR role, with correct workload`() {
      // ignored, wrong role
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER))

      // ignored, inactive
      givenAUser(roles = listOf(UserRole.CAS1_ASSESSOR), isActive = false)

      val (allocatableUser, _) = givenAUser(roles = listOf(UserRole.CAS1_ASSESSOR))

      val (creatingUser, jwt) = givenAUser()

      val (offenderDetails) = givenAnOffender()
      val crn = offenderDetails.otherIds.crn

      val placementApplication = givenAPlacementApplication(
        createdByUser = creatingUser,
        allocatedToUser = creatingUser,
        crn = crn,
        submittedAt = OffsetDateTime.now(),
        expectedArrival = LocalDate.now(),
        duration = 1,
      )

      val numAppAssessPending = 3
      repeat(numAppAssessPending) {
        createAssessment(assessedAt = null, allocatableUser, creatingUser, crn)
      }
      // withdrawn, ignored
      createAssessment(assessedAt = null, allocatableUser, creatingUser, crn, isWithdrawn = true)

      val numPlacementAppAssessPending = 4
      repeat(numPlacementAppAssessPending) {
        createPlacementApplication(assessedAt = null, allocatableUser, creatingUser, crn)
      }
      // withdrawn, ignored
      createPlacementApplication(assessedAt = null, allocatableUser, creatingUser, crn, isWithdrawn = true)

      val numAppAssessCompletedBetween1And7DaysAgo = 4
      repeat(numAppAssessCompletedBetween1And7DaysAgo) {
        val days = Random.nextInt(1, 7).toLong()
        createAssessment(OffsetDateTime.now().minusDays(days), allocatableUser, creatingUser, crn)
      }

      val numPlacementAppAssessCompletedBetween1And7DaysAgo = 2
      repeat(numPlacementAppAssessCompletedBetween1And7DaysAgo) {
        val days = Random.nextInt(1, 7).toLong()
        createPlacementApplication(OffsetDateTime.now().minusDays(days), allocatableUser, creatingUser, crn)
      }

      val numAppAssessCompletedBetween8And30DaysAgo = 4
      repeat(numAppAssessCompletedBetween8And30DaysAgo) {
        val days = Random.nextInt(8, 30).toLong()
        createAssessment(OffsetDateTime.now().minusDays(days), allocatableUser, creatingUser, crn)
      }

      val numPlacementAppAssessCompletedBetween8And30DaysAgo = 3
      repeat(numPlacementAppAssessCompletedBetween8And30DaysAgo) {
        val days = Random.nextInt(8, 30).toLong()
        createPlacementApplication(OffsetDateTime.now().minusDays(days), allocatableUser, creatingUser, crn)
      }

      // completed after 30 days ago, ignored
      repeat(10) {
        createAssessment(OffsetDateTime.now().minusDays(31), allocatableUser, creatingUser, crn)
      }
      repeat(10) {
        createPlacementApplication(OffsetDateTime.now().minusDays(31), allocatableUser, creatingUser, crn)
      }

      webTestClient.get()
        .uri("/tasks/placement-application/${placementApplication.id}")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json(
          objectMapper.writeValueAsString(
            TaskWrapper(
              task = taskTransformer.transformPlacementApplicationToTask(
                placementApplication,
                getOffenderSummaries(offenderDetails),
              ),
              users = listOf(
                userTransformer.transformJpaToAPIUserWithWorkload(
                  allocatableUser,
                  UserWorkload(
                    numTasksPending = 7,
                    numTasksCompleted7Days = 6,
                    numTasksCompleted30Days = 13,
                  ),
                ),
              ),
            ),
          ),
        )
    }

    private fun createAssessment(
      assessedAt: OffsetDateTime?,
      allocatedUser: UserEntity,
      createdByUser: UserEntity,
      crn: String,
      isWithdrawn: Boolean = false,
    ) {
      givenAnAssessmentForApprovedPremises(
        allocatedToUser = allocatedUser,
        createdByUser = createdByUser,
        crn = crn,
        decision = null,
        reallocated = false,
        submittedAt = assessedAt,
        isWithdrawn = isWithdrawn,
      )
    }

    private fun createPlacementApplication(
      assessedAt: OffsetDateTime?,
      allocatedUser: UserEntity,
      createdByUser: UserEntity,
      crn: String,
      isWithdrawn: Boolean = false,
    ) {
      givenAPlacementApplication(
        createdByUser = createdByUser,
        allocatedToUser = allocatedUser,
        submittedAt = assessedAt?.minusDays(1),
        decisionMadeAt = assessedAt,
        crn = crn,
        isWithdrawn = isWithdrawn,
      )
    }
  }

  @Nested
  inner class ReallocateTaskTest : IntegrationTestBase() {
    @Autowired
    lateinit var userTransformer: UserTransformer

    @BeforeEach
    fun stubBankHolidaysApi() {
      govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()
    }

    @Test
    fun `Reallocate application to different assessor without JWT returns 401`() {
      webTestClient.post()
        .uri("/tasks/assessment/9c7abdf6-fd39-4670-9704-98a5bbfec95e/allocations")
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
          .uri("/tasks/assessment/9c7abdf6-fd39-4670-9704-98a5bbfec95e/allocations")
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
                  .uri("/tasks/assessment/${existingAssessment.id}/allocations")
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
                    objectMapper.writeValueAsString(
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
                  .uri("/tasks/assessment/${existingAssessment.id}/allocations")
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
                duration = 15,
              ) { placementApplication ->
                webTestClient.post()
                  .uri("/tasks/placement-application/${placementApplication.id}/allocations")
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
                    objectMapper.writeValueAsString(
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
                assertThat(allocatedPlacementApplication.duration).isEqualTo(15)
              }
            }
          }
        }
      }
    }

    @Test
    fun `Reallocating a Temporary Accommodation assessment does not require a request body`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { originalUser, _ ->
        givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { expectedUser, jwt ->
          givenAnOffender { offenderDetails, _ ->
            givenAnAssessmentForTemporaryAccommodation(
              allocatedToUser = originalUser,
              createdByUser = originalUser,
              crn = offenderDetails.otherIds.crn,
            ) { assessment, _ ->
              webTestClient.post()
                .uri("/tasks/assessment/${assessment.id}/allocations")
                .header("Authorization", "Bearer $jwt")
                .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
                .bodyValue(Unit)
                .exchange()
                .expectStatus()
                .isCreated

              val result = temporaryAccommodationAssessmentRepository.findAll().first { it.id == assessment.id }
              assertThat(result.allocatedToUser).isNotNull()
              assertThat(result.allocatedToUser!!.id).isEqualTo(expectedUser.id)
            }
          }
        }
      }
    }
  }

  @Nested
  inner class DeallocateTaskTest : IntegrationTestBase() {
    @Test
    fun `Deallocate assessment without JWT returns 401 Unauthorized`() {
      webTestClient.delete()
        .uri("/tasks/assessment/9c7abdf6-fd39-4670-9704-98a5bbfec95e/allocations")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Deallocate Temporary Accommodation assessment without CAS3_ASSESSOR role returns 403 Forbidden`() {
      givenAUser { _, jwt ->
        webTestClient.delete()
          .uri("/tasks/assessment/9c7abdf6-fd39-4670-9704-98a5bbfec95e/allocations")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Deallocate Approved Premises assessment returns 403 Forbidden`() {
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { user, jwt ->
        givenAnOffender { offenderDetails, _ ->
          givenAUser { _, _ ->
            givenAnAssessmentForApprovedPremises(
              allocatedToUser = user,
              createdByUser = user,
              crn = offenderDetails.otherIds.crn,
            ) { assessment, _ ->
              webTestClient.delete()
                .uri("/tasks/assessment/${assessment.id}/allocations")
                .header("Authorization", "Bearer $jwt")
                .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
                .exchange()
                .expectStatus()
                .isForbidden
            }
          }
        }
      }
    }

    @Test
    fun `Deallocate Temporary Accommodation assessment returns 200 and unassigns the allocated user`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender { offenderDetails, _ ->
          givenAnAssessmentForTemporaryAccommodation(
            allocatedToUser = user,
            createdByUser = user,
            crn = offenderDetails.otherIds.crn,
          ) { existingAssessment, _ ->

            webTestClient.delete()
              .uri("/tasks/assessment/${existingAssessment.id}/allocations")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
              .exchange()
              .expectStatus()
              .isNoContent

            val assessment =
              temporaryAccommodationAssessmentRepository.findAll().first { it.id == existingAssessment.id }

            assertThat(assessment.allocatedToUser).isNull()
            assertThat(assessment.allocatedAt).isNull()
          }
        }
      }
    }
  }

  fun getOffenderSummaries(offenderDetails: OffenderDetailSummary): List<PersonSummaryInfoResult> = listOf(
    PersonSummaryInfoResult.Success.Full(
      offenderDetails.otherIds.crn,
      CaseSummaryFactory().withName(
        NameFactory()
          .withForename(offenderDetails.firstName)
          .withSurname(offenderDetails.surname)
          .produce(),
      )
        .produce(),
    ),
  )
}
