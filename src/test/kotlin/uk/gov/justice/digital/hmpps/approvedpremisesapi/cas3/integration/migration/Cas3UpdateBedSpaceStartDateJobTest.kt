package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.migration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName.temporaryAccommodation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration.MigrationJobTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateAfter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.Period

class Cas3UpdateBedSpaceStartDateJobTest : MigrationJobTestBase() {

  @Test
  fun `beds with null startDate are updated using createdAt date`() {
    givenAUser { user, jwt ->

      val premisesTemporaryAccommodation = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withProbationRegion(user.probationRegion)
        withLocalAuthorityArea(localAuthorityEntityFactory.produceAndPersist())
      }

      val room = roomEntityFactory.produceAndPersist {
        withPremises(premisesTemporaryAccommodation)
        withName("Test Room")
      }

      val beds = (1..5).map {
        val bed = bedEntityFactory.produceAndPersist {
          withRoom(room)
          withName("Bed $it")
          withStartDate(null)
        }
        setCreatedAt(bed, OffsetDateTime.now().randomDateTimeBefore(360))
      }

      migrationJobService.runMigrationJob(MigrationJobType.updateCas3BedspaceStartDate, 10)

      val savedBeds = bedRepository.findAll()
      assertThat(savedBeds).hasSize(5)
      savedBeds.forEach {
        assertThat(it.startDate).isEqualTo(it.createdAt!!.toLocalDate())
      }
    }
  }

  fun `non cas3 beds remain unchanged`() {
    givenAUser { user, jwt ->

      val premisesTemporaryAccommodation = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withProbationRegion(user.probationRegion)
        withLocalAuthorityArea(localAuthorityEntityFactory.produceAndPersist())
      }

      val room = roomEntityFactory.produceAndPersist {
        withPremises(premisesTemporaryAccommodation)
        withName("Test Room")
      }

      val createdAt = OffsetDateTime.now().randomDateTimeBefore(360)
      val beds = (1..5).map {
        bedEntityFactory.produceAndPersist {
          withRoom(room)
          withName("Bed $it")
          withStartDate(null)
        }
      }

      migrationJobService.runMigrationJob(MigrationJobType.updateCas3BedspaceStartDate, 10)

      val savedBeds = bedRepository.findAll()
      assertThat(savedBeds).hasSize(5)
      savedBeds.forEach {
        assertThat(it.startDate).isEqualTo(createdAt.toLocalDate())
      }
    }
  }

  @Test
  fun `beds with null startDate and null createdAt are updated using oldest booking arrival date`() {
    givenAUser { user, jwt ->

      val premisesTemporaryAccommodation = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withProbationRegion(user.probationRegion)
        withLocalAuthorityArea(localAuthorityEntityFactory.produceAndPersist())
      }

      val room = roomEntityFactory.produceAndPersist {
        withPremises(premisesTemporaryAccommodation)
        withName("Test Room")
      }

      val beds = (1..3).map {
        bedEntityFactory.produceAndPersist {
          withRoom(room)
          withName("Bed $it")
          withStartDate(null)
        }
      }

      beds.map {
        setCreatedAt(it, null)
      }

      val oldestArrivalDate = LocalDate.now().randomDateBefore(60)
      val newerArrivalDate = LocalDate.now().randomDateAfter(59)

      beds.forEachIndexed { index, bed ->

        bookingEntityFactory.produceAndPersist {
          withPremises(premisesTemporaryAccommodation)
          withServiceName(temporaryAccommodation)
          withBed(bed)
          withArrivalDate(oldestArrivalDate)
          withDepartureDate(oldestArrivalDate.plusDays(7))
          withServiceName(temporaryAccommodation)
        }

        bookingEntityFactory.produceAndPersist {
          withPremises(premisesTemporaryAccommodation)
          withServiceName(temporaryAccommodation)
          withBed(bed)
          withArrivalDate(newerArrivalDate)
          withDepartureDate(newerArrivalDate.plusDays(7))
          withServiceName(temporaryAccommodation)
        }
      }

      migrationJobService.runMigrationJob(MigrationJobType.updateCas3BedspaceStartDate, 10)

      val savedBeds = bedRepository.findAll()
      assertThat(savedBeds).hasSize(3)
      savedBeds.forEach {
        assertThat(it.startDate).isEqualTo(oldestArrivalDate)
      }
    }
  }

  @Test
  fun `beds with null startDate and no bookings is set to endDate`() {
    givenAUser { user, jwt ->

      val premisesTemporaryAccommodation = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withProbationRegion(user.probationRegion)
        withLocalAuthorityArea(localAuthorityEntityFactory.produceAndPersist())
      }

      val room = roomEntityFactory.produceAndPersist {
        withPremises(premisesTemporaryAccommodation)
        withName("Test Room")
      }

      val endDte = LocalDate.now().minus(Period.ofDays(randomInt(5, 30)))

      val bed = bedEntityFactory.produceAndPersist {
        withRoom(room)
        withName("Bed with no bookings")
        withStartDate(null)
        withEndDate(endDte)
      }

      setCreatedAt(bed, null)

      migrationJobService.runMigrationJob(MigrationJobType.updateCas3BedspaceStartDate, 10)

      val savedBed = bedRepository.findById(bed.id).get()
      assertThat(savedBed.startDate).isEqualTo(endDte)
    }
  }

  private fun setCreatedAt(bed: BedEntity, createdAt: OffsetDateTime?) {
    bed.createdAt = createdAt
    bedRepository.save(bed)
  }
}
