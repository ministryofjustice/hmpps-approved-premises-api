package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a Probation Region`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesPlacementApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PostCodeDistrictEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.AllocationType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UserAllocationsEngine
import java.time.OffsetDateTime

class UserAllocationsEngineTest : InitialiseDatabasePerClassTestBase() {
  @Autowired
  lateinit var realUserRepository: UserRepository

  lateinit var applicationSchema: ApprovedPremisesApplicationJsonSchemaEntity
  lateinit var assessmentSchema: ApprovedPremisesAssessmentJsonSchemaEntity
  lateinit var placementApplicationSchema: ApprovedPremisesPlacementApplicationJsonSchemaEntity
  lateinit var premises: ApprovedPremisesEntity
  lateinit var postcodeDistrict: PostCodeDistrictEntity
  lateinit var expectedQualifiedAssessmentUser: UserEntity
  lateinit var expectedUnqualifiedAssessmentUser: UserEntity
  lateinit var expectedPlacementApplicationMatcher: UserEntity
  lateinit var expectedPlacementRequestMatcher: UserEntity

  lateinit var excludedMatcher: UserEntity
  lateinit var excludedAssessor: UserEntity
  lateinit var excludedPlacementApplicationMatcher: UserEntity

  @BeforeAll
  fun setup() {
    applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist()
    assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist()
    placementApplicationSchema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist()
    premises = approvedPremisesEntityFactory.produceAndPersist {
      withYieldedProbationRegion { `Given a Probation Region`() }
      withYieldedLocalAuthorityArea {
        localAuthorityEntityFactory.produceAndPersist()
      }
    }
    postcodeDistrict = postCodeDistrictFactory.produceAndPersist()

    createUserForAssessmentQuery(
      "Non Assessor",
      isAssessor = false,
      qualifications = listOf(),
      numberOfPendingAssessments = 0,
      numberOfRecentCompletedAssessments = 0,
      numberOfLessRecentCompletedAssessments = 0,
    )
    createUserForAssessmentQuery(
      "Assessor with both Qualifications and two pending allocated Assessments",
      isAssessor = true,
      qualifications = listOf(UserQualification.PIPE, UserQualification.WOMENS),
      numberOfPendingAssessments = 2,
      numberOfRecentCompletedAssessments = 0,
      numberOfLessRecentCompletedAssessments = 0,
    )
    createUserForAssessmentQuery(
      "Assessor with both Qualifications, zero pending allocated Assessments and one complete Assessment from the last week",
      isAssessor = true,
      qualifications = listOf(UserQualification.PIPE, UserQualification.WOMENS),
      numberOfPendingAssessments = 0,
      numberOfRecentCompletedAssessments = 1,
      numberOfLessRecentCompletedAssessments = 2,
    )
    createUserForAssessmentQuery(
      "Assessor with both Qualifications and one pending allocated Assessment",
      isAssessor = true,
      qualifications = listOf(UserQualification.PIPE, UserQualification.WOMENS),
      numberOfPendingAssessments = 1,
      numberOfRecentCompletedAssessments = 0,
      numberOfLessRecentCompletedAssessments = 2,
    )
    createUserForAssessmentQuery(
      "Inactive Assessor with both Qualifications and zero pending allocated Assessments",
      isAssessor = true,
      qualifications = listOf(UserQualification.PIPE, UserQualification.WOMENS),
      numberOfPendingAssessments = 0,
      numberOfRecentCompletedAssessments = 0,
      numberOfLessRecentCompletedAssessments = 2,
      isActive = false,
    )
    createUserForAssessmentQuery(
      "Assessor with one Qualification and zero pending allocated Assessments",
      isAssessor = true,
      qualifications = listOf(UserQualification.PIPE),
      numberOfPendingAssessments = 0,
      numberOfRecentCompletedAssessments = 0,
      numberOfLessRecentCompletedAssessments = 2,
    )
    excludedAssessor = createUserForAssessmentQuery(
      "Excluded User",
      isAssessor = true,
      qualifications = listOf(UserQualification.PIPE, UserQualification.WOMENS),
      isExcluded = true,
      numberOfPendingAssessments = 0,
      numberOfRecentCompletedAssessments = 0,
      numberOfLessRecentCompletedAssessments = 2,
    )

    createUserForPlacementApplicationsQuery(
      "Non Matcher",
      isMatcher = false,
      qualifications = listOf(),
      numberOfPlacementApplications = 0,
      numberOfRecentCompletedPlacementApplications = 0,
      numberOfLessRecentCompletedPlacementApplications = 0,
    )
    createUserForPlacementApplicationsQuery(
      "Matcher with no qualifications",
      isMatcher = false,
      qualifications = listOf(),
      numberOfPlacementApplications = 0,
      numberOfRecentCompletedPlacementApplications = 0,
      numberOfLessRecentCompletedPlacementApplications = 0,
    )
    createUserForPlacementApplicationsQuery(
      "Matcher with both Qualifications and two pending allocated Placement Applications",
      isMatcher = true,
      qualifications = listOf(UserQualification.PIPE, UserQualification.WOMENS),
      numberOfPlacementApplications = 2,
      numberOfRecentCompletedPlacementApplications = 0,
      numberOfLessRecentCompletedPlacementApplications = 0,
    )
    createUserForPlacementApplicationsQuery(
      "Matcher with both Qualifications, zero pending allocated Placement Applications and one complete Placement Application from the last week",
      isMatcher = true,
      qualifications = listOf(UserQualification.PIPE, UserQualification.WOMENS),
      numberOfPlacementApplications = 0,
      numberOfRecentCompletedPlacementApplications = 1,
      numberOfLessRecentCompletedPlacementApplications = 2,
    )
    createUserForPlacementApplicationsQuery(
      "Matcher with both Qualifications and one pending allocated Placement Application",
      isMatcher = true,
      qualifications = listOf(UserQualification.PIPE, UserQualification.WOMENS),
      numberOfPlacementApplications = 1,
      numberOfRecentCompletedPlacementApplications = 0,
      numberOfLessRecentCompletedPlacementApplications = 2,
    )
    createUserForPlacementApplicationsQuery(
      "Inactive Matcher with both Qualifications and zero pending allocated Placement Applications",
      isMatcher = true,
      qualifications = listOf(UserQualification.PIPE, UserQualification.WOMENS),
      numberOfPlacementApplications = 0,
      numberOfRecentCompletedPlacementApplications = 0,
      numberOfLessRecentCompletedPlacementApplications = 2,
      isActive = false,
    )
    createUserForPlacementApplicationsQuery(
      "Matcher with one Qualification and zero pending allocated Placement Applications",
      isMatcher = true,
      qualifications = listOf(UserQualification.WOMENS),
      numberOfPlacementApplications = 0,
      numberOfRecentCompletedPlacementApplications = 0,
      numberOfLessRecentCompletedPlacementApplications = 2,
    )
    excludedPlacementApplicationMatcher = createUserForPlacementApplicationsQuery(
      "Excluded Matcher with both Qualifications and zero pending allocated Placement Applications",
      isMatcher = true,
      isExcluded = true,
      qualifications = listOf(UserQualification.PIPE, UserQualification.WOMENS),
      numberOfPlacementApplications = 0,
      numberOfRecentCompletedPlacementApplications = 0,
      numberOfLessRecentCompletedPlacementApplications = 2,
    )
    createUserForPlacementRequestsQuery(
      "Non Matcher",
      isMatcher = false,
      qualifications = listOf(),
      numberOfPlacementRequests = 0,
      numberOfRecentCompletedPlacementRequests = 0,
      numberOfLessRecentCompletedPlacementRequests = 0,
    )
    createUserForPlacementRequestsQuery(
      "Matcher with no qualifications",
      isMatcher = false,
      qualifications = listOf(),
      numberOfPlacementRequests = 0,
      numberOfRecentCompletedPlacementRequests = 0,
      numberOfLessRecentCompletedPlacementRequests = 0,
    )
    createUserForPlacementRequestsQuery(
      "Matcher with both Qualifications and two pending allocated Placement Requests",
      isMatcher = true,
      qualifications = listOf(UserQualification.PIPE, UserQualification.WOMENS),
      numberOfPlacementRequests = 2,
      numberOfRecentCompletedPlacementRequests = 0,
      numberOfLessRecentCompletedPlacementRequests = 0,
    )
    createUserForPlacementRequestsQuery(
      "Matcher with both Qualifications, zero pending allocated Placement Requests and one complete Placement Request from the last week",
      isMatcher = true,
      qualifications = listOf(UserQualification.PIPE, UserQualification.WOMENS),
      numberOfPlacementRequests = 0,
      numberOfRecentCompletedPlacementRequests = 1,
      numberOfLessRecentCompletedPlacementRequests = 2,
    )
    createUserForPlacementRequestsQuery(
      "Matcher with both Qualifications and one pending allocated Placement Request",
      isMatcher = true,
      qualifications = listOf(UserQualification.PIPE, UserQualification.WOMENS),
      numberOfPlacementRequests = 1,
      numberOfRecentCompletedPlacementRequests = 0,
      numberOfLessRecentCompletedPlacementRequests = 0,
    )
    createUserForPlacementRequestsQuery(
      "Matcher with one Qualification and zero pending allocated Placement Requests",
      isMatcher = true,
      qualifications = listOf(UserQualification.PIPE),
      numberOfPlacementRequests = 0,
      numberOfRecentCompletedPlacementRequests = 0,
      numberOfLessRecentCompletedPlacementRequests = 2,
    )
    createUserForPlacementRequestsQuery(
      "Inactive Matcher with both Qualifications and zero pending allocated Placement Requests",
      isMatcher = true,
      qualifications = listOf(UserQualification.PIPE, UserQualification.WOMENS),
      numberOfPlacementRequests = 0,
      numberOfRecentCompletedPlacementRequests = 0,
      numberOfLessRecentCompletedPlacementRequests = 2,
      isActive = false,
    )
    excludedMatcher = createUserForPlacementRequestsQuery(
      "Excluded Matcher with both Qualifications and zero pending allocated Placement Requests",
      isMatcher = true,
      qualifications = listOf(UserQualification.PIPE, UserQualification.WOMENS),
      numberOfPlacementRequests = 0,
      isExcluded = true,
      numberOfRecentCompletedPlacementRequests = 0,
      numberOfLessRecentCompletedPlacementRequests = 2,
    )

    expectedQualifiedAssessmentUser = createUserForAssessmentQuery(
      "Assessor with both Qualifications and zero pending allocated Assessments",
      isAssessor = true,
      qualifications = listOf(UserQualification.PIPE, UserQualification.WOMENS),
      numberOfPendingAssessments = 0,
      numberOfRecentCompletedAssessments = 0,
      numberOfLessRecentCompletedAssessments = 2,
    )

    expectedUnqualifiedAssessmentUser = createUserForAssessmentQuery(
      "Assessor with no qualifications",
      isAssessor = true,
      qualifications = listOf(UserQualification.LAO),
      numberOfPendingAssessments = 0,
      numberOfRecentCompletedAssessments = 0,
      numberOfLessRecentCompletedAssessments = 0,
    )

    expectedPlacementApplicationMatcher = createUserForPlacementApplicationsQuery(
      "Matcher with both Qualifications and zero pending allocated Placement Applications",
      isMatcher = true,
      qualifications = listOf(UserQualification.PIPE, UserQualification.WOMENS),
      numberOfPlacementApplications = 0,
      numberOfRecentCompletedPlacementApplications = 0,
      numberOfLessRecentCompletedPlacementApplications = 2,
    )

    expectedPlacementRequestMatcher = createUserForPlacementRequestsQuery(
      "Matcher with both Qualifications and zero pending allocated Placement Requests",
      isMatcher = true,
      qualifications = listOf(UserQualification.PIPE, UserQualification.WOMENS),
      numberOfPlacementRequests = 0,
      numberOfRecentCompletedPlacementRequests = 0,
      numberOfLessRecentCompletedPlacementRequests = 2,
    )
  }

  @Test
  fun `finding a qualified assessor gets an assessor with the smallest workload and the correct qualifications`() {
    val allocationEngine = UserAllocationsEngine(realUserRepository, AllocationType.Assessment, listOf(UserQualification.PIPE, UserQualification.WOMENS), false)
    val userPool = allocationEngine.getUserPool()
    val allocatedUser = allocationEngine.getAllocatedUser()

    userPool.forEach { user ->
      val qualifications = user.qualifications.map { it.qualification }

      assertThat(user.roles.map { it.role }).contains(UserRole.CAS1_ASSESSOR)
      assertThat(qualifications).contains(UserQualification.PIPE)
      assertThat(qualifications).contains(UserQualification.WOMENS)
    }
    assertThat(allocatedUser).isNotNull()
    assertThat(allocatedUser!!.deliusUsername).isEqualTo(expectedQualifiedAssessmentUser.deliusUsername)
  }

  @Test
  fun `finding an assessor without qualifications gets an assessor with the smallest workload and without any specialist qualifications`() {
    val allocationEngine = UserAllocationsEngine(realUserRepository, AllocationType.Assessment, emptyList(), false)
    val userPool = allocationEngine.getUserPool()
    val allocatedUser = allocationEngine.getAllocatedUser()

    userPool.forEach { user ->
      val qualifications = user.qualifications.map { it.qualification }

      assertThat(user.roles.map { it.role }).contains(UserRole.CAS1_ASSESSOR)
      assertThat(qualifications).doesNotContain(UserQualification.ESAP)
      assertThat(qualifications).doesNotContain(UserQualification.PIPE)
      assertThat(qualifications).doesNotContain(UserQualification.EMERGENCY)
    }
    assertThat(allocatedUser).isNotNull()
    assertThat(allocatedUser!!.deliusUsername).isEqualTo(expectedUnqualifiedAssessmentUser.deliusUsername)
  }

  @Test
  fun `finding a matcher for a placement application gets a matcher with the smallest workload and the correct qualifications`() {
    val allocationEngine = UserAllocationsEngine(realUserRepository, AllocationType.PlacementApplication, listOf(UserQualification.PIPE, UserQualification.WOMENS), false)
    val userPool = allocationEngine.getUserPool()
    val actualAllocatedUser = allocationEngine.getAllocatedUser()

    userPool.forEach { user ->
      val qualifications = user.qualifications.map { it.qualification }

      assertThat(user.roles.map { it.role }).contains(UserRole.CAS1_MATCHER)
      assertThat(qualifications).contains(UserQualification.PIPE)
      assertThat(qualifications).contains(UserQualification.WOMENS)
    }
    assertThat(actualAllocatedUser).isNotNull()
    assertThat(actualAllocatedUser!!.deliusUsername).isEqualTo(expectedPlacementApplicationMatcher.deliusUsername)
  }

  @Test
  fun `finding a matcher for a placement request gets a matcher with the smallest workload and the correct qualifications`() {
    val allocationEngine = UserAllocationsEngine(realUserRepository, AllocationType.PlacementRequest, listOf(UserQualification.PIPE, UserQualification.WOMENS), false)
    val userPool = allocationEngine.getUserPool()
    val actualAllocatedUser = allocationEngine.getAllocatedUser()

    userPool.forEach { user ->
      val qualifications = user.qualifications.map { it.qualification }

      assertThat(user.roles.map { it.role }).contains(UserRole.CAS1_MATCHER)
      assertThat(qualifications).contains(UserQualification.PIPE)
      assertThat(qualifications).contains(UserQualification.WOMENS)
    }
    assertThat(actualAllocatedUser).isNotNull()
    assertThat(actualAllocatedUser!!.deliusUsername).isEqualTo(expectedPlacementRequestMatcher.deliusUsername)
  }

  @Test
  fun `setting excludeAutoAllocations ensures users with the auto allocation roles remain in the pool`() {
    val assessmentAllocationEngine = UserAllocationsEngine(
      realUserRepository,
      AllocationType.Assessment,
      listOf(UserQualification.PIPE, UserQualification.WOMENS),
      isLao = false,
      excludeAutoAllocations = false,
    )
    val placementApplicationAllocationEngine = UserAllocationsEngine(
      realUserRepository,
      AllocationType.PlacementApplication,
      listOf(UserQualification.PIPE, UserQualification.WOMENS),
      isLao = false,
      excludeAutoAllocations = false,
    )
    val placementRequestAllocationsEngine = UserAllocationsEngine(
      realUserRepository,
      AllocationType.PlacementRequest,
      listOf(UserQualification.PIPE, UserQualification.WOMENS),
      isLao = false,
      excludeAutoAllocations = false,
    )

    assertThat(assessmentAllocationEngine.getUserPool().map { it.id }).contains(excludedAssessor.id)
    assertThat(placementApplicationAllocationEngine.getUserPool().map { it.id }).contains(excludedPlacementApplicationMatcher.id)
    assertThat(placementRequestAllocationsEngine.getUserPool().map { it.id }).contains(excludedMatcher.id)
  }

  private fun createUserForPlacementRequestsQuery(deliusUsername: String, isMatcher: Boolean, qualifications: List<UserQualification>, numberOfPlacementRequests: Int, numberOfRecentCompletedPlacementRequests: Int, numberOfLessRecentCompletedPlacementRequests: Int, isActive: Boolean = true, isExcluded: Boolean = false): UserEntity {
    val roles = mutableListOf(
      UserRole.CAS1_EXCLUDED_FROM_PLACEMENT_APPLICATION_ALLOCATION,
    )

    if (isMatcher) {
      roles += UserRole.CAS1_MATCHER
    }

    if (isExcluded) {
      roles += UserRole.CAS1_EXCLUDED_FROM_MATCH_ALLOCATION
    }

    val user = createUser(
      "$deliusUsername for Placement Requests query",
      roles,
      qualifications,
      isActive,
    )

    repeat(numberOfPlacementRequests) {
      createPlacementRequest(user, false, 2)
    }

    repeat(numberOfRecentCompletedPlacementRequests) {
      createPlacementRequest(user, true, 3)
    }

    repeat(numberOfLessRecentCompletedPlacementRequests) {
      createPlacementRequest(user, true, 18)
    }

    return user
  }

  private fun createUserForPlacementApplicationsQuery(deliusUsername: String, isMatcher: Boolean, qualifications: List<UserQualification>, numberOfPlacementApplications: Int, numberOfRecentCompletedPlacementApplications: Int, numberOfLessRecentCompletedPlacementApplications: Int, isActive: Boolean = true, isExcluded: Boolean = false): UserEntity {
    val roles = mutableListOf(
      UserRole.CAS1_EXCLUDED_FROM_MATCH_ALLOCATION,
    )

    if (isMatcher) {
      roles += UserRole.CAS1_MATCHER
    }

    if (isExcluded) {
      roles += UserRole.CAS1_EXCLUDED_FROM_PLACEMENT_APPLICATION_ALLOCATION
    }

    val user = createUser(
      "$deliusUsername for Placement Applications query",
      roles,
      qualifications,
      isActive,
    )

    repeat(numberOfPlacementApplications) {
      placementApplicationFactory.produceAndPersist {
        withAllocatedToUser(user)
        withSchemaVersion(placementApplicationSchema)
        withCreatedByUser(user)
        withApplication(
          approvedPremisesApplicationEntityFactory.produceAndPersist {
            withCreatedByUser(user)
            withApplicationSchema(applicationSchema)
          },
        )
        withSubmittedAt(null)
      }
    }

    repeat(numberOfRecentCompletedPlacementApplications) {
      placementApplicationFactory.produceAndPersist {
        withAllocatedToUser(user)
        withSchemaVersion(placementApplicationSchema)
        withCreatedByUser(user)
        withApplication(
          approvedPremisesApplicationEntityFactory.produceAndPersist {
            withCreatedByUser(user)
            withApplicationSchema(approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist())
          },
        )
        withSubmittedAt(OffsetDateTime.now().minusDays(2))
        withCreatedAt(OffsetDateTime.now().minusDays(3))
      }
    }

    repeat(numberOfLessRecentCompletedPlacementApplications) {
      placementApplicationFactory.produceAndPersist {
        withAllocatedToUser(user)
        withSchemaVersion(placementApplicationSchema)
        withCreatedByUser(user)
        withApplication(
          approvedPremisesApplicationEntityFactory.produceAndPersist {
            withCreatedByUser(user)
            withApplicationSchema(approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist())
          },
        )
        withSubmittedAt(OffsetDateTime.now().minusDays(12))
        withCreatedAt(OffsetDateTime.now().minusDays(18))
      }
    }

    return user
  }

  private fun createUserForAssessmentQuery(deliusUsername: String, isAssessor: Boolean, qualifications: List<UserQualification>, numberOfPendingAssessments: Int, numberOfRecentCompletedAssessments: Int, numberOfLessRecentCompletedAssessments: Int, isActive: Boolean = true, isExcluded: Boolean = false): UserEntity {
    val roles = mutableListOf<UserRole>()

    if (isAssessor) {
      roles += UserRole.CAS1_ASSESSOR
    }

    if (isExcluded) {
      roles += UserRole.CAS1_EXCLUDED_FROM_ASSESS_ALLOCATION
    }

    val user = createUser(
      "$deliusUsername for Assessments query",
      roles,
      qualifications,
      isActive,
    )

    repeat(numberOfPendingAssessments) {
      approvedPremisesAssessmentEntityFactory.produceAndPersist {
        withAllocatedToUser(user)
        withApplication(
          approvedPremisesApplicationEntityFactory.produceAndPersist {
            withCreatedByUser(user)
            withApplicationSchema(applicationSchema)
          },
        )
        withSubmittedAt(null)
        withAssessmentSchema(assessmentSchema)
      }
    }

    repeat(numberOfRecentCompletedAssessments) {
      approvedPremisesAssessmentEntityFactory.produceAndPersist {
        withAllocatedToUser(user)
        withApplication(
          approvedPremisesApplicationEntityFactory.produceAndPersist {
            withCreatedByUser(user)
            withApplicationSchema(approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist())
          },
        )
        withSubmittedAt(OffsetDateTime.now().minusDays(2))
        withCreatedAt(OffsetDateTime.now().minusDays(3))
        withAssessmentSchema(approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist())
      }
    }

    repeat(numberOfLessRecentCompletedAssessments) {
      approvedPremisesAssessmentEntityFactory.produceAndPersist {
        withAllocatedToUser(user)
        withApplication(
          approvedPremisesApplicationEntityFactory.produceAndPersist {
            withCreatedByUser(user)
            withApplicationSchema(approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist())
          },
        )
        withSubmittedAt(OffsetDateTime.now().minusDays(12))
        withCreatedAt(OffsetDateTime.now().minusDays(18))
        withAssessmentSchema(approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist())
      }
    }

    return user
  }

  private fun createUser(deliusUsername: String, roles: List<UserRole>, qualifications: List<UserQualification>, isActive: Boolean = true): UserEntity {
    val user = userEntityFactory.produceAndPersist {
      withDeliusUsername(deliusUsername)
      withYieldedProbationRegion { `Given a Probation Region`() }
      withIsActive(isActive)
    }

    roles.forEach {
      userRoleAssignmentEntityFactory.produceAndPersist {
        withUser(user)
        withRole(it)
      }
    }

    qualifications.forEach {
      userQualificationAssignmentEntityFactory.produceAndPersist {
        withUser(user)
        withQualification(it)
      }
    }

    return user
  }

  private fun createApplicationAndAssessment(): Pair<ApprovedPremisesApplicationEntity, ApprovedPremisesAssessmentEntity> {
    val user = userEntityFactory.produceAndPersist {
      withYieldedProbationRegion { `Given a Probation Region`() }
    }

    val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withCreatedByUser(user)
      withApplicationSchema(applicationSchema)
    }

    val assessment = approvedPremisesAssessmentEntityFactory.produceAndPersist {
      withAllocatedToUser(user)
      withApplication(
        application,
      )
      withSubmittedAt(null)
      withAssessmentSchema(assessmentSchema)
    }

    return Pair(application, assessment)
  }

  private fun createPlacementRequest(allocatedToUser: UserEntity, withBooking: Boolean, createdAtDaysAgo: Long): PlacementRequestEntity {
    val (application, assessment) = createApplicationAndAssessment()

    return placementRequestFactory.produceAndPersist {
      withAllocatedToUser(allocatedToUser)
      withPlacementRequirements(
        placementRequirementsFactory.produceAndPersist {
          withApplication(application)
          withAssessment(assessment)
          withPostcodeDistrict(postcodeDistrict)
          withDesirableCriteria(
            characteristicEntityFactory.produceAndPersistMultiple(5),
          )
          withEssentialCriteria(
            characteristicEntityFactory.produceAndPersistMultiple(3),
          )
        },
      )
      if (withBooking) {
        withBooking(
          bookingEntityFactory.produceAndPersist {
            withServiceName(ServiceName.approvedPremises)
            withPremises(premises)
          },
        )
      }
      withCreatedAt(OffsetDateTime.now().minusDays(createdAtDaysAgo))
      withApplication(application)
      withAssessment(assessment)
    }
  }
}
