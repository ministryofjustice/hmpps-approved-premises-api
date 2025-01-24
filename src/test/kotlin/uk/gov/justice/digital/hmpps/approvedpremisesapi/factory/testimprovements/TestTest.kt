package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.testimprovements

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import java.util.UUID

class TestTest : IntegrationTestBase() {

  // for unit tests

  @Test
  fun `attempting new user entity`() {
    val one = UserEntityFactoryProduce().produce()
    assertThat(one.name).isEqualTo("TODO()")
    val two = UserEntityFactoryProduce().produceWithValues(id = UUID.randomUUID(), name = "new name")
    assertThat(two.name).isEqualTo("new name")
  }

  // for integration tests

  @Autowired
  lateinit var userEntityFactoryProduceAndPersist: UserEntityFactoryProduceAndPersist

  @Test
  fun `produce and persist new entity`() {
    val one = userEntityFactoryProduceAndPersist.produce()
    one.probationRegion = givenAProbationRegion()
    println(one.deliusUsername)
    var saved1 = userEntityFactoryProduceAndPersist.persist(one)
    assertThat(userRepository.findByDeliusUsername(one.deliusUsername)?.deliusUsername).isEqualTo(one.deliusUsername)

    var probationRegion = givenAProbationRegion()
    var two = userEntityFactoryProduceAndPersist.produceAndPersist(probationRegion)

    assertThat(userRepository.findByDeliusUsername(two.deliusUsername)?.deliusUsername).isEqualTo(two.deliusUsername)
  }
}
