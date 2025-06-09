package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration.cas3

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName.approvedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName.cas2
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName.cas2v2
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName.temporaryAccommodation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddListCaseSummaryToBulkResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration.MigrationJobTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.Name

class Cas3UpdateBookingOffenderNameJobTest : MigrationJobTestBase() {

  @Test
  fun `all bookings offender names are updated from Delius`() {
    givenAUser { user, jwt ->

      val probationRegion = givenAProbationRegion()

      val premises = givenAnApprovedPremises(
        region = probationRegion,
      )

      val bookings = bookingEntityFactory.produceAndPersistMultiple(15) {
        withPremises(premises)
        withServiceName(temporaryAccommodation)
      }

      val cases1 = bookings.take(10).map {
        CaseSummaryFactory()
          .withCrn(it.crn)
          .withName(Name(forename = "Forename ${it.crn}", surname = "Surname ${it.crn}"))
          .produce()
      }

      val cases2 = bookings.subList(10, 15).map {
        CaseSummaryFactory()
          .withCrn(it.crn)
          .withName(Name(forename = "Forename ${it.crn}", surname = "Surname ${it.crn}"))
          .produce()
      }

      apDeliusContextAddListCaseSummaryToBulkResponse(cases1)
      apDeliusContextAddListCaseSummaryToBulkResponse(cases2)

      migrationJobService.runMigrationJob(MigrationJobType.cas3BookingOffenderName, 10)

      val savedBookings = bookingRepository.findAll()
      assertThat(savedBookings).hasSize(15)
      savedBookings.forEach {
        assertThat(it.offenderName).isEqualTo("Forename ${it.crn} Surname ${it.crn}")
      }
    }
  }

  @Test
  fun `only cas3 bookings offender names are updated from Delius`() {
    givenAUser { user, jwt ->
      val probationRegion = givenAProbationRegion()

      val premises = givenAnApprovedPremises(
        region = probationRegion,
      )

      val bookings = bookingEntityFactory.produceAndPersistMultiple(2) {
        withPremises(premises)
        withServiceName(temporaryAccommodation)
      }
      val cases = bookings.map {
        CaseSummaryFactory()
          .withCrn(it.crn)
          .withName(Name(forename = "Forename ${it.crn}", surname = "Surname ${it.crn}"))
          .produce()
      }

      apDeliusContextAddListCaseSummaryToBulkResponse(cases)

      listOf(approvedPremises, cas2, cas2v2).forEach { serviceName ->
        bookingEntityFactory.produceAndPersistMultiple(2) {
          withPremises(premises)
          withServiceName(serviceName)
        }
      }

      migrationJobService.runMigrationJob(MigrationJobType.cas3BookingOffenderName, 10)

      val savedBookings = bookingRepository.findAll()
      assertThat(savedBookings).hasSize(8)
      savedBookings.forEach {
        if (temporaryAccommodation.value.equals(it.service)) {
          assertThat(it.offenderName).isEqualTo("Forename ${it.crn} Surname ${it.crn}")
        } else {
          assertThat(it.offenderName).isNull()
        }
      }
    }
  }

  @Test
  fun `when offender is not found in Delius throws an exception`() {
    givenAUser { user, jwt ->
      val probationRegion = givenAProbationRegion()

      val premises = givenAnApprovedPremises(
        region = probationRegion,
      )

      val booking = bookingEntityFactory.produceAndPersist {
        withPremises(premises)
        withServiceName(temporaryAccommodation)
      }

      migrationJobService.runMigrationJob(MigrationJobType.cas3BookingOffenderName, 10)

      Assertions.assertThat(logEntries)
        .withFailMessage("-> logEntries actually contains: $logEntries")
        .anyMatch {
          it.level == "error" &&
            it.message == "Unable to update bookings offenders name with crn ${booking.crn}" &&
            it.throwable != null &&
            it.throwable.message == "Unable to complete GET request to /probation-cases/summaries: 404 NOT_FOUND"
        }

      val savedBooking = bookingRepository.findAll()
      Assertions.assertThat(savedBooking).hasSize(1)
      Assertions.assertThat(savedBooking.get(0).id).isEqualTo(booking.id)
      Assertions.assertThat(savedBooking.get(0).offenderName).isNull()
    }
  }
}
