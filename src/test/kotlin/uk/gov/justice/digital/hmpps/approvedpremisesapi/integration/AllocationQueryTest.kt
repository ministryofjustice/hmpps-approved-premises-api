package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesPlacementApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PostCodeDistrictEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import java.time.OffsetDateTime

class AllocationQueryTest : IntegrationTestBase() {
  @Autowired
  lateinit var realUserRepository: UserRepository

  lateinit var applicationSchema: ApprovedPremisesApplicationJsonSchemaEntity
  lateinit var assessmentSchema: ApprovedPremisesAssessmentJsonSchemaEntity
  lateinit var placementApplicationSchema: ApprovedPremisesPlacementApplicationJsonSchemaEntity
  lateinit var premises: ApprovedPremisesEntity
  lateinit var postcodeDistrict: PostCodeDistrictEntity

  @BeforeEach
  fun setup() {
    applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist()
    assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist()
    placementApplicationSchema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist()
    premises = approvedPremisesEntityFactory.produceAndPersist {
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist {
          withYieldedApArea {
            apAreaEntityFactory.produceAndPersist()
          }
        }
      }
      withYieldedLocalAuthorityArea {
        localAuthorityEntityFactory.produceAndPersist()
      }
    }
    postcodeDistrict = postCodeDistrictFactory.produceAndPersist()
  }

  @Test
  fun `findQualifiedAssessorWithLeastPendingOrCompletedInLastWeekAssessments query works as described`() {
    createUserForAssessmentQuery(
      "Non Assessor",
      isAssessor = false,
      hasQualifications = false,
      numberOfPendingAssessments = 0,
      numberOfRecentCompletedAssessments = 0,
      numberOfLessRecentCompletedAssessments = 0,
    )
    createUserForAssessmentQuery(
      "Assessor with no qualifications",
      isAssessor = true,
      hasQualifications = false,
      numberOfPendingAssessments = 0,
      numberOfRecentCompletedAssessments = 0,
      numberOfLessRecentCompletedAssessments = 0,
    )
    createUserForAssessmentQuery(
      "Assessor with both Qualifications and two pending allocated Assessments",
      isAssessor = true,
      hasQualifications = false,
      numberOfPendingAssessments = 2,
      numberOfRecentCompletedAssessments = 0,
      numberOfLessRecentCompletedAssessments = 0,
    )
    createUserForAssessmentQuery(
      "Assessor with both Qualifications, zero pending allocated Assessments and one complete Assessment from the last week",
      isAssessor = true,
      hasQualifications = true,
      numberOfPendingAssessments = 0,
      numberOfRecentCompletedAssessments = 1,
      numberOfLessRecentCompletedAssessments = 2,
    )
    createUserForAssessmentQuery(
      "Assessor with both Qualifications and one pending allocated Assessment",
      isAssessor = true,
      hasQualifications = true,
      numberOfPendingAssessments = 1,
      numberOfRecentCompletedAssessments = 0,
      numberOfLessRecentCompletedAssessments = 2,
    )
    createUserForAssessmentQuery(
      "Assessor with both Qualifications and zero pending allocated Assessments",
      isAssessor = true,
      hasQualifications = true,
      numberOfPendingAssessments = 0,
      numberOfRecentCompletedAssessments = 0,
      numberOfLessRecentCompletedAssessments = 2,
    )
    val excludedAllocatedUser = createUserForAssessmentQuery(
      "Excluded User",
      isAssessor = true,
      hasQualifications = true,
      numberOfPendingAssessments = 0,
      numberOfRecentCompletedAssessments = 0,
      numberOfLessRecentCompletedAssessments = 2,
    )

    val actualAllocatedUser = realUserRepository.findQualifiedAssessorWithLeastPendingOrCompletedInLastWeekAssessments(listOf("PIPE", "WOMENS"), 2, listOf(excludedAllocatedUser.id))

    assertThat(actualAllocatedUser!!.deliusUsername).isEqualTo("Assessor with both Qualifications and zero pending allocated Assessments")
  }

  @Test
  fun `findQualifiedMatcherWithLeastPendingOrCompletedInLastWeekPlacementApplications works as expected`() {
    createUserForPlacementApplicationsQuery(
      "Non Matcher",
      isMatcher = false,
      hasQualifications = false,
      numberOfPlacementApplications = 0,
      numberOfRecentCompletedPlacementApplications = 0,
      numberOfLessRecentCompletedPlacementApplications = 0,
    )
    createUserForPlacementApplicationsQuery(
      "Matcher with no qualifications",
      isMatcher = false,
      hasQualifications = false,
      numberOfPlacementApplications = 0,
      numberOfRecentCompletedPlacementApplications = 0,
      numberOfLessRecentCompletedPlacementApplications = 0,
    )
    createUserForPlacementApplicationsQuery(
      "Matcher with both Qualifications and two pending allocated Placement Applications",
      isMatcher = true,
      hasQualifications = true,
      numberOfPlacementApplications = 2,
      numberOfRecentCompletedPlacementApplications = 0,
      numberOfLessRecentCompletedPlacementApplications = 0,
    )
    createUserForPlacementApplicationsQuery(
      "Matcher with both Qualifications, zero pending allocated Placement Applications and one complete Placement Application from the last week",
      isMatcher = true,
      hasQualifications = true,
      numberOfPlacementApplications = 0,
      numberOfRecentCompletedPlacementApplications = 1,
      numberOfLessRecentCompletedPlacementApplications = 2,
    )
    createUserForPlacementApplicationsQuery(
      "Matcher with both Qualifications and one pending allocated Placement Application",
      isMatcher = true,
      hasQualifications = true,
      numberOfPlacementApplications = 1,
      numberOfRecentCompletedPlacementApplications = 0,
      numberOfLessRecentCompletedPlacementApplications = 2,
    )
    createUserForPlacementApplicationsQuery(
      "Matcher with both Qualifications and zero pending allocated Placement Applications",
      isMatcher = true,
      hasQualifications = true,
      numberOfPlacementApplications = 0,
      numberOfRecentCompletedPlacementApplications = 0,
      numberOfLessRecentCompletedPlacementApplications = 2,
    )

    val excludedAllocatedUser = createUserForPlacementApplicationsQuery(
      "Excluded user",
      isMatcher = true,
      hasQualifications = true,
      numberOfPlacementApplications = 0,
      numberOfRecentCompletedPlacementApplications = 0,
      numberOfLessRecentCompletedPlacementApplications = 2,
    )

    val actualAllocatedUser = realUserRepository.findQualifiedMatcherWithLeastPendingOrCompletedInLastWeekPlacementApplications(listOf("PIPE", "WOMENS"), 2, listOf(excludedAllocatedUser.id))

    assertThat(actualAllocatedUser!!.deliusUsername).isEqualTo("Matcher with both Qualifications and zero pending allocated Placement Applications")
  }

  @Test
  fun `findQualifiedMatcherWithLeastPendingOrCompletedInLastWeekPlacementRequests works as expected`() {
    createUserForPlacementRequestsQuery(
      "Non Matcher",
      isMatcher = false,
      hasQualifications = false,
      numberOfPlacementRequests = 0,
      numberOfRecentCompletedPlacementRequests = 0,
      numberOfLessRecentCompletedPlacementRequests = 0,
    )
    createUserForPlacementRequestsQuery(
      "Matcher with no qualifications",
      isMatcher = false,
      hasQualifications = false,
      numberOfPlacementRequests = 0,
      numberOfRecentCompletedPlacementRequests = 0,
      numberOfLessRecentCompletedPlacementRequests = 0,
    )
    createUserForPlacementRequestsQuery(
      "Matcher with both Qualifications and two pending allocated Placement Requests",
      isMatcher = true,
      hasQualifications = true,
      numberOfPlacementRequests = 2,
      numberOfRecentCompletedPlacementRequests = 0,
      numberOfLessRecentCompletedPlacementRequests = 0,
    )
    createUserForPlacementRequestsQuery(
      "Matcher with both Qualifications, zero pending allocated Placement Requests and one complete Placement Request from the last week",
      isMatcher = true,
      hasQualifications = true,
      numberOfPlacementRequests = 0,
      numberOfRecentCompletedPlacementRequests = 1,
      numberOfLessRecentCompletedPlacementRequests = 2,
    )
    createUserForPlacementRequestsQuery(
      "Matcher with both Qualifications and one pending allocated Placement Request",
      isMatcher = true,
      hasQualifications = true,
      numberOfPlacementRequests = 1,
      numberOfRecentCompletedPlacementRequests = 0,
      numberOfLessRecentCompletedPlacementRequests = 0,
    )
    createUserForPlacementRequestsQuery(
      "Matcher with both Qualifications and zero pending allocated Placement Requests",
      isMatcher = true,
      hasQualifications = true,
      numberOfPlacementRequests = 0,
      numberOfRecentCompletedPlacementRequests = 0,
      numberOfLessRecentCompletedPlacementRequests = 2,
    )

    val excludedAllocatedUser = createUserForPlacementRequestsQuery(
      "Excluded user",
      isMatcher = true,
      hasQualifications = true,
      numberOfPlacementRequests = 0,
      numberOfRecentCompletedPlacementRequests = 0,
      numberOfLessRecentCompletedPlacementRequests = 2,
    )

    val actualAllocatedUser = realUserRepository.findQualifiedMatcherWithLeastPendingOrCompletedInLastWeekPlacementRequests(listOf("PIPE", "WOMENS"), 2, listOf(excludedAllocatedUser.id))

    assertThat(actualAllocatedUser!!.deliusUsername).isEqualTo("Matcher with both Qualifications and zero pending allocated Placement Requests")
  }

  private fun createUserForPlacementRequestsQuery(deliusUsername: String, isMatcher: Boolean, hasQualifications: Boolean, numberOfPlacementRequests: Int, numberOfRecentCompletedPlacementRequests: Int, numberOfLessRecentCompletedPlacementRequests: Int): UserEntity {
    val user = createUser(
      deliusUsername,
      if (isMatcher) listOf(UserRole.CAS1_MATCHER) else emptyList(),
      if (hasQualifications) listOf(UserQualification.PIPE, UserQualification.WOMENS) else emptyList(),
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

  private fun createUserForPlacementApplicationsQuery(deliusUsername: String, isMatcher: Boolean, hasQualifications: Boolean, numberOfPlacementApplications: Int, numberOfRecentCompletedPlacementApplications: Int, numberOfLessRecentCompletedPlacementApplications: Int): UserEntity {
    val user = createUser(
      deliusUsername,
      if (isMatcher) listOf(UserRole.CAS1_MATCHER) else emptyList(),
      if (hasQualifications) listOf(UserQualification.PIPE, UserQualification.WOMENS) else emptyList(),
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

  private fun createUserForAssessmentQuery(deliusUsername: String, isAssessor: Boolean, hasQualifications: Boolean, numberOfPendingAssessments: Int, numberOfRecentCompletedAssessments: Int, numberOfLessRecentCompletedAssessments: Int): UserEntity {
    val user = createUser(
      deliusUsername,
      if (isAssessor) listOf(UserRole.CAS1_ASSESSOR) else emptyList(),
      if (hasQualifications) listOf(UserQualification.PIPE, UserQualification.WOMENS) else emptyList(),
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

  private fun createUser(deliusUsername: String, roles: List<UserRole>, qualifications: List<UserQualification>): UserEntity {
    val user = userEntityFactory.produceAndPersist {
      withDeliusUsername(deliusUsername)
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist {
          withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
        }
      }
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

  private fun createApplicationAndAssessment(): Pair<ApprovedPremisesApplicationEntity, AssessmentEntity> {
    val user = userEntityFactory.produceAndPersist {
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist {
          withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
        }
      }
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
