package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedRepository

class BedDetailQueryTest : IntegrationTestBase() {
  @Autowired
  lateinit var realBedRepository: BedRepository

  @Test
  fun `summary works as expected`() {
    val probationRegion = probationRegionEntityFactory.produceAndPersist {
      withYieldedApArea {
        apAreaEntityFactory.produceAndPersist()
      }
    }

    val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

    val premises = approvedPremisesEntityFactory.produceAndPersist {
      withProbationRegion(probationRegion)
      withLocalAuthorityArea(localAuthorityArea)
    }

    val room = roomEntityFactory.produceAndPersist {
      withPremises(premises)
    }

    val bed = bedEntityFactory.produceAndPersist {
      withRoom(room)
    }

    val result = realBedRepository.getDetailById(bed.id)!!

    assertThat(result.id).isEqualTo(bed.id)
    assertThat(result.name).isEqualTo(bed.name)
    assertThat(result.roomId).isEqualTo(room.id)
    assertThat(result.roomName).isEqualTo(room.name)
    assertThat(result.bedBooked).isEqualTo(false)
    assertThat(result.bedOutOfService).isEqualTo(false)
  }
}
