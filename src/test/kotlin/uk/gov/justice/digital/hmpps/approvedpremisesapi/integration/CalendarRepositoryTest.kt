package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.CalendarBedInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.CalendarBookingInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.CalendarLostBedInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.CalendarRepository
import java.time.LocalDate
import java.util.UUID

class CalendarRepositoryTest : IntegrationTestBase() {
  @Autowired
  lateinit var calendarRepository: CalendarRepository

  @Test
  fun `Results are correct for a Premises without Rooms or Beds`() {
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

    val result = calendarRepository.getCalendarInfo(premises.id, LocalDate.of(2023, 6, 9), LocalDate.of(2023, 7, 9))
    assertThat(result).isEmpty()
  }

  @Test
  fun `Results are correct for a Premises with a Room & Bed but no Bookings or Lost Beds`() {
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

    val result = calendarRepository.getCalendarInfo(premises.id, LocalDate.of(2023, 6, 9), LocalDate.of(2023, 7, 9))

    val expectedBedKey = CalendarBedInfo(
      bedId = bed.id,
      bedName = bed.name,
    )

    assertThat(result).containsKey(expectedBedKey)

    assertThat(result[expectedBedKey]).isEmpty()
  }

  @Test
  fun `Results are correct for a Premises with a Room & Bed and a cancelled lost bed and booking`() {
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
  fun `Results are correct for a Premises with non-double-booked Bookings & Lost Bed`() {
    `Given an Offender` { offenderDetailsOne, _ ->
      `Given an Offender` { offenderDetailsTwo, _ ->
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
  fun `Results are correct for a Premises with double-booked Bookings & Lost Bed`() {
    `Given an Offender` { offenderDetailsOne, _ ->
      `Given an Offender` { offenderDetailsTwo, _ ->
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
