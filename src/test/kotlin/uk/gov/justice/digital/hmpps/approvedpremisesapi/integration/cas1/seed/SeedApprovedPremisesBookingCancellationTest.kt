package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.seed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.SeedTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.CsvBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.CancelBookingSeedCsvRow
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class SeedApprovedPremisesBookingCancellationTest : SeedTestBase() {
  @Test
  fun `Attempting to cancel a Booking that does not exist logs an error`() {
    withCsv(
      "booking-does-not-exist",
      approvedPremisesBookingCancelSeedCsvRowsToCsv(
        listOf(
          CancelBookingSeedCsvRow(UUID.fromString("ec35b02b-b981-4836-86b3-88101bc8d937")),
        ),
      ),
    )

    seedService.seedData(SeedFileType.approvedPremisesCancelBookings, "booking-does-not-exist")

    assertThat(logEntries)
      .withFailMessage("-> logEntries actually contains: $logEntries")
      .anyMatch {
        it.level == "error" &&
          it.message == "Error on row 1:" &&
          it.throwable != null &&
          it.throwable.message == "No Booking with Id of ec35b02b-b981-4836-86b3-88101bc8d937 exists"
      }
  }

  @Test
  fun `Attempting to cancel a non-Approved Premises Booking logs an error`() {
    `Given an Offender` { offenderDetails, _ ->
      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist {
            withId(UUID.randomUUID())
            withYieldedApArea {
              apAreaEntityFactory.produceAndPersist()
            }
          }
        }
      }

      val bed = bedEntityFactory.produceAndPersist {
        withName("test-bed")
        withYieldedRoom {
          roomEntityFactory.produceAndPersist {
            withName("test-room")
            withYieldedPremises { premises }
          }
        }
      }

      val booking = bookingEntityFactory.produceAndPersist {
        withPremises(premises)
        withCrn(offenderDetails.otherIds.crn)
        withServiceName(ServiceName.temporaryAccommodation)
        withYieldedBed { bed }
      }

      withCsv(
        "booking-not-for-ap",
        approvedPremisesBookingCancelSeedCsvRowsToCsv(
          listOf(
            CancelBookingSeedCsvRow(booking.id),
          ),
        ),
      )

      seedService.seedData(SeedFileType.approvedPremisesCancelBookings, "booking-not-for-ap")

      assertThat(logEntries)
        .withFailMessage("-> logEntries actually contains: $logEntries")
        .anyMatch {
          it.level == "error" &&
            it.message == "Error on row 1:" &&
            it.throwable != null &&
            it.throwable.message == "Booking ${booking.id} is not an Approved Premises Booking"
        }
    }
  }

  @Test
  fun `Cancelling an Approved Premises Booking succeeds`() {
    `Given an Offender` { offenderDetails, _ ->
      val premises = approvedPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist {
            withId(UUID.randomUUID())
            withYieldedApArea {
              apAreaEntityFactory.produceAndPersist()
            }
          }
        }
      }

      val bed = bedEntityFactory.produceAndPersist {
        withName("test-bed")
        withYieldedRoom {
          roomEntityFactory.produceAndPersist {
            withName("test-room")
            withYieldedPremises { premises }
          }
        }
      }

      val booking = bookingEntityFactory.produceAndPersist {
        withPremises(premises)
        withCrn(offenderDetails.otherIds.crn)
        withServiceName(ServiceName.approvedPremises)
        withYieldedBed { bed }
      }

      withCsv(
        "booking-for-ap",
        approvedPremisesBookingCancelSeedCsvRowsToCsv(
          listOf(
            CancelBookingSeedCsvRow(booking.id),
          ),
        ),
      )

      seedService.seedData(SeedFileType.approvedPremisesCancelBookings, "booking-for-ap")

      val savedBooking = bookingRepository.findByIdOrNull(booking.id)

      assertThat(savedBooking!!.cancellation).isNotNull
      assertThat(savedBooking.cancellation!!.reason.name).isEqualTo("Error in Booking Details")
    }
  }

  private fun approvedPremisesBookingCancelSeedCsvRowsToCsv(rows: List<CancelBookingSeedCsvRow>): String {
    val builder = CsvBuilder()
      .withUnquotedFields(
        "id",
      )
      .newRow()

    rows.forEach {
      builder
        .withQuotedField(it.id)
        .newRow()
    }

    return builder.build()
  }
}
