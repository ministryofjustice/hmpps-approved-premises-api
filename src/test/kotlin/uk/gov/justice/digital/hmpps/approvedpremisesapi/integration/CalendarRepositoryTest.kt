package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.CalendarBedInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.CalendarBookingInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.CalendarLostBedInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.CalendarRepository
import java.time.LocalDate
import java.util.UUID

class CalendarRepositoryTest : IntegrationTestBase() {
  @Autowired
  lateinit var calendarRepository: CalendarRepository

  lateinit var premises: PremisesEntity

  @BeforeEach
  fun createPremises() {
    premises = approvedPremisesEntityFactory.produceAndPersist {
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
  }

  @Test
  fun `Results are correct for a Premises without Rooms or Beds`() {
    val result = calendarRepository.getCalendarInfo(premises.id, LocalDate.of(2023, 6, 9), LocalDate.of(2023, 7, 9))
    assertThat(result).isEmpty()
  }

  @Test
  fun `Results are correct for a Premises with a Room & Bed but no Bookings or Lost Beds`() {
    val bed = bedEntityFactory.produceAndPersist {
      withName("test-bed")
      withYieldedRoom {
        roomEntityFactory.produceAndPersist {
          withName("test-room")
          withYieldedPremises { premises }
        }
      }
    }

    val result = calendarRepository.getCalendarInfo(premises.id, LocalDate.of(2023, 6, 9), LocalDate.of(2023, 7, 9))

    val expectedBedKey = CalendarBedInfo(
      bedId = bed.id,
      bedName = bed.name,
    )

    assertThat(result).containsKey(expectedBedKey)

    assertThat(result[expectedBedKey]).isEmpty()
  }

  @Test
  fun `Deactivated beds are not shown`() {
    val deactivatedBed = bedEntityFactory.produceAndPersist {
      withName("deactivated-bed")
      withYieldedRoom {
        roomEntityFactory.produceAndPersist {
          withName("deactivated-room")
          withYieldedPremises { premises }
        }
      }
      withEndDate { LocalDate.of(2023, 4, 9) }
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

    val deactivatedBedKey = CalendarBedInfo(
      bedId = deactivatedBed.id,
      bedName = deactivatedBed.name,
    )

    val expectedBedKey = CalendarBedInfo(
      bedId = bed.id,
      bedName = bed.name,
    )

    val resultWithStartDateBeforeDeactivation = calendarRepository.getCalendarInfo(premises.id, LocalDate.of(2023, 3, 9), LocalDate.of(2023, 4, 9))

    assertThat(resultWithStartDateBeforeDeactivation).containsKey(deactivatedBedKey)
    assertThat(resultWithStartDateBeforeDeactivation).containsKey(expectedBedKey)

    val resultWithStartDateAfterDeactivation = calendarRepository.getCalendarInfo(premises.id, LocalDate.of(2023, 6, 9), LocalDate.of(2023, 7, 9))

    assertThat(resultWithStartDateAfterDeactivation).doesNotContainKey(deactivatedBedKey)
    assertThat(resultWithStartDateAfterDeactivation).containsKey(expectedBedKey)
  }

  @Test
  fun `Results are correct for a Premises with a Room & Bed and a cancelled lost bed and booking`() {
    val bed = bedEntityFactory.produceAndPersist {
      withName("test-bed")
      withYieldedRoom {
        roomEntityFactory.produceAndPersist {
          withName("test-room")
          withYieldedPremises { premises }
        }
      }
    }

    val lostBed = lostBedsEntityFactory.produceAndPersist {
      withPremises(premises)
      withBed(bed)
      withStartDate(LocalDate.of(2023, 6, 4))
      withEndDate(LocalDate.of(2023, 6, 14))
      withYieldedReason { lostBedReasonEntityFactory.produceAndPersist() }
    }

    val booking = bookingEntityFactory.produceAndPersist {
      withPremises(premises)
      withBed(bed)
      withServiceName(ServiceName.approvedPremises)
      withArrivalDate(LocalDate.of(2023, 6, 15))
      withDepartureDate(LocalDate.of(2023, 6, 20))
    }

    lostBedCancellationEntityFactory.produceAndPersist {
      withLostBed(lostBed)
    }

    cancellationEntityFactory.produceAndPersist {
      withBooking(booking)
      withYieldedReason {
        cancellationReasonEntityFactory.produceAndPersist()
      }
    }

    val result = calendarRepository.getCalendarInfo(premises.id, LocalDate.of(2023, 6, 9), LocalDate.of(2023, 7, 9))

    val expectedBedKey = CalendarBedInfo(
      bedId = bed.id,
      bedName = bed.name,
    )

    assertThat(result).containsKey(expectedBedKey)

    assertThat(result[expectedBedKey]).isEmpty()
  }

  @Test
  fun `Results are correct for a Premises with a Room & Bed and a lost bed and non-arrived booking`() {
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
      withBed(bed)
      withServiceName(ServiceName.approvedPremises)
      withArrivalDate(LocalDate.of(2023, 6, 15))
      withDepartureDate(LocalDate.of(2023, 6, 20))
    }

    nonArrivalEntityFactory.produceAndPersist {
      withBooking(booking)
      withYieldedReason {
        nonArrivalReasonEntityFactory.produceAndPersist()
      }
    }

    val result = calendarRepository.getCalendarInfo(premises.id, LocalDate.of(2023, 6, 9), LocalDate.of(2023, 7, 9))

    val expectedBedKey = CalendarBedInfo(
      bedId = bed.id,
      bedName = bed.name,
    )

    assertThat(result).containsKey(expectedBedKey)

    assertThat(result[expectedBedKey]).isEmpty()
  }

  @Test
  fun `Results are correct for a Premises with non-double-booked Bookings & Lost Bed`() {
    givenAnOffender { offenderDetailsOne, _ ->
      givenAnOffender { offenderDetailsTwo, _ ->
        val bed = bedEntityFactory.produceAndPersist {
          withName("test-bed")
          withYieldedRoom {
            roomEntityFactory.produceAndPersist {
              withName("test-room")
              withYieldedPremises { premises }
            }
          }
        }

        val lostBed = lostBedsEntityFactory.produceAndPersist {
          withPremises(premises)
          withBed(bed)
          withStartDate(LocalDate.of(2023, 6, 4))
          withEndDate(LocalDate.of(2023, 6, 14))
          withYieldedReason { lostBedReasonEntityFactory.produceAndPersist() }
        }

        val bookingOne = bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withBed(bed)
          withServiceName(ServiceName.approvedPremises)
          withCrn(offenderDetailsOne.otherIds.crn)
          withArrivalDate(LocalDate.of(2023, 6, 15))
          withDepartureDate(LocalDate.of(2023, 6, 20))
        }

        val bookingTwo = bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withBed(bed)
          withServiceName(ServiceName.approvedPremises)
          withCrn(offenderDetailsTwo.otherIds.crn)
          withArrivalDate(LocalDate.of(2023, 6, 21))
          withDepartureDate(LocalDate.of(2023, 6, 30))
        }

        val result = calendarRepository.getCalendarInfo(premises.id, LocalDate.of(2023, 6, 9), LocalDate.of(2023, 7, 9))

        val expectedBedKey = CalendarBedInfo(
          bedId = bed.id,
          bedName = bed.name,
        )

        assertThat(result).containsKey(expectedBedKey)

        assertThat(result[expectedBedKey]).containsExactlyInAnyOrder(
          CalendarLostBedInfo(
            startDate = lostBed.startDate,
            endDate = lostBed.endDate,
            lostBedId = lostBed.id,
          ),
          CalendarBookingInfo(
            startDate = bookingOne.arrivalDate,
            endDate = bookingOne.departureDate,
            bookingId = bookingOne.id,
            crn = offenderDetailsOne.otherIds.crn,
            personName = null,
          ),
          CalendarBookingInfo(
            startDate = bookingTwo.arrivalDate,
            endDate = bookingTwo.departureDate,
            bookingId = bookingTwo.id,
            crn = offenderDetailsTwo.otherIds.crn,
            personName = null,
          ),
        )
      }
    }
  }

  @Test
  fun `Results are correct for a Premises with a Booking and Lost Bed that finishes on the start date`() {
    givenAnOffender { offenderDetailsOne, _ ->
      val bed = bedEntityFactory.produceAndPersist {
        withName("test-bed")
        withYieldedRoom {
          roomEntityFactory.produceAndPersist {
            withName("test-room")
            withYieldedPremises { premises }
          }
        }
      }

      val lostBed = lostBedsEntityFactory.produceAndPersist {
        withPremises(premises)
        withBed(bed)
        withStartDate(LocalDate.of(2023, 6, 9))
        withEndDate(LocalDate.of(2023, 6, 10))
        withYieldedReason { lostBedReasonEntityFactory.produceAndPersist() }
      }

      val booking = bookingEntityFactory.produceAndPersist {
        withPremises(premises)
        withBed(bed)
        withServiceName(ServiceName.approvedPremises)
        withCrn(offenderDetailsOne.otherIds.crn)
        withArrivalDate(LocalDate.of(2023, 6, 9))
        withDepartureDate(LocalDate.of(2023, 6, 10))
      }

      val result = calendarRepository.getCalendarInfo(premises.id, LocalDate.of(2023, 6, 10), LocalDate.of(2023, 7, 10))

      val expectedBedKey = CalendarBedInfo(
        bedId = bed.id,
        bedName = bed.name,
      )

      assertThat(result).containsKey(expectedBedKey)

      assertThat(result[expectedBedKey]).containsExactlyInAnyOrder(
        CalendarLostBedInfo(
          startDate = lostBed.startDate,
          endDate = lostBed.endDate,
          lostBedId = lostBed.id,
        ),
        CalendarBookingInfo(
          startDate = booking.arrivalDate,
          endDate = booking.departureDate,
          bookingId = booking.id,
          crn = offenderDetailsOne.otherIds.crn,
          personName = null,
        ),
      )
    }
  }

  @Test
  fun `Results are correct for a Premises with double-booked Bookings & Lost Bed`() {
    givenAnOffender { offenderDetailsOne, _ ->
      givenAnOffender { offenderDetailsTwo, _ ->
        val bed = bedEntityFactory.produceAndPersist {
          withName("test-bed")
          withYieldedRoom {
            roomEntityFactory.produceAndPersist {
              withName("test-room")
              withYieldedPremises { premises }
            }
          }
        }

        val lostBed = lostBedsEntityFactory.produceAndPersist {
          withPremises(premises)
          withBed(bed)
          withStartDate(LocalDate.of(2023, 6, 4))
          withEndDate(LocalDate.of(2023, 6, 14))
          withYieldedReason { lostBedReasonEntityFactory.produceAndPersist() }
        }

        val bookingOne = bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withBed(bed)
          withServiceName(ServiceName.approvedPremises)
          withCrn(offenderDetailsOne.otherIds.crn)
          withArrivalDate(LocalDate.of(2023, 6, 15))
          withDepartureDate(LocalDate.of(2023, 6, 26))
        }

        val bookingTwo = bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withBed(bed)
          withServiceName(ServiceName.approvedPremises)
          withCrn(offenderDetailsTwo.otherIds.crn)
          withArrivalDate(LocalDate.of(2023, 6, 21))
          withDepartureDate(LocalDate.of(2023, 6, 30))
        }

        val result = calendarRepository.getCalendarInfo(premises.id, LocalDate.of(2023, 6, 9), LocalDate.of(2023, 7, 9))

        val expectedBedKey = CalendarBedInfo(
          bedId = bed.id,
          bedName = bed.name,
        )

        assertThat(result).containsKey(expectedBedKey)

        assertThat(result[expectedBedKey]).containsExactlyInAnyOrder(
          CalendarLostBedInfo(
            startDate = lostBed.startDate,
            endDate = lostBed.endDate,
            lostBedId = lostBed.id,
          ),
          CalendarBookingInfo(
            startDate = bookingOne.arrivalDate,
            endDate = bookingOne.departureDate,
            bookingId = bookingOne.id,
            crn = offenderDetailsOne.otherIds.crn,
            personName = null,
          ),
          CalendarBookingInfo(
            startDate = bookingTwo.arrivalDate,
            endDate = bookingTwo.departureDate,
            bookingId = bookingTwo.id,
            crn = offenderDetailsTwo.otherIds.crn,
            personName = null,
          ),
        )
      }
    }
  }
}
