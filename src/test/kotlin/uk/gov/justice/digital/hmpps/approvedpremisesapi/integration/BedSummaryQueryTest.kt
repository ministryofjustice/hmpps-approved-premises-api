package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
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
    val probationRegion = givenAProbationRegion()

    val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

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

    val bedWithCancelledBooking = bedEntityFactory.produceAndPersist {
      withRoom(
        roomEntityFactory.produceAndPersist {
          withPremises(premises)
        },
      )
    }

    val bedWithNonArrivedBooking = bedEntityFactory.produceAndPersist {
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

    val bedWithCancelledLostBed = bedEntityFactory.produceAndPersist {
      withRoom(
        roomEntityFactory.produceAndPersist {
          withPremises(premises)
        },
      )
    }

    bedEntityFactory.produceAndPersist {
      withRoom(
        roomEntityFactory.produceAndPersist {
          withPremises(premises)
        },
      )
      withEndDate { LocalDate.now().minusDays(7) }
    }

    bookingEntityFactory.produceAndPersist {
      withPremises(premises)
      withBed(bedWithBooking)
      withArrivalDate(LocalDate.now().minusDays((7).toLong()))
      withDepartureDate(LocalDate.now().plusDays((20).toLong()))
    }

    cas3LostBedsEntityFactory.produceAndPersist {
      withPremises(premises)
      withBed(bedWithLostBed)
      withYieldedReason { cas3LostBedReasonEntityFactory.produceAndPersist() }
      withStartDate(LocalDate.now().minusDays((7).toLong()))
      withEndDate(LocalDate.now().plusDays((20).toLong()))
    }

    val cancelledBooking = bookingEntityFactory.produceAndPersist {
      withPremises(premises)
      withBed(bedWithCancelledBooking)
      withArrivalDate(LocalDate.now().minusDays((7).toLong()))
      withDepartureDate(LocalDate.now().plusDays((20).toLong()))
    }

    cancellationEntityFactory.produceAndPersist {
      withBooking(cancelledBooking)
      withYieldedReason { cancellationReasonEntityFactory.produceAndPersist() }
    }

    val nonArrivedBooking = bookingEntityFactory.produceAndPersist {
      withPremises(premises)
      withBed(bedWithNonArrivedBooking)
      withArrivalDate(LocalDate.now().minusDays((7).toLong()))
      withDepartureDate(LocalDate.now().plusDays((20).toLong()))
    }

    nonArrivalEntityFactory.produceAndPersist {
      withBooking(nonArrivedBooking)
      withYieldedReason {
        nonArrivalReasonEntityFactory.produceAndPersist()
      }
    }

    val cancelledLostBed = cas3LostBedsEntityFactory.produceAndPersist {
      withPremises(premises)
      withBed(bedWithCancelledLostBed)
      withYieldedReason { cas3LostBedReasonEntityFactory.produceAndPersist() }
      withStartDate(LocalDate.now().minusDays((7).toLong()))
      withEndDate(LocalDate.now().plusDays((20).toLong()))
    }

    cas3LostBedCancellationEntityFactory.produceAndPersist {
      withLostBed(cancelledLostBed)
    }

    val results: List<DomainBedSummary> =
      realBedRepository.findAllBedsForPremises(premises.id)

    assertThat(results.size).isEqualTo(6)

    results.first { it.id == bedWithoutBooking.id }.let {
      assertThat(it.name).isEqualTo(bedWithoutBooking.name)
      assertThat(it.roomId).isEqualTo(bedWithoutBooking.room.id)
      assertThat(it.roomName).isEqualTo(bedWithoutBooking.room.name)
      assertThat(it.bedBooked).isEqualTo(false)
      assertThat(it.bedOutOfService).isEqualTo(false)
    }

    results.first { it.id == bedWithBooking.id }.let {
      assertThat(it.name).isEqualTo(bedWithBooking.name)
      assertThat(it.roomId).isEqualTo(bedWithBooking.room.id)
      assertThat(it.roomName).isEqualTo(bedWithBooking.room.name)
      assertThat(it.bedBooked).isEqualTo(true)
      assertThat(it.bedOutOfService).isEqualTo(false)
    }

    results.first { it.id == bedWithLostBed.id }.let {
      assertThat(it.name).isEqualTo(bedWithLostBed.name)
      assertThat(it.roomId).isEqualTo(bedWithLostBed.room.id)
      assertThat(it.roomName).isEqualTo(bedWithLostBed.room.name)
      assertThat(it.bedBooked).isEqualTo(false)
      assertThat(it.bedOutOfService).isEqualTo(true)
    }

    results.first { it.id == bedWithCancelledLostBed.id }.let {
      assertThat(it.name).isEqualTo(bedWithCancelledLostBed.name)
      assertThat(it.roomId).isEqualTo(bedWithCancelledLostBed.room.id)
      assertThat(it.roomName).isEqualTo(bedWithCancelledLostBed.room.name)
      assertThat(it.bedBooked).isEqualTo(false)
      assertThat(it.bedOutOfService).isEqualTo(false)
    }

    results.first { it.id == bedWithCancelledBooking.id }.let {
      assertThat(it.name).isEqualTo(bedWithCancelledBooking.name)
      assertThat(it.roomId).isEqualTo(bedWithCancelledBooking.room.id)
      assertThat(it.roomName).isEqualTo(bedWithCancelledBooking.room.name)
      assertThat(it.bedBooked).isEqualTo(false)
      assertThat(it.bedOutOfService).isEqualTo(false)
    }

    results.first { it.id == bedWithNonArrivedBooking.id }.let {
      assertThat(it.name).isEqualTo(bedWithNonArrivedBooking.name)
      assertThat(it.roomId).isEqualTo(bedWithNonArrivedBooking.room.id)
      assertThat(it.roomName).isEqualTo(bedWithNonArrivedBooking.room.name)
      assertThat(it.bedBooked).isEqualTo(false)
      assertThat(it.bedOutOfService).isEqualTo(false)
    }
  }
}
