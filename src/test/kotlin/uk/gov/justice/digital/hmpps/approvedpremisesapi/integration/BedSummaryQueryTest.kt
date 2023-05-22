package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainBedSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import java.time.LocalDate

class BedSummaryQueryTest : IntegrationTestBase() {
  @Autowired
  lateinit var realBedRepository: BedRepository

  lateinit var bed: BedEntity
  lateinit var premises: PremisesEntity

  @BeforeEach
  fun setup() {
    var probationRegion = probationRegionEntityFactory.produceAndPersist {
      withYieldedApArea {
        apAreaEntityFactory.produceAndPersist()
      }
    }

    var localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

    this.premises = approvedPremisesEntityFactory.produceAndPersist {
      withProbationRegion(probationRegion)
      withLocalAuthorityArea(localAuthorityArea)
    }
  }

  @Test
  fun `summary works shows basic bed details and status`() {
    val bedWithoutBooking = bedEntityFactory.produceAndPersist {
      withRoom(
        roomEntityFactory.produceAndPersist {
          withPremises(premises)
        },
      )
    }

    val bedWithBooking = bedEntityFactory.produceAndPersist {
      withRoom(
        roomEntityFactory.produceAndPersist {
          withPremises(premises)
        },
      )
    }

    val bedWithLostBed = bedEntityFactory.produceAndPersist {
      withRoom(
        roomEntityFactory.produceAndPersist {
          withPremises(premises)
        },
      )
    }

    bookingEntityFactory.produceAndPersist {
      withPremises(premises)
      withBed(bedWithBooking)
      withArrivalDate(LocalDate.now().minusDays((7).toLong()))
      withDepartureDate(LocalDate.now().plusDays((20).toLong()))
    }

    lostBedsEntityFactory.produceAndPersist {
      withPremises(premises)
      withBed(bedWithLostBed)
      withYieldedReason { lostBedReasonEntityFactory.produceAndPersist() }
      withStartDate(LocalDate.now().minusDays((7).toLong()))
      withEndDate(LocalDate.now().plusDays((20).toLong()))
    }

    val results: List<DomainBedSummary> =
      realBedRepository.findAllBedsForPremises(premises.id)

    assertThat(results.size).isEqualTo(3)

    results.first { it.id == bedWithoutBooking.id }.let {
      assertThat(it.name).isEqualTo(bedWithoutBooking.name)
      assertThat(it.roomName).isEqualTo(bedWithoutBooking.room.name)
      assertThat(it.bedBooked).isEqualTo(false)
      assertThat(it.bedOutOfService).isEqualTo(false)
    }

    results.first { it.id == bedWithBooking.id }.let {
      assertThat(it.name).isEqualTo(bedWithBooking.name)
      assertThat(it.roomName).isEqualTo(bedWithBooking.room.name)
      assertThat(it.bedBooked).isEqualTo(true)
      assertThat(it.bedOutOfService).isEqualTo(false)
    }

    results.first { it.id == bedWithLostBed.id }.let {
      assertThat(it.name).isEqualTo(bedWithLostBed.name)
      assertThat(it.roomName).isEqualTo(bedWithLostBed.room.name)
      assertThat(it.bedBooked).isEqualTo(false)
      assertThat(it.bedOutOfService).isEqualTo(true)
    }
  }
}
