package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.testimprovements

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion

class IntegrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var userEntityFactoryComponent: UserEntityFactory

  @Test
  fun `produce and persist new entity`() {
    val user = UserTest(probationRegion = givenAProbationRegion(), deliusUsername = "testusername")
    val savedUserEntity = userRepository.saveAndFlush(user.toEntity())

    assertThat(userRepository.findByDeliusUsername(user.deliusUsername)?.deliusUsername)
      .isEqualTo(user.deliusUsername).isEqualTo(savedUserEntity.deliusUsername)

    val savedUserEntity2 = userEntityFactoryComponent.persist(UserTest(probationRegion = givenAProbationRegion()))

    assertThat(userRepository.findByDeliusUsername(savedUserEntity2.deliusUsername)?.deliusUsername).isEqualTo(
      savedUserEntity2.deliusUsername,
    )
  }
}
