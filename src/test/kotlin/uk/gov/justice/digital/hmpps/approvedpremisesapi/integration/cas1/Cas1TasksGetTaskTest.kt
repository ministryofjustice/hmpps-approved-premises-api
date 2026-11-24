package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskWrapper
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnAssessmentForApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.UserWorkload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1TaskTransformer
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

class Cas1TasksGetTaskTest : IntegrationTestBase() {
  @Autowired
  lateinit var taskTransformer: Cas1TaskTransformer

  @Autowired
  lateinit var userTransformer: UserTransformer

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = ["CAS1_CRU_MEMBER", "CAS1_JANITOR", "CAS1_EXPERIMENTAL", "CAS1_AP_AREA_MANAGER"], mode = EnumSource.Mode.EXCLUDE)
  fun `Return 403 forbidden for users with roles other than CAS1_CRU_MEMBER, CAS1_CRU_MEMBER_FIND_AND_BOOK_BETA, CAS1_JANITOR, CAS1_AP_AREA_MANAGER`(userRole: UserRole) {
    val (user, jwt) = givenAUser(roles = listOf(userRole))

    val placementApplication = givenAPlacementApplication(
      createdByUser = user,
      allocatedToUser = user,
      crn = "CRN123",
      submittedAt = OffsetDateTime.now(),
    )

    webTestClient.get()
      .uri("/cas1/tasks/placement-application/${placementApplication.id}")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Request without JWT returns 401`() {
    webTestClient.get()
      .uri("/cas1/tasks/assessment/f601ff2d-b1e0-4878-8731-ccfa19a2ce84")
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
            .uri("/cas1/tasks/unknown-task/${application.id}")
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
          .uri("/cas1/tasks/assessment/${assessment.id}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            jsonMapper.writeValueAsString(
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
                  .uri("/cas1/tasks/assessment/${assessment.id}")
                  .header("Authorization", "Bearer $jwt")
                  .exchange()
                  .expectStatus()
                  .isOk
                  .expectBody()
                  .json(
                    jsonMapper.writeValueAsString(
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
                .uri("/cas1/tasks/assessment/${assessment.id}")
                .header("Authorization", "Bearer $jwt")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .json(
                  jsonMapper.writeValueAsString(
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
    val (creatingUser, jwt) = givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER))

    val placementApplication = givenAPlacementApplication(
      createdByUser = creatingUser,
      allocatedToUser = creatingUser,
      crn = "cRN123",
      submittedAt = null,
    )

    webTestClient.get()
      .uri("/cas1/tasks/placement-application/${placementApplication.id}")
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

    val (creatingUser, jwt) = givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER))

    val (offenderDetails) = givenAnOffender()
    val crn = offenderDetails.otherIds.crn

    val placementApplication = givenAPlacementApplication(
      createdByUser = creatingUser,
      allocatedToUser = creatingUser,
      crn = crn,
      submittedAt = OffsetDateTime.now(),
      expectedArrival = LocalDate.now(),
      requestedDuration = 1,
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
    // automatic, ignored
    createPlacementApplication(assessedAt = null, allocatableUser, creatingUser, crn, automatic = true)

    val numAppAssessCompletedBetween1And7DaysAgo = 4
    repeat(numAppAssessCompletedBetween1And7DaysAgo) {
      val days = kotlin.random.Random.nextInt(1, 7).toLong()
      createAssessment(OffsetDateTime.now().minusDays(days), allocatableUser, creatingUser, crn)
    }

    val numPlacementAppAssessCompletedBetween1And7DaysAgo = 2
    repeat(numPlacementAppAssessCompletedBetween1And7DaysAgo) {
      val days = kotlin.random.Random.nextInt(1, 7).toLong()
      createPlacementApplication(OffsetDateTime.now().minusDays(days), allocatableUser, creatingUser, crn)
    }
    // automatic, ignored
    createPlacementApplication(OffsetDateTime.now().minusDays(1), allocatableUser, creatingUser, crn, automatic = true)

    val numAppAssessCompletedBetween8And30DaysAgo = 4
    repeat(numAppAssessCompletedBetween8And30DaysAgo) {
      val days = kotlin.random.Random.nextInt(8, 30).toLong()
      createAssessment(OffsetDateTime.now().minusDays(days), allocatableUser, creatingUser, crn)
    }

    val numPlacementAppAssessCompletedBetween8And30DaysAgo = 3
    repeat(numPlacementAppAssessCompletedBetween8And30DaysAgo) {
      val days = kotlin.random.Random.nextInt(8, 30).toLong()
      createPlacementApplication(OffsetDateTime.now().minusDays(days), allocatableUser, creatingUser, crn)
    }
    // automatic, ignored
    createPlacementApplication(OffsetDateTime.now().minusDays(10), allocatableUser, creatingUser, crn, automatic = true)

    // completed after 30 days ago, ignored
    repeat(10) {
      createAssessment(OffsetDateTime.now().minusDays(31), allocatableUser, creatingUser, crn)
    }
    repeat(10) {
      createPlacementApplication(OffsetDateTime.now().minusDays(31), allocatableUser, creatingUser, crn)
    }

    webTestClient.get()
      .uri("/cas1/tasks/placement-application/${placementApplication.id}")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(
        jsonMapper.writeValueAsString(
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

  @SuppressWarnings("LongParameterList")
  private fun createPlacementApplication(
    assessedAt: OffsetDateTime?,
    allocatedUser: UserEntity,
    createdByUser: UserEntity,
    crn: String,
    isWithdrawn: Boolean = false,
    automatic: Boolean = false,
  ) {
    givenAPlacementApplication(
      createdByUser = createdByUser,
      allocatedToUser = allocatedUser,
      submittedAt = assessedAt?.minusDays(1),
      decisionMadeAt = assessedAt,
      crn = crn,
      isWithdrawn = isWithdrawn,
      automatic = automatic,
    )
  }
}
