package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.CommunityAPI_mockSuccessfulOffenderDetailsCall

class PopulateNomsNumbersOnBookingsMigrationTest : MigrationJobTestBase() {
  @Test
  fun `All Bookings without Noms Number have Noms Number populated from Community API with a 500ms artificial delay`() {
    val premises = approvedPremisesEntityFactory.produceAndPersist {
      withProbationRegion(
        probationRegionEntityFactory.produceAndPersist {
          withApArea(apAreaEntityFactory.produceAndPersist())
        }
      )
      withLocalAuthorityArea(localAuthorityEntityFactory.produceAndPersist())
    }

    val room = roomEntityFactory.produceAndPersist {
      withPremises(premises)
    }

    val bed = bedEntityFactory.produceAndPersist {
      withRoom(room)
    }

    val bookingOne = bookingEntityFactory.produceAndPersist {
      withPremises(premises)
      withBed(bed)
      withCrn("CRNBOOKING1")
      withNomsNumber(null)
    }

    val bookingTwo = bookingEntityFactory.produceAndPersist {
      withPremises(premises)
      withBed(bed)
      withCrn("CRNBOOKING2")
      withNomsNumber(null)
    }

    CommunityAPI_mockSuccessfulOffenderDetailsCall(
      OffenderDetailsSummaryFactory()
        .withCrn(bookingOne.crn)
        .withNomsNumber("NOMSBOOKING1")
        .produce()
    )

    CommunityAPI_mockSuccessfulOffenderDetailsCall(
      OffenderDetailsSummaryFactory()
        .withCrn(bookingTwo.crn)
        .withNomsNumber("NOMSBOOKING2")
        .produce()
    )

    val startTime = System.currentTimeMillis()
    migrationJobService.runMigrationJob(MigrationJobType.populateNomsNumbersBookings)
    val endTime = System.currentTimeMillis()

    assertThat(endTime - startTime).isGreaterThan(500 * 2)

    val bookingOneAfterUpdate = bookingRepository.findByIdOrNull(bookingOne.id)!!
    val bookingTwoAfterUpdate = bookingRepository.findByIdOrNull(bookingTwo.id)!!

    assertThat(bookingOneAfterUpdate.nomsNumber).isEqualTo("NOMSBOOKING1")
    assertThat(bookingTwoAfterUpdate.nomsNumber).isEqualTo("NOMSBOOKING2")
  }
}
