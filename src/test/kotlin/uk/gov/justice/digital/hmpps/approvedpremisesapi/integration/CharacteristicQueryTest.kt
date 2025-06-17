package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository

class CharacteristicQueryTest : IntegrationTestBase() {
  @Autowired
  lateinit var realCharacteristicRepository: CharacteristicRepository

  @Test
  fun `findAllForRoomId returns all the characteristics for a roomId`() {
    val premises = givenAnApprovedPremises()

    val char1 = characteristicEntityFactory.produceAndPersist()
    val char2 = characteristicEntityFactory.produceAndPersist()
    val char3 = characteristicEntityFactory.produceAndPersist()

    val roomCharacteristics = mutableListOf(char1, char2, char3)

    var otherCharacteristics = mutableListOf(
      characteristicEntityFactory.produceAndPersist(),
      characteristicEntityFactory.produceAndPersist(),
    )

    val room = roomEntityFactory.produceAndPersist {
      withPremises(premises)
      withCharacteristics(roomCharacteristics)
    }

    assertThat(realCharacteristicRepository.findAllForRoomId(room.id)).containsExactlyInAnyOrder(
      char1,
      char2,
      char3,
    )
  }
}
