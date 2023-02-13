package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import java.util.UUID
import javax.servlet.http.HttpServletRequest

class UserAccessServiceTest {
  private val userService = mockk<UserService>()
  private val currentRequest = mockk<HttpServletRequest>()

  private val userAccessService = UserAccessService(
    userService,
    currentRequest,
  )

  private val probationRegionId = UUID.randomUUID()
  private val user = UserEntityFactory()
    .withProbationRegion(
      ProbationRegionEntityFactory()
        .withId(probationRegionId)
        .withApArea(
          ApAreaEntityFactory()
            .produce()
        )
        .produce()
    )
    .produce()

  @Test
  fun `userHasAllRegionsAccess returns false if the current request has 'X-Service-Name' header with value 'temporary-accommodation'`() {
    every { currentRequest.getHeader("X-Service-Name") } returns "temporary-accommodation"

    assertThat(userAccessService.userHasAllRegionsAccess(user)).isFalse
  }

  @Test
  fun `userHasAllRegionsAccess returns true if the current request has 'X-Service-Name' header with value 'approved-premises'`() {
    every { currentRequest.getHeader("X-Service-Name") } returns "approved-premises"

    assertThat(userAccessService.userHasAllRegionsAccess(user)).isTrue
  }

  @Test
  fun `userHasAllRegionsAccess returns true by default`() {
    every { currentRequest.getHeader("X-Service-Name") } returns "arbitrary-value"

    assertThat(userAccessService.userHasAllRegionsAccess(user)).isTrue
  }

  @Test
  fun `currentUserHasAllRegionsAccess returns false if the current request has 'X-Service-Name' header with value 'temporary-accommodation'`() {
    every { currentRequest.getHeader("X-Service-Name") } returns "temporary-accommodation"
    every { userService.getUserForRequest() } returns user

    assertThat(userAccessService.currentUserHasAllRegionsAccess()).isFalse
  }

  @Test
  fun `currentUserHasAllRegionsAccess returns true if the current request has 'X-Service-Name' header with value 'approved-premises'`() {
    every { currentRequest.getHeader("X-Service-Name") } returns "approved-premises"
    every { userService.getUserForRequest() } returns user

    assertThat(userAccessService.currentUserHasAllRegionsAccess()).isTrue
  }

  @Test
  fun `currentUserHasAllRegionsAccess returns true by default`() {
    every { currentRequest.getHeader("X-Service-Name") } returns "arbitrary-value"
    every { userService.getUserForRequest() } returns user

    assertThat(userAccessService.currentUserHasAllRegionsAccess()).isTrue
  }

  @Test
  fun `userCanAccessRegion returns false if the current user does not have all regions access and their probation region ID does not equal the specified ID`() {
    every { currentRequest.getHeader("X-Service-Name") } returns "temporary-accommodation"

    assertThat(userAccessService.userCanAccessRegion(user, UUID.randomUUID())).isFalse
  }

  @Test
  fun `userCanAccessRegion returns true if the current user has all regions access`() {
    every { currentRequest.getHeader("X-Service-Name") } returns "approved-premises"

    assertThat(userAccessService.userCanAccessRegion(user, UUID.randomUUID())).isTrue
  }

  @Test
  fun `userCanAccessRegion returns true if the current user's probation region ID is equal to the specified ID`() {
    every { currentRequest.getHeader("X-Service-Name") } returns "temporary-accommodation"

    assertThat(userAccessService.userCanAccessRegion(user, probationRegionId)).isTrue
  }

  @Test
  fun `currentUserCanAccessRegion returns false if the current user does not have all regions access and their probation region ID does not equal the specified ID`() {
    every { currentRequest.getHeader("X-Service-Name") } returns "temporary-accommodation"
    every { userService.getUserForRequest() } returns user

    assertThat(userAccessService.currentUserCanAccessRegion(UUID.randomUUID())).isFalse
  }

  @Test
  fun `currentUserCanAccessRegion returns true if the current user has all regions access`() {
    every { currentRequest.getHeader("X-Service-Name") } returns "approved-premises"
    every { userService.getUserForRequest() } returns user

    assertThat(userAccessService.currentUserCanAccessRegion(UUID.randomUUID())).isTrue
  }

  @Test
  fun `currentUserCanAccessRegion returns true if the current user's probation region ID is equal to the specified ID`() {
    every { currentRequest.getHeader("X-Service-Name") } returns "temporary-accommodation"
    every { userService.getUserForRequest() } returns user

    assertThat(userAccessService.currentUserCanAccessRegion(probationRegionId)).isTrue
  }
}
