package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole

class AllocationQueryTest : IntegrationTestBase() {
  @Autowired
  lateinit var realUserRepository: UserRepository

  @Test
  fun `findQualifiedAssessorWithLeastPendingAllocations query works as described`() {
    `Given a User that is not an Assessor`()
    `Given a User that is an Assessor with no Qualifications`()
    `Given a User that is an Assessor with both Qualifications and one pending allocated Assessment`()
    val expectedAllocatedUser = `Given a User that is an Assessor with both Qualifications and zero pending allocated Assessments`()

    val actualAllocatedUser = realUserRepository.findQualifiedAssessorWithLeastPendingAllocations(listOf("PIPE", "WOMENS"), 2)

    assertThat(actualAllocatedUser!!.id).isEqualTo(expectedAllocatedUser.id)
  }

  private fun `Given a User that is not an Assessor`(): UserEntity {
    val user = userEntityFactory.produceAndPersist {
      withDeliusUsername("NON-ASSESSOR")
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist {
          withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
        }
      }
    }

    return user
  }

  private fun `Given a User that is an Assessor with no Qualifications`(): UserEntity {
    val user = userEntityFactory.produceAndPersist {
      withDeliusUsername("ASSESSOR-NO-QUALIFICATIONS")
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist {
          withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
        }
      }
    }

    userRoleAssignmentEntityFactory.produceAndPersist {
      withUser(user)
      withRole(UserRole.ASSESSOR)
    }

    return user
  }

  private fun `Given a User that is an Assessor with both Qualifications and one pending allocated Assessment`(): UserEntity {
    val user = userEntityFactory.produceAndPersist {
      withDeliusUsername("ASSESSOR-ONE-ALLOCATION")
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist {
          withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
        }
      }
    }

    userRoleAssignmentEntityFactory.produceAndPersist {
      withUser(user)
      withRole(UserRole.ASSESSOR)
    }

    userQualificationAssignmentEntityFactory.produceAndPersist {
      withUser(user)
      withQualification(UserQualification.PIPE)
    }

    userQualificationAssignmentEntityFactory.produceAndPersist {
      withUser(user)
      withQualification(UserQualification.WOMENS)
    }

    assessmentEntityFactory.produceAndPersist {
      withAllocatedToUser(user)
      withApplication(
        approvedPremisesApplicationEntityFactory.produceAndPersist {
          withCreatedByUser(user)
          withApplicationSchema(approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist())
        },
      )
      withSubmittedAt(null)
      withAssessmentSchema(approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist())
    }

    return user
  }

  private fun `Given a User that is an Assessor with both Qualifications and zero pending allocated Assessments`(): UserEntity {
    val user = userEntityFactory.produceAndPersist {
      withDeliusUsername("ASSESSOR-ZERO-ALLOCATIONS")
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist {
          withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
        }
      }
    }

    userRoleAssignmentEntityFactory.produceAndPersist {
      withUser(user)
      withRole(UserRole.ASSESSOR)
    }

    userQualificationAssignmentEntityFactory.produceAndPersist {
      withUser(user)
      withQualification(UserQualification.PIPE)
    }

    userQualificationAssignmentEntityFactory.produceAndPersist {
      withUser(user)
      withQualification(UserQualification.WOMENS)
    }

    return user
  }
}
