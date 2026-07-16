package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.migration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.givens.givenACas3Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspacesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration.MigrationJobTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate

class Cas3UpdateBedspaceStartDateJobTest : MigrationJobTestBase() {

  @Test
  fun `All bedspaces with a start date after booking arrival date will have their start date updated to match the arrival date of the first booking`() {
    val probationRegion = givenAProbationRegion()
    val premises = givenACas3Premises(probationRegion = probationRegion)

    val bedspaces = cas3BedspaceEntityFactory.produceAndPersistMultiple(5) {
      withPremises(premises)
      withStartDate(LocalDate.now().randomDateBefore(180))
    }

    val bedspaceOne = bedspaces[0]
    val bedspaceTwo = bedspaces[1]
    val bedspaceThree = bedspaces[2]
    val bedspaceFour = bedspaces[3]
    val bedspaceFive = bedspaces[4]

    val bookingBedspaceOne = createBooking(bedspaceOne, premises, bedspaceOne.startDate.minusDays(10))
    val bookingBedspaceTwo = createBooking(bedspaceTwo, premises, bedspaceTwo.startDate.minusDays(1))
    createBooking(bedspaceThree, premises, bedspaceThree.startDate)
    createBooking(bedspaceFour, premises, bedspaceFour.startDate.plusDays(1))

    migrationJobService.runMigrationJob(MigrationJobType.updateCas3BedspaceStartDate, 2)

    val updatedBedspaceOne = cas3BedspacesRepository.findById(bedspaceOne.id).get()
    assertThat(updatedBedspaceOne.startDate).isEqualTo(bookingBedspaceOne.arrivalDate)

    val updatedBedspaceTwo = cas3BedspacesRepository.findById(bedspaceTwo.id).get()
    assertThat(updatedBedspaceTwo.startDate).isEqualTo(bookingBedspaceTwo.arrivalDate)

    val updatedBedspaceThree = cas3BedspacesRepository.findById(bedspaceThree.id).get()
    assertThat(updatedBedspaceThree.startDate).isEqualTo(bedspaceThree.startDate)

    val updatedBedspaceFour = cas3BedspacesRepository.findById(bedspaceFour.id).get()
    assertThat(updatedBedspaceFour.startDate).isEqualTo(bedspaceFour.startDate)

    val updatedBedspaceFive = cas3BedspacesRepository.findById(bedspaceFive.id).get()
    assertThat(updatedBedspaceFive.startDate).isEqualTo(bedspaceFive.startDate)
  }

  private fun createBooking(bedspace: Cas3BedspacesEntity, premises: Cas3PremisesEntity, arrivalDate: LocalDate) = cas3BookingEntityFactory.produceAndPersist {
    withBedspace(bedspace)
    withPremises(premises)
    withArrivalDate(arrivalDate)
    withDepartureDate(arrivalDate.plusDays(90))
    withCrn(randomStringMultiCaseWithNumbers(10))
  }
}
