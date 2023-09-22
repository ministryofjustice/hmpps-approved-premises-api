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
      qualifications = listOf(),
      numberOfPendingAssessments = 0,
      numberOfRecentCompletedAssessments = 0,
      numberOfLessRecentCompletedAssessments = 0,
    )
    createUserForAssessmentQuery(
      "Assessor with no qualifications",
      isAssessor = true,
      qualifications = listOf(),
      numberOfPendingAssessments = 0,
      numberOfRecentCompletedAssessments = 0,
      numberOfLessRecentCompletedAssessments = 0,
    )
    createUserForAssessmentQuery(
      "Assessor with both Qualifications and two pending allocated Assessments",
      isAssessor = true,
      qualifications = listOf(),
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
      "Assessor with both Qualifications and zero pending allocated Assessments",
      isAssessor = true,
      qualifications = listOf(UserQualification.PIPE, UserQualification.WOMENS),
      numberOfPendingAssessments = 0,
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
    createUserForAssessmentQuery(
      "Excluded User",
      isAssessor = true,
      qualifications = listOf(UserQualification.PIPE, UserQualification.WOMENS),
      isExcluded = true,
      numberOfPendingAssessments = 0,
      numberOfRecentCompletedAssessments = 0,
      numberOfLessRecentCompletedAssessments = 2,
    )

    val actualAllocatedUserWithQualifications = realUserRepository.findQualifiedAssessorWithLeastPendingOrCompletedInLastWeekAssessments(listOf("PIPE", "WOMENS"), 2)

    assertThat(actualAllocatedUserWithQualifications!!.deliusUsername).isEqualTo("Assessor with both Qualifications and zero pending allocated Assessments")
  }

  @Test
  fun `findQualifiedMatcherWithLeastPendingOrCompletedInLastWeekPlacementApplications works as expected`() {
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
    createUserForPlacementApplicationsQuery(
      "Matcher with both Qualifications and zero pending allocated Placement Applications",
      isMatcher = true,
      qualifications = listOf(UserQualification.PIPE, UserQualification.WOMENS),
      numberOfPlacementApplications = 0,
      numberOfRecentCompletedPlacementApplications = 0,
      numberOfLessRecentCompletedPlacementApplications = 2,
    )
    createUserForPlacementApplicationsQuery(
      "Excluded Matcher with both Qualifications and zero pending allocated Placement Applications",
      isMatcher = true,
      isExcluded = true,
      qualifications = listOf(UserQualification.PIPE, UserQualification.WOMENS),
      numberOfPlacementApplications = 0,
      numberOfRecentCompletedPlacementApplications = 0,
      numberOfLessRecentCompletedPlacementApplications = 2,
    )

    val actualAllocatedUser = realUserRepository.findQualifiedMatcherWithLeastPendingOrCompletedInLastWeekPlacementApplications(listOf("PIPE", "WOMENS"), 2)

    assertThat(actualAllocatedUser!!.deliusUsername).isEqualTo("Matcher with both Qualifications and zero pending allocated Placement Applications")
  }

  @Test
  fun `findQualifiedMatcherWithLeastPendingOrCompletedInLastWeekPlacementRequests works as expected`() {
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
      qualifications = listOf(UserQualification.PIPE, UserQualification.WOMENS),
      numberOfPlacementRequests = 0,
      numberOfRecentCompletedPlacementRequests = 0,
      numberOfLessRecentCompletedPlacementRequests = 2,
    )
    createUserForPlacementRequestsQuery(
      "Matcher with both Qualifications and zero pending allocated Placement Requests",
      isMatcher = true,
      qualifications = listOf(UserQualification.PIPE, UserQualification.WOMENS),
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
    createUserForPlacementRequestsQuery(
      "Excluded Matcher with both Qualifications and zero pending allocated Placement Requests",
      isMatcher = true,
      qualifications = listOf(UserQualification.PIPE, UserQualification.WOMENS),
      numberOfPlacementRequests = 0,
      isExcluded = true,
      numberOfRecentCompletedPlacementRequests = 0,
      numberOfLessRecentCompletedPlacementRequests = 2,
    )

    val actualAllocatedUser = realUserRepository.findQualifiedMatcherWithLeastPendingOrCompletedInLastWeekPlacementRequests(listOf("PIPE", "WOMENS"), 2)

    assertThat(actualAllocatedUser!!.deliusUsername).isEqualTo("Matcher with both Qualifications and zero pending allocated Placement Requests")
  }

  private fun createUserForPlacementRequestsQuery(deliusUsername: String, isMatcher: Boolean, qualifications: List<UserQualification>, numberOfPlacementRequests: Int, numberOfRecentCompletedPlacementRequests: Int, numberOfLessRecentCompletedPlacementRequests: Int, isActive: Boolean = true, isExcluded: Boolean = false): UserEntity {
    val roles = mutableListOf<UserRole>()

    if (isMatcher) {
      roles += UserRole.CAS1_MATCHER
    }

    if (isExcluded) {
      roles += UserRole.CAS1_EXCLUDED_FROM_MATCH_ALLOCATION
    }

    val user = createUser(
      deliusUsername,
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
    val roles = mutableListOf<UserRole>()

    if (isMatcher) {
      roles += UserRole.CAS1_MATCHER
    }

    if (isExcluded) {
      roles += UserRole.CAS1_EXCLUDED_FROM_PLACEMENT_APPLICATION_ALLOCATION
    }

    val user = createUser(
      deliusUsername,
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
      deliusUsername,
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
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist {
          withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
        }
      }
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
