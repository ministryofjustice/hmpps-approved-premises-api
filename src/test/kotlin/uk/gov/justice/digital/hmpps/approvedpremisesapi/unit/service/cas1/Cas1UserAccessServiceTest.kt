package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS1_JANITOR
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EnvironmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1UserAccessService

class Cas1UserAccessServiceTest {
  private val userService = mockk<UserService>()
  private val environmentService = mockk<EnvironmentService>()

  private val service = Cas1UserAccessService(
    userService,
    environmentService,
  )

  @Nested
  inner class CurrentUserHasPermission {

    @Test
    fun `currentUserHasPermission denies experimental permissions in production environment`() {
      every {
        userService.getUserForRequest()
      } returns UserEntityFactory().withDefaults().withRoles(CAS1_JANITOR).produce()

      every { environmentService.isNotProd() } returns false

      val result = service.currentUserHasPermission(UserPermission.CAS1_TEST_EXPERIMENTAL_PERMISSION)

      assertThat(result).isFalse
    }

    @Test
    fun `currentUserHasPermission allows experimental permissions in non-production environment`() {
      every {
        userService.getUserForRequest()
      } returns UserEntityFactory().withDefaults().withRoles(CAS1_JANITOR).produce()

      every { environmentService.isNotProd() } returns true

      val result = service.currentUserHasPermission(UserPermission.CAS1_TEST_EXPERIMENTAL_PERMISSION)

      assertThat(result).isTrue
    }
  }
}
