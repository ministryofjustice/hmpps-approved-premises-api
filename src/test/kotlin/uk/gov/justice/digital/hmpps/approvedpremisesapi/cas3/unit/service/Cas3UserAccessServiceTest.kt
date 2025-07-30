package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.addRoleForUnitTest
import java.util.UUID

@ExtendWith(MockKExtension::class)
class Cas3UserAccessServiceTest {

  @MockK
  lateinit var userService: UserService

  @InjectMockKs
  lateinit var cas3UserAccessService: Cas3UserAccessService

  @BeforeEach
  fun setup() {
    user = UserEntityFactory()
      .withProbationRegion(probationRegion)
      .withRoleEnums(mutableListOf())
      .produce()

    every { userService.getUserForRequest() } returns user
  }

  private val probationRegionId = UUID.randomUUID()
  private val probationRegion = ProbationRegionEntityFactory()
    .withId(probationRegionId)
    .withApArea(ApAreaEntityFactory().produce())
    .produce()

  lateinit var user: UserEntity

  @Nested
  inner class UserCanViewVoidBedspaces {
    @Test
    fun `User can view Void Bedspaces when in probation region and has CAS3_ASSESSOR role`() {
      user.addRoleForUnitTest(UserRole.CAS3_ASSESSOR)
      assertThat(cas3UserAccessService.canViewVoidBedspaces(probationRegionId = probationRegionId)).isTrue
    }

    @Test
    fun `User cannot view Void Bedspaces when not in correct probation region`() {
      user.addRoleForUnitTest(UserRole.CAS3_ASSESSOR)
      assertThat(cas3UserAccessService.canViewVoidBedspaces(probationRegionId = UUID.randomUUID())).isFalse
    }

    @Test
    fun `User can view void bedspaces if not in same region but has CAS3_REPORTER and CAS3_ASSESSOR roles`() {
      user.addRoleForUnitTest(UserRole.CAS3_ASSESSOR)
      user.addRoleForUnitTest(UserRole.CAS3_REPORTER)
      assertThat(cas3UserAccessService.canViewVoidBedspaces(probationRegionId = UUID.randomUUID())).isTrue
    }

    @Test
    fun `User cannot view Void Bedspaces without CAS3_ASSESSOR role`() {
      assertThat(cas3UserAccessService.canViewVoidBedspaces(probationRegionId = probationRegionId)).isFalse
    }
  }

  @Nested
  inner class UserCanAccessRegion {
    @Test
    fun `User can access reqion when in same region`() {
      assertThat(cas3UserAccessService.userCanAccessRegion(user, probationRegionId)).isTrue
    }

    @Test
    fun `User can access reqion when they have CAS3_REPORTER role`() {
      user.addRoleForUnitTest(UserRole.CAS3_REPORTER)
      assertThat(cas3UserAccessService.userCanAccessRegion(user, UUID.randomUUID())).isTrue
    }

    @Test
    fun `User cannot access reqion when they do not have CAS3_REPORTER or in different region`() {
      assertThat(cas3UserAccessService.userCanAccessRegion(user, UUID.randomUUID())).isFalse
    }
  }
}
