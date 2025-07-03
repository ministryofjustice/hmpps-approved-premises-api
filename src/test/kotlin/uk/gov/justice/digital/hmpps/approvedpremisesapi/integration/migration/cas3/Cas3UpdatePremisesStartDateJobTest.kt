package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration.cas3

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
import java.time.LocalDate
import java.time.OffsetDateTime

class Cas3UpdatePremisesStartDateJobTest : MigrationJobTestBase() {

  @Test
  fun `Premises with null startDate are updated using createdAt date`() {
    givenAUser { user, jwt ->

      val premisesTemporaryAccommodation = temporaryAccommodationPremisesEntityFactory.produceAndPersistMultiple(5) {
        withProbationRegion(user.probationRegion)
        withLocalAuthorityArea(localAuthorityEntityFactory.produceAndPersist())
      }

      premisesTemporaryAccommodation.forEach {
        it.startDate = null
        it.createdAt = OffsetDateTime.now().randomDateTimeBefore(360)
        temporaryAccommodationPremisesRepository.save(it)
      }

      migrationJobService.runMigrationJob(MigrationJobType.updateCas3PremisesStartDate, 10)

      val savedTap = temporaryAccommodationPremisesRepository.findAll()
      assertThat(savedTap).hasSize(5)
      savedTap.forEach {
        assertThat(it.startDate).isEqualTo(it.createdAt!!.toLocalDate())
      }
    }
  }

  @Test
  fun `Premises with null startDate and null createdAt are updated using oldest booking arrival date`() {
    givenAUser { user, jwt ->

      val premisesTemporaryAccommodation = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withProbationRegion(user.probationRegion)
        withLocalAuthorityArea(localAuthorityEntityFactory.produceAndPersist())
      }

      premisesTemporaryAccommodation.createdAt = null
      temporaryAccommodationPremisesRepository.save(premisesTemporaryAccommodation)

      val oldestArrivalDate = LocalDate.now().randomDateBefore(60)
      val newerArrivalDate = LocalDate.now().randomDateAfter(59)

      bookingEntityFactory.produceAndPersist {
        withPremises(premisesTemporaryAccommodation)
        withServiceName(temporaryAccommodation)
        withBed(
          bedEntityFactory.produceAndPersist {
            withRoom(
              roomEntityFactory.produceAndPersist {
                withPremises(premisesTemporaryAccommodation)
              },
            )
          },
        )
        withArrivalDate(oldestArrivalDate)
        withDepartureDate(oldestArrivalDate.plusDays(7))
        withServiceName(temporaryAccommodation)
      }

      bookingEntityFactory.produceAndPersist {
        withPremises(premisesTemporaryAccommodation)
        withServiceName(temporaryAccommodation)
        withBed(
          bedEntityFactory.produceAndPersist {
            withRoom(
              roomEntityFactory.produceAndPersist {
                withPremises(premisesTemporaryAccommodation)
              },
            )
          },
        )
        withArrivalDate(newerArrivalDate)
        withDepartureDate(newerArrivalDate.plusDays(7))
        withServiceName(temporaryAccommodation)
      }

      migrationJobService.runMigrationJob(MigrationJobType.updateCas3PremisesStartDate, 10)

      val savedTap = temporaryAccommodationPremisesRepository.findAll()
      assertThat(savedTap).hasSize(1)
      savedTap.forEach {
        assertThat(it.startDate).isEqualTo(oldestArrivalDate)
      }
    }
  }

  @Test
  fun `Premises with null startDate and no bookings remain unchanged`() {
    givenAUser { user, jwt ->

      val premisesTemporaryAccommodation = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withProbationRegion(user.probationRegion)
        withLocalAuthorityArea(localAuthorityEntityFactory.produceAndPersist())
      }

      premisesTemporaryAccommodation.createdAt = null
      temporaryAccommodationPremisesRepository.save(premisesTemporaryAccommodation)

      val room = roomEntityFactory.produceAndPersist {
        withPremises(premisesTemporaryAccommodation)
        withName("Test Room")
      }

      val oldestBedStartDate = LocalDate.now().randomDateBefore(60)
      val newerBedStartDate = LocalDate.now().randomDateAfter(59)

      val bed1 = bedEntityFactory.produceAndPersist {
        withRoom(room)
        withName("Bed with no bookings")
        withStartDate(oldestBedStartDate)
      }

      val bed2 = bedEntityFactory.produceAndPersist {
        withRoom(room)
        withName("Bed with no bookings")
        withStartDate(newerBedStartDate)
      }

      setCreatedAt(bed1, null)
      setCreatedAt(bed2, null)

      migrationJobService.runMigrationJob(MigrationJobType.updateCas3PremisesStartDate, 10)

      assertThat(temporaryAccommodationPremisesRepository.findById(premisesTemporaryAccommodation.id).get().startDate).isEqualTo(oldestBedStartDate)
    }
  }

  private fun setCreatedAt(bed: BedEntity, createdAt: OffsetDateTime?) {
    bed.createdAt = createdAt
    bedRepository.save(bed)
  }
}
