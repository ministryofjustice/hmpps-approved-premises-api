package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.migration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration.MigrationJobTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate

class Cas3UpdateBedspaceStartDateJobTest : MigrationJobTestBase() {
  @Test
  fun `All bedspaces with a start date after booking arrival date will have their start date updated to match the arrival date of the first booking`() {
    val probationRegion = givenAProbationRegion()
    val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion {
        probationRegion
      }
    }

    val bedspaces = bedEntityFactory.produceAndPersistMultiple(5) {
      withName(randomStringMultiCaseWithNumbers(10))
      withStartDate(LocalDate.now().randomDateBefore(180))
      withYieldedRoom {
        roomEntityFactory.produceAndPersist {
          withName(randomStringMultiCaseWithNumbers(7))
          withYieldedPremises { premises }
        }
      }
    }

    val bedspaceOne = bedspaces[0]
    val bedspaceTwo = bedspaces[1]
    val bedspaceThree = bedspaces[2]
    val bedspaceFour = bedspaces[3]
    val bedspaceFive = bedspaces[4]

    val bookingBedspaceOne = createBooking(bedspaceOne, premises, bedspaceOne.startDate?.minusDays(10)!!)
    val bookingBedspaceTwo = createBooking(bedspaceTwo, premises, bedspaceTwo.startDate?.minusDays(1)!!)
    createBooking(bedspaceThree, premises, bedspaceThree.startDate!!)
    createBooking(bedspaceFour, premises, bedspaceFour.startDate?.plusDays(1)!!)

    migrationJobService.runMigrationJob(MigrationJobType.updateCas3BedspaceStartDate, 2)

    val updatedBedspaceOne = bedRepository.findById(bedspaceOne.id).get()
    assertThat(updatedBedspaceOne.startDate).isEqualTo(bookingBedspaceOne.arrivalDate)

    val updatedBedspaceTwo = bedRepository.findById(bedspaceTwo.id).get()
    assertThat(updatedBedspaceTwo.startDate).isEqualTo(bookingBedspaceTwo.arrivalDate)

    val updatedBedspaceThree = bedRepository.findById(bedspaceThree.id).get()
    assertThat(updatedBedspaceThree.startDate).isEqualTo(bedspaceThree.startDate)

    val updatedBedspaceFour = bedRepository.findById(bedspaceFour.id).get()
    assertThat(updatedBedspaceFour.startDate).isEqualTo(bedspaceFour.startDate)

    val updatedBedspaceFive = bedRepository.findById(bedspaceFive.id).get()
    assertThat(updatedBedspaceFive.startDate).isEqualTo(bedspaceFive.startDate)
  }

  private fun createBooking(bedspace: BedEntity, premises: TemporaryAccommodationPremisesEntity, arrivalDate: LocalDate) = bookingEntityFactory.produceAndPersist {
    withBed(bedspace)
    withPremises(premises)
    withArrivalDate(arrivalDate)
    withDepartureDate(arrivalDate.plusDays(90))
    withServiceName(ServiceName.temporaryAccommodation)
    withCrn(randomStringMultiCaseWithNumbers(10))
  }
}
