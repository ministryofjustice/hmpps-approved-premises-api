package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1BedRepository

class BedDetailQueryTest : IntegrationTestBase() {
  @Autowired
  lateinit var cas1BedRepository: Cas1BedRepository

  @Test
  fun `summary works as expected`() {
    val premises = givenAnApprovedPremises()

    val room = roomEntityFactory.produceAndPersist {
      withPremises(premises)
    }

    val bed = cas1BedEntityFactory.produceAndPersist {
      withRoom(room)
    }

    val result = cas1BedRepository.getDetailById(bed.id)!!

    assertThat(result.id).isEqualTo(bed.id)
    assertThat(result.name).isEqualTo(bed.name)
    assertThat(result.roomId).isEqualTo(room.id)
    assertThat(result.roomName).isEqualTo(room.name)
    assertThat(result.bedBooked).isEqualTo(false)
    assertThat(result.bedOutOfService).isEqualTo(false)
  }
}
