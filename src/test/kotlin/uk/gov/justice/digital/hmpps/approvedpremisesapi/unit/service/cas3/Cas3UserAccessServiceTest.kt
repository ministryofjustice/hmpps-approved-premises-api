package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas3

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.Cas3UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.addRoleForUnitTest
import java.util.UUID

@ExtendWith(MockKExtension::class)
class Cas3UserAccessServiceTest {

  @MockK
  lateinit var userAccessService: UserAccessService

  @MockK
  lateinit var userService: UserService

  @InjectMockKs
  lateinit var cas3UserAccessService: Cas3UserAccessService

  @Nested
  inner class VoidBedspaces {

    @BeforeEach
    fun setup() {
      every { userService.getUserForRequest() } returns user
      every { userAccessService.userCanAccessRegion(any(), any(), any()) } answers { callOriginal() }
      every { userAccessService.userHasAllRegionsAccess(any(), any()) } answers { callOriginal() }
    }

    private val probationRegionId = UUID.randomUUID()
    private val probationRegion = ProbationRegionEntityFactory()
      .withId(probationRegionId)
      .withApArea(ApAreaEntityFactory().produce())
      .produce()

    private val user = UserEntityFactory()
      .withProbationRegion(probationRegion)
      .produce()

    @Test
    fun `user can view Void Bedspaces when in probation region and has CAS_ASSESSOR role`() {
      user.addRoleForUnitTest(UserRole.CAS3_ASSESSOR)
      assertThat(cas3UserAccessService.canViewVoidBedspaces(probationRegionId = probationRegionId)).isTrue
    }

    @Test
    fun `User cannot view Void Bedspaces when not in correct probation region`() {
      user.addRoleForUnitTest(UserRole.CAS3_ASSESSOR)
      assertThat(cas3UserAccessService.canViewVoidBedspaces(probationRegionId = UUID.randomUUID())).isFalse
    }

    @Test
    fun `User cannot view Void Bedspaces without CAS_ASSESSOR role`() {
      assertThat(cas3UserAccessService.canViewVoidBedspaces(probationRegionId = probationRegionId)).isFalse
      assertThat(cas3UserAccessService.canViewVoidBedspaces(probationRegionId = probationRegionId)).isFalse
    }
  }
}
