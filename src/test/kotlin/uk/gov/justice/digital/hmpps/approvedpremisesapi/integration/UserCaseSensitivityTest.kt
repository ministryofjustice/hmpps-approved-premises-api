package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService

class UserCaseSensitivityTest : IntegrationTestBase() {
  @Autowired
  lateinit var userService: UserService

  @Test
  fun `Fetching a user with lowercase username returns the user with normalised uppercase username`() {
    givenAUser { userEntity, _ ->
      val returnedUser = userService.getExistingUserOrCreate(userEntity.deliusUsername.lowercase())

      assertThat(returnedUser.id).isEqualTo(userEntity.id)
    }
  }
}
