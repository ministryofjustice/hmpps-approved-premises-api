package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository

class CharacteristicQueryTest : IntegrationTestBase() {
  @Autowired
  lateinit var realCharacteristicRepository: CharacteristicRepository

  @Test
  fun `findAllForRoomId returns all the characteristics for a roomId`() {
    val probationRegion = givenAProbationRegion()

    var localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

    var premises = approvedPremisesEntityFactory.produceAndPersist {
      withProbationRegion(probationRegion)
      withLocalAuthorityArea(localAuthorityArea)
    }

    var roomCharacteristics = mutableListOf(
      characteristicEntityFactory.produceAndPersist(),
      characteristicEntityFactory.produceAndPersist(),
      characteristicEntityFactory.produceAndPersist(),
    )

    var otherCharacteristics = mutableListOf(
      characteristicEntityFactory.produceAndPersist(),
      characteristicEntityFactory.produceAndPersist(),
    )

    var room = roomEntityFactory.produceAndPersist {
      withPremises(premises)
      withCharacteristics(roomCharacteristics)
    }

    assertThat(realCharacteristicRepository.findAllForRoomId(room.id)).isEqualTo(roomCharacteristics)
  }
}
