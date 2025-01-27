package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.testimprovements

import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UnitTest {

  // for unit tests
  @Test
  fun `attempting new user entity`() {
    val one = UserTestExample(name = "namename")
    assertThat(one.name).isEqualTo("namename")

    val two = UserTestExample(id = UUID.randomUUID(), name = "new name")
    assertThat(two.name).isEqualTo("new name")
  }

  private val userFactoryExample = UserTestExample()

  @Test
  fun `attempting new user entity two`() {
    val one = userFactoryExample.copy(name = "name").toEntity()
    assertThat(one.name).isEqualTo("name")

    val two = userFactoryExample.copy(id = UUID.randomUUID(), name = "new name").toEntity()
    assertThat(two.name).isEqualTo("new name")
  }
}
