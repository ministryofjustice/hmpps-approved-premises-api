package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ContextStaffMemberFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffUserDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Approved Premises Bed`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockSuccessfulStaffMembersCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.communityApiMockNotFoundStaffUserDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.communityApiMockSuccessfulOffenderDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.communityApiMockSuccessfulStaffUserDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.ApprovedPremisesBookingSeedCsvRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.CsvBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateAfter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class SeedBookingsTest : SeedTestBase() {
  @Test
  fun `Creating a Booking for a CRN which does not have a NOMS number in Community API logs an error`() {
    val crn = "CRN123"

    communityApiMockSuccessfulOffenderDetailsCall(
      OffenderDetailsSummaryFactory()
        .withCrn(crn)
        .withNomsNumber(null)
        .produce(),
    )

    withCsv(
      "crn-without-noms",
      approvedPremisesBookingSeedCsvRowsToCsv(
        listOf(
          ApprovedPremisesBookingSeedCsvRowFactory()
            .withCrn(crn)
            .produce(),
        ),
      ),
    )

    seedService.seedData(SeedFileType.approvedPremisesBookings, "crn-without-noms")

    assertThat(logEntries).anyMatch {
      it.level == "error" &&
        it.message.contains("Error on row 1") &&
        it.throwable != null &&
        it.throwable.message!!.contains("Offender does not have a NOMS number")
    }
  }

  @Test
  fun `Creating a Booking for a non-existent Bed logs an error`() {
    val crn = "CRN123"

    communityApiMockSuccessfulOffenderDetailsCall(
      OffenderDetailsSummaryFactory()
        .withCrn(crn)
        .produce(),
    )

    withCsv(
      "non-existent-bed",
      approvedPremisesBookingSeedCsvRowsToCsv(
        listOf(
          ApprovedPremisesBookingSeedCsvRowFactory()
            .withCrn(crn)
            .withBedCode("BED")
            .produce(),
        ),
      ),
    )

    seedService.seedData(SeedFileType.approvedPremisesBookings, "non-existent-bed")

    assertThat(logEntries).anyMatch {
      it.level == "error" &&
        it.message.contains("Error on row 1") &&
        it.throwable != null &&
        it.throwable.message!!.contains("Bed with code BED does not exist")
    }
  }

  @Test
  fun `Creating a Booking with a non-existent Key Worker logs an error`() {
    `Given an Approved Premises Bed` { bed ->
      val crn = "CRN123"
      val keyWorkerUsername = "USERNAME"

      communityApiMockSuccessfulOffenderDetailsCall(
        OffenderDetailsSummaryFactory()
          .withCrn(crn)
          .produce(),
      )

      communityApiMockNotFoundStaffUserDetailsCall(keyWorkerUsername)

      withCsv(
        "non-existent-key-worker",
        approvedPremisesBookingSeedCsvRowsToCsv(
          listOf(
            ApprovedPremisesBookingSeedCsvRowFactory()
              .withCrn(crn)
              .withBedCode(bed.code!!)
              .withKeyWorkerDeliusUsername(keyWorkerUsername)
              .produce(),
          ),
        ),
      )

      seedService.seedData(SeedFileType.approvedPremisesBookings, "non-existent-key-worker")

      assertThat(logEntries).anyMatch {
        it.level == "error" &&
          it.message.contains("Error on row 1") &&
          it.throwable != null &&
          it.throwable.message!!.contains("Unable to complete GET request to /secure/staff/username/USERNAME: 404 NOT_FOUND")
      }
    }
  }

  @Test
  fun `Creating a Booking with a non-existent Departure Reason logs an error`() {
    `Given an Approved Premises Bed` { bed ->
      val crn = "CRN123"

      communityApiMockSuccessfulOffenderDetailsCall(
        OffenderDetailsSummaryFactory()
          .withCrn(crn)
          .produce(),
      )

      withCsv(
        "non-existent-departure-reason",
        approvedPremisesBookingSeedCsvRowsToCsv(
          listOf(
            ApprovedPremisesBookingSeedCsvRowFactory()
              .withCrn(crn)
              .withBedCode(bed.code!!)
              .withDepartureReason("DEPARTUREREASON")
              .produce(),
          ),
        ),
      )

      seedService.seedData(SeedFileType.approvedPremisesBookings, "non-existent-departure-reason")

      assertThat(logEntries).anyMatch {
        it.level == "error" &&
          it.message.contains("Error on row 1") &&
          it.throwable != null &&
          it.throwable.message!!.contains("Could not find Departure Reason with name 'DEPARTUREREASON'")
      }
    }
  }

  @Test
  fun `Creating a Booking with a non-existent Departure Destination Provider logs an error`() {
    `Given an Approved Premises Bed` { bed ->
      val crn = "CRN123"

      communityApiMockSuccessfulOffenderDetailsCall(
        OffenderDetailsSummaryFactory()
          .withCrn(crn)
          .produce(),
      )

      withCsv(
        "non-existent-departure-destination-provider",
        approvedPremisesBookingSeedCsvRowsToCsv(
          listOf(
            ApprovedPremisesBookingSeedCsvRowFactory()
              .withCrn(crn)
              .withBedCode(bed.code!!)
              .withDepartureDestinationProvider("DESTINATIONPROVIDER")
              .produce(),
          ),
        ),
      )

      seedService.seedData(SeedFileType.approvedPremisesBookings, "non-existent-departure-destination-provider")

      assertThat(logEntries).anyMatch {
        it.level == "error" &&
          it.message.contains("Error on row 1") &&
          it.throwable != null &&
          it.throwable.message!!.contains("Could not find Destination Provider with name 'DESTINATIONPROVIDER'")
      }
    }
  }

  @Test
  fun `Creating a Booking with a non-existent Move on Category logs an error`() {
    `Given an Approved Premises Bed` { bed ->
      val crn = "CRN123"

      communityApiMockSuccessfulOffenderDetailsCall(
        OffenderDetailsSummaryFactory()
          .withCrn(crn)
          .produce(),
      )

      withCsv(
        "non-existent-departure-move-on-category",
        approvedPremisesBookingSeedCsvRowsToCsv(
          listOf(
            ApprovedPremisesBookingSeedCsvRowFactory()
              .withCrn(crn)
              .withBedCode(bed.code!!)
              .withDepartureMoveOnCategory("MOVEONCATEGORY")
              .produce(),
          ),
        ),
      )

      seedService.seedData(SeedFileType.approvedPremisesBookings, "non-existent-departure-move-on-category")

      assertThat(logEntries).anyMatch {
        it.level == "error" &&
          it.message.contains("Error on row 1") &&
          it.throwable != null &&
          it.throwable.message!!.contains("Could not find Move on Category with name 'MOVEONCATEGORY'")
      }
    }
  }

  @Test
  fun `Creating a Booking with a non-existent Destination Provider logs an error`() {
    `Given an Approved Premises Bed` { bed ->
      val crn = "CRN123"

      communityApiMockSuccessfulOffenderDetailsCall(
        OffenderDetailsSummaryFactory()
          .withCrn(crn)
          .produce(),
      )

      withCsv(
        "non-existent-departure-destination-provider",
        approvedPremisesBookingSeedCsvRowsToCsv(
          listOf(
            ApprovedPremisesBookingSeedCsvRowFactory()
              .withCrn(crn)
              .withBedCode(bed.code!!)
              .withDepartureDestinationProvider("DESTINATIONPROVIDER")
              .produce(),
          ),
        ),
      )

      seedService.seedData(SeedFileType.approvedPremisesBookings, "non-existent-departure-destination-provider")

      assertThat(logEntries).anyMatch {
        it.level == "error" &&
          it.message.contains("Error on row 1") &&
          it.throwable != null &&
          it.throwable.message!!.contains("Could not find Destination Provider with name 'DESTINATIONPROVIDER'")
      }
    }
  }

  @Test
  fun `Creating a Booking with a non-existent Non Arrival Reason logs an error`() {
    `Given an Approved Premises Bed` { bed ->
      val crn = "CRN123"

      communityApiMockSuccessfulOffenderDetailsCall(
        OffenderDetailsSummaryFactory()
          .withCrn(crn)
          .produce(),
      )

      withCsv(
        "non-existent-non-arrival-reason",
        approvedPremisesBookingSeedCsvRowsToCsv(
          listOf(
            ApprovedPremisesBookingSeedCsvRowFactory()
              .withCrn(crn)
              .withBedCode(bed.code!!)
              .withNonArrivalReason("NONARRIVALREASON")
              .produce(),
          ),
        ),
      )

      seedService.seedData(SeedFileType.approvedPremisesBookings, "non-existent-non-arrival-reason")

      assertThat(logEntries).anyMatch {
        it.level == "error" &&
          it.message.contains("Error on row 1") &&
          it.throwable != null &&
          it.throwable.message!!.contains("Could not find Non Arrival Reason with name 'NONARRIVALREASON'")
      }
    }
  }

  @Test
  fun `Creating a Booking with a non-existent Cancellation Reason logs an error`() {
    `Given an Approved Premises Bed` { bed ->
      val crn = "CRN123"

      communityApiMockSuccessfulOffenderDetailsCall(
        OffenderDetailsSummaryFactory()
          .withCrn(crn)
          .produce(),
      )

      withCsv(
        "non-existent-cancellation-reason",
        approvedPremisesBookingSeedCsvRowsToCsv(
          listOf(
            ApprovedPremisesBookingSeedCsvRowFactory()
              .withCrn(crn)
              .withBedCode(bed.code!!)
              .withCancellationReason("CANCELLATIONREASON")
              .produce(),
          ),
        ),
      )

      seedService.seedData(SeedFileType.approvedPremisesBookings, "non-existent-cancellation-reason")

      assertThat(logEntries).anyMatch {
        it.level == "error" &&
          it.message.contains("Error on row 1") &&
          it.throwable != null &&
          it.throwable.message!!.contains("Could not find Cancellation Reason with name 'CANCELLATIONREASON'")
      }
    }
  }

  @Test
  fun `Creating a future Booking (without Arrival, Departure, NonArrival or Cancellation) succeeds`() {
    `Given an Approved Premises Bed` { bed ->
      val bookingToCreateRow = ApprovedPremisesBookingSeedCsvRowFactory()
        .withBedCode(bed.code!!)
        .produce()

      val offlineApplication = offlineApplicationEntityFactory.produceAndPersist {
        withCrn(bookingToCreateRow.crn)
        withService(ServiceName.approvedPremises.value)
      }

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(bookingToCreateRow.crn)
        .produce()

      communityApiMockSuccessfulOffenderDetailsCall(
        offenderDetails,
      )

      withCsv(
        "future-booking",
        approvedPremisesBookingSeedCsvRowsToCsv(
          listOf(
            bookingToCreateRow,
          ),
        ),
      )

      seedService.seedData(SeedFileType.approvedPremisesBookings, "future-booking")

      val createdBooking = bookingRepository.findByIdOrNull(bookingToCreateRow.id)

      assertThat(createdBooking).isNotNull
      assertThat(createdBooking!!.crn).isEqualTo(bookingToCreateRow.crn)
      assertThat(createdBooking.nomsNumber).isEqualTo(offenderDetails.otherIds.nomsNumber)
      assertThat(createdBooking.bed!!.id).isEqualTo(bed.id)
      assertThat(createdBooking.offlineApplication!!.id).isEqualTo(offlineApplication.id)
      assertThat(createdBooking.service).isEqualTo(ServiceName.approvedPremises.value)
      assertThat(createdBooking.arrivalDate).isEqualTo(bookingToCreateRow.plannedArrivalDate)
      assertThat(createdBooking.departureDate).isEqualTo(bookingToCreateRow.plannedDepartureDate)
    }
  }

  @Test
  fun `Creating an ongoing Booking (with Arrival without Departure, NonArrival or Cancellation) succeeds`() {
    `Given an Approved Premises Bed` { bed ->
      val keyWorkerStaffUserDetails = StaffUserDetailsFactory()
        .produce()
      val staffMember = ContextStaffMemberFactory()
        .withStaffCode(keyWorkerStaffUserDetails.staffCode)
        .produce()

      communityApiMockSuccessfulStaffUserDetailsCall(keyWorkerStaffUserDetails)
      apDeliusContextMockSuccessfulStaffMembersCall(staffMember, (bed.room.premises as ApprovedPremisesEntity).qCode)

      val bookingToCreateRow = ApprovedPremisesBookingSeedCsvRowFactory()
        .withBedCode(bed.code!!)
        .withPlannedArrivalDate(LocalDate.parse("2023-06-06"))
        .withPlannedDepartureDate(LocalDate.parse("2023-06-30"))
        .withArrivalDate(LocalDate.parse("2023-06-07"))
        .withArrivalNotes("Notes about arrival")
        .withKeyWorkerDeliusUsername(keyWorkerStaffUserDetails.username)
        .produce()

      val offlineApplication = offlineApplicationEntityFactory.produceAndPersist {
        withCrn(bookingToCreateRow.crn)
        withService(ServiceName.approvedPremises.value)
      }

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(bookingToCreateRow.crn)
        .produce()

      communityApiMockSuccessfulOffenderDetailsCall(
        offenderDetails,
      )

      withCsv(
        "existing-booking",
        approvedPremisesBookingSeedCsvRowsToCsv(
          listOf(
            bookingToCreateRow,
          ),
        ),
      )

      seedService.seedData(SeedFileType.approvedPremisesBookings, "existing-booking")

      val createdBooking = bookingRepository.findByIdOrNull(bookingToCreateRow.id)

      assertThat(createdBooking).isNotNull
      assertThat(createdBooking!!.crn).isEqualTo(bookingToCreateRow.crn)
      assertThat(createdBooking.nomsNumber).isEqualTo(offenderDetails.otherIds.nomsNumber)
      assertThat(createdBooking.bed!!.id).isEqualTo(bed.id)
      assertThat(createdBooking.offlineApplication!!.id).isEqualTo(offlineApplication.id)
      assertThat(createdBooking.service).isEqualTo(ServiceName.approvedPremises.value)
      assertThat(createdBooking.arrivalDate).isEqualTo(bookingToCreateRow.arrivalDate)
      assertThat(createdBooking.departureDate).isEqualTo(bookingToCreateRow.plannedDepartureDate)
      assertThat(createdBooking.arrival).isNotNull
      assertThat(createdBooking.arrival!!.arrivalDate).isEqualTo(bookingToCreateRow.arrivalDate)
      assertThat(createdBooking.arrival!!.notes).isEqualTo(bookingToCreateRow.arrivalNotes)
    }
  }

  @Test
  fun `Creating a departed Booking (with Arrival, Departure without NonArrival or Cancellation) succeeds`() {
    `Given an Approved Premises Bed` { bed ->
      val keyWorkerStaffUserDetails = StaffUserDetailsFactory()
        .produce()
      val staffMember = ContextStaffMemberFactory()
        .withStaffCode(keyWorkerStaffUserDetails.staffCode)
        .produce()

      communityApiMockSuccessfulStaffUserDetailsCall(keyWorkerStaffUserDetails)
      apDeliusContextMockSuccessfulStaffMembersCall(staffMember, (bed.room.premises as ApprovedPremisesEntity).qCode)

      val departureReason = departureReasonEntityFactory.produceAndPersist {
        withServiceScope(ServiceName.approvedPremises.value)
      }
      val destinationProvider = destinationProviderEntityFactory.produceAndPersist()
      val moveOnCategory = moveOnCategoryEntityFactory.produceAndPersist {
        withServiceScope(ServiceName.approvedPremises.value)
      }

      val bookingToCreateRow = ApprovedPremisesBookingSeedCsvRowFactory()
        .withBedCode(bed.code!!)
        .withPlannedArrivalDate(LocalDate.parse("2023-06-06"))
        .withPlannedDepartureDate(LocalDate.parse("2023-06-30"))
        .withArrivalDate(LocalDate.parse("2023-06-07"))
        .withArrivalNotes("Notes about arrival")
        .withKeyWorkerDeliusUsername(keyWorkerStaffUserDetails.username)
        .withDepartureDateTime(OffsetDateTime.parse("2023-06-29T15:00:30+01:00"))
        .withDepartureReason(departureReason.name)
        .withDepartureDestinationProvider(destinationProvider.name)
        .withDepartureMoveOnCategory(moveOnCategory.name)
        .withDepartureNotes("Notes about departure")
        .produce()

      val offlineApplication = offlineApplicationEntityFactory.produceAndPersist {
        withCrn(bookingToCreateRow.crn)
        withService(ServiceName.approvedPremises.value)
      }

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(bookingToCreateRow.crn)
        .produce()

      communityApiMockSuccessfulOffenderDetailsCall(
        offenderDetails,
      )

      withCsv(
        "departed-booking",
        approvedPremisesBookingSeedCsvRowsToCsv(
          listOf(
            bookingToCreateRow,
          ),
        ),
      )

      seedService.seedData(SeedFileType.approvedPremisesBookings, "departed-booking")

      val createdBooking = bookingRepository.findByIdOrNull(bookingToCreateRow.id)

      assertThat(createdBooking).isNotNull
      assertThat(createdBooking!!.crn).isEqualTo(bookingToCreateRow.crn)
      assertThat(createdBooking.nomsNumber).isEqualTo(offenderDetails.otherIds.nomsNumber)
      assertThat(createdBooking.bed!!.id).isEqualTo(bed.id)
      assertThat(createdBooking.offlineApplication!!.id).isEqualTo(offlineApplication.id)
      assertThat(createdBooking.service).isEqualTo(ServiceName.approvedPremises.value)
      assertThat(createdBooking.arrivalDate).isEqualTo(bookingToCreateRow.arrivalDate)
      assertThat(createdBooking.departureDate).isEqualTo(bookingToCreateRow.departureDateTime!!.toLocalDate())
      assertThat(createdBooking.arrival).isNotNull
      assertThat(createdBooking.arrival!!.arrivalDate).isEqualTo(bookingToCreateRow.arrivalDate)
      assertThat(createdBooking.arrival!!.notes).isEqualTo(bookingToCreateRow.arrivalNotes)
      assertThat(createdBooking.departure).isNotNull
      assertThat(createdBooking.departure!!.dateTime).isEqualTo(bookingToCreateRow.departureDateTime)
      assertThat(createdBooking.departure!!.notes).isEqualTo(bookingToCreateRow.departureNotes)
      assertThat(createdBooking.departure!!.destinationProvider!!.id).isEqualTo(destinationProvider.id)
      assertThat(createdBooking.departure!!.moveOnCategory.id).isEqualTo(moveOnCategory.id)
      assertThat(createdBooking.departure!!.reason.id).isEqualTo(departureReason.id)
    }
  }

  @Test
  fun `Creating a Non Arrived Booking (with NonArrival without Arrival, Departure, Cancellation) succeeds`() {
    `Given an Approved Premises Bed` { bed ->
      val nonArrivalReason = nonArrivalReasonEntityFactory.produceAndPersist()

      val bookingToCreateRow = ApprovedPremisesBookingSeedCsvRowFactory()
        .withBedCode(bed.code!!)
        .withPlannedArrivalDate(LocalDate.parse("2023-06-06"))
        .withPlannedDepartureDate(LocalDate.parse("2023-06-30"))
        .withNonArrivalDate(LocalDate.parse("2023-06-30"))
        .withNonArrivalReason(nonArrivalReason.name)
        .withNonArrivalNotes("Notes about non arrival")
        .produce()

      val offlineApplication = offlineApplicationEntityFactory.produceAndPersist {
        withCrn(bookingToCreateRow.crn)
        withService(ServiceName.approvedPremises.value)
      }

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(bookingToCreateRow.crn)
        .produce()

      communityApiMockSuccessfulOffenderDetailsCall(
        offenderDetails,
      )

      withCsv(
        "non-arrived-booking",
        approvedPremisesBookingSeedCsvRowsToCsv(
          listOf(
            bookingToCreateRow,
          ),
        ),
      )

      seedService.seedData(SeedFileType.approvedPremisesBookings, "non-arrived-booking")

      val createdBooking = bookingRepository.findByIdOrNull(bookingToCreateRow.id)

      assertThat(createdBooking).isNotNull
      assertThat(createdBooking!!.crn).isEqualTo(bookingToCreateRow.crn)
      assertThat(createdBooking.nomsNumber).isEqualTo(offenderDetails.otherIds.nomsNumber)
      assertThat(createdBooking.bed!!.id).isEqualTo(bed.id)
      assertThat(createdBooking.offlineApplication!!.id).isEqualTo(offlineApplication.id)
      assertThat(createdBooking.service).isEqualTo(ServiceName.approvedPremises.value)
      assertThat(createdBooking.arrivalDate).isEqualTo(bookingToCreateRow.plannedArrivalDate)
      assertThat(createdBooking.departureDate).isEqualTo(bookingToCreateRow.plannedDepartureDate)
      assertThat(createdBooking.nonArrival).isNotNull
      assertThat(createdBooking.nonArrival!!.reason.id).isEqualTo(nonArrivalReason.id)
      assertThat(createdBooking.nonArrival!!.notes).isEqualTo(bookingToCreateRow.nonArrivalNotes)
    }
  }

  @Test
  fun `Creating a Cancelled Booking (with Cancellation without Arrival, Non Arrival, Departure) succeeds`() {
    `Given an Approved Premises Bed` { bed ->
      val cancellationReason = cancellationReasonEntityFactory.produceAndPersist {
        withServiceScope(ServiceName.approvedPremises.value)
      }

      val bookingToCreateRow = ApprovedPremisesBookingSeedCsvRowFactory()
        .withBedCode(bed.code!!)
        .withPlannedArrivalDate(LocalDate.parse("2023-06-06"))
        .withPlannedDepartureDate(LocalDate.parse("2023-06-30"))
        .withCancellationReason(cancellationReason.name)
        .withCancellationNotes("Notes about cancellation")
        .withCancellationDate(LocalDate.parse("2023-06-07"))
        .produce()

      val offlineApplication = offlineApplicationEntityFactory.produceAndPersist {
        withCrn(bookingToCreateRow.crn)
        withService(ServiceName.approvedPremises.value)
      }

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(bookingToCreateRow.crn)
        .produce()

      communityApiMockSuccessfulOffenderDetailsCall(
        offenderDetails,
      )

      withCsv(
        "cancelled-booking",
        approvedPremisesBookingSeedCsvRowsToCsv(
          listOf(
            bookingToCreateRow,
          ),
        ),
      )

      seedService.seedData(SeedFileType.approvedPremisesBookings, "cancelled-booking")

      val createdBooking = bookingRepository.findByIdOrNull(bookingToCreateRow.id)

      assertThat(createdBooking).isNotNull
      assertThat(createdBooking!!.crn).isEqualTo(bookingToCreateRow.crn)
      assertThat(createdBooking.nomsNumber).isEqualTo(offenderDetails.otherIds.nomsNumber)
      assertThat(createdBooking.bed!!.id).isEqualTo(bed.id)
      assertThat(createdBooking.offlineApplication!!.id).isEqualTo(offlineApplication.id)
      assertThat(createdBooking.service).isEqualTo(ServiceName.approvedPremises.value)
      assertThat(createdBooking.arrivalDate).isEqualTo(bookingToCreateRow.plannedArrivalDate)
      assertThat(createdBooking.departureDate).isEqualTo(bookingToCreateRow.plannedDepartureDate)
      assertThat(createdBooking.cancellation).isNotNull
      assertThat(createdBooking.cancellation!!.reason.id).isEqualTo(cancellationReason.id)
      assertThat(createdBooking.cancellation!!.date).isEqualTo(bookingToCreateRow.cancellationDate)
      assertThat(createdBooking.cancellation!!.notes).isEqualTo(bookingToCreateRow.cancellationNotes)
    }
  }

  @Test
  fun `Including a row for an existing Booking does nothing`() {
    `Given an Approved Premises Bed` { bed ->
      val existingBookingRow = ApprovedPremisesBookingSeedCsvRowFactory()
        .withBedCode(bed.code!!)
        .produce()

      offlineApplicationEntityFactory.produceAndPersist {
        withCrn(existingBookingRow.crn)
        withService(ServiceName.approvedPremises.value)
      }

      val existingBooking = bookingEntityFactory.produceAndPersist {
        withId(existingBookingRow.id)
        withBed(bed)
        withPremises(bed.room.premises)
      }

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(existingBookingRow.crn)
        .produce()

      communityApiMockSuccessfulOffenderDetailsCall(
        offenderDetails,
      )

      withCsv(
        "existing-booking",
        approvedPremisesBookingSeedCsvRowsToCsv(
          listOf(existingBookingRow),
        ),
      )

      seedService.seedData(SeedFileType.approvedPremisesBookings, "existing-booking")

      val bookingFromDatabase = bookingRepository.findByIdOrNull(existingBookingRow.id)

      assertThat(bookingFromDatabase!!.crn).isEqualTo(existingBooking.crn)
      assertThat(bookingFromDatabase.arrivalDate).isEqualTo(existingBooking.arrivalDate)
      assertThat(bookingFromDatabase.departureDate).isEqualTo(existingBooking.departureDate)
    }
  }

  private fun approvedPremisesBookingSeedCsvRowsToCsv(rows: List<ApprovedPremisesBookingSeedCsvRow>): String {
    val builder = CsvBuilder()
      .withUnquotedFields(
        "id",
        "crn",
        "plannedArrivalDate",
        "plannedDepartureDate",
        "keyWorkerDeliusUsername",
        "bedCode",
        "arrivalDate",
        "arrivalNotes",
        "departureDateTime",
        "departureReason",
        "departureDestinationProvider",
        "departureMoveOnCategory",
        "departureNotes",
        "nonArrivalDate",
        "nonArrivalReason",
        "nonArrivalNotes",
        "cancellationDate",
        "cancellationReason",
        "cancellationNotes",
      )
      .newRow()

    rows.forEach {
      builder
        .withQuotedField(it.id)
        .withQuotedField(it.crn)
        .withQuotedField(it.plannedArrivalDate)
        .withQuotedField(it.plannedDepartureDate)
        .withQuotedField(it.keyWorkerDeliusUsername ?: "")
        .withQuotedField(it.bedCode)
        .withQuotedField(it.arrivalDate ?: "")
        .withQuotedField(it.arrivalNotes ?: "")
        .withQuotedField(it.departureDateTime ?: "")
        .withQuotedField(it.departureReason ?: "")
        .withQuotedField(it.departureDestinationProvider ?: "")
        .withQuotedField(it.departureMoveOnCategory ?: "")
        .withQuotedField(it.departureNotes ?: "")
        .withQuotedField(it.nonArrivalDate ?: "")
        .withQuotedField(it.nonArrivalReason ?: "")
        .withQuotedField(it.nonArrivalNotes ?: "")
        .withQuotedField(it.cancellationDate ?: "")
        .withQuotedField(it.cancellationReason ?: "")
        .withQuotedField(it.cancellationNotes ?: "")
        .newRow()
    }

    return builder.build()
  }
}

class ApprovedPremisesBookingSeedCsvRowFactory : Factory<ApprovedPremisesBookingSeedCsvRow> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var crn: Yielded<String> = { randomStringUpperCase(6) }
  private var plannedArrivalDate: Yielded<LocalDate> = { LocalDate.now().randomDateAfter(20) }
  private var plannedDepartureDate: Yielded<LocalDate> = { LocalDate.now().plusDays(20).randomDateAfter(40) }
  private var keyWorkerDeliusUsername: Yielded<String?> = { null }
  private var bedCode: Yielded<String> = { randomStringUpperCase(6) }
  private var arrivalDate: Yielded<LocalDate?> = { null }
  private var arrivalNotes: Yielded<String?> = { null }
  private var departureDateTime: Yielded<OffsetDateTime?> = { null }
  private var departureReason: Yielded<String?> = { null }
  private var departureDestinationProvider: Yielded<String?> = { null }
  private var departureMoveOnCategory: Yielded<String?> = { null }
  private var departureNotes: Yielded<String?> = { null }
  private var nonArrivalDate: Yielded<LocalDate?> = { null }
  private var nonArrivalReason: Yielded<String?> = { null }
  private var nonArrivalNotes: Yielded<String?> = { null }
  private var cancellationDate: Yielded<LocalDate?> = { null }
  private var cancellationReason: Yielded<String?> = { null }
  private var cancellationNotes: Yielded<String?> = { null }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withCrn(crn: String) = apply {
    this.crn = { crn }
  }

  fun withPlannedArrivalDate(plannedArrivalDate: LocalDate) = apply {
    this.plannedArrivalDate = { plannedArrivalDate }
  }

  fun withPlannedDepartureDate(plannedDepartureDate: LocalDate) = apply {
    this.plannedDepartureDate = { plannedDepartureDate }
  }

  fun withKeyWorkerDeliusUsername(keyWorkerDeliusUsername: String?) = apply {
    this.keyWorkerDeliusUsername = { keyWorkerDeliusUsername }
  }

  fun withBedCode(bedCode: String) = apply {
    this.bedCode = { bedCode }
  }

  fun withArrivalDate(arrivalDate: LocalDate?) = apply {
    this.arrivalDate = { arrivalDate }
  }

  fun withArrivalNotes(arrivalNotes: String?) = apply {
    this.arrivalNotes = { arrivalNotes }
  }

  fun withDepartureDateTime(departureDateTime: OffsetDateTime?) = apply {
    this.departureDateTime = { departureDateTime }
  }

  fun withDepartureReason(departureReason: String?) = apply {
    this.departureReason = { departureReason }
  }

  fun withDepartureDestinationProvider(departureDestinationProvider: String?) = apply {
    this.departureDestinationProvider = { departureDestinationProvider }
  }

  fun withDepartureMoveOnCategory(departureMoveOnCategory: String?) = apply {
    this.departureMoveOnCategory = { departureMoveOnCategory }
  }

  fun withDepartureNotes(departureNotes: String?) = apply {
    this.departureNotes = { departureNotes }
  }

  fun withNonArrivalDate(nonArrivalDate: LocalDate?) = apply {
    this.nonArrivalDate = { nonArrivalDate }
  }

  fun withNonArrivalReason(nonArrivalReason: String?) = apply {
    this.nonArrivalReason = { nonArrivalReason }
  }

  fun withNonArrivalNotes(nonArrivalNotes: String?) = apply {
    this.nonArrivalNotes = { nonArrivalNotes }
  }

  fun withCancellationDate(cancellationDate: LocalDate?) = apply {
    this.cancellationDate = { cancellationDate }
  }

  fun withCancellationReason(cancellationReason: String?) = apply {
    this.cancellationReason = { cancellationReason }
  }

  fun withCancellationNotes(cancellationNotes: String?) = apply {
    this.cancellationNotes = { cancellationNotes }
  }

  override fun produce() = ApprovedPremisesBookingSeedCsvRow(
    id = this.id(),
    crn = this.crn(),
    plannedArrivalDate = this.plannedArrivalDate(),
    plannedDepartureDate = this.plannedDepartureDate(),
    keyWorkerDeliusUsername = this.keyWorkerDeliusUsername(),
    bedCode = this.bedCode(),
    arrivalDate = this.arrivalDate(),
    arrivalNotes = this.arrivalNotes(),
    departureDateTime = this.departureDateTime(),
    departureReason = this.departureReason(),
    departureDestinationProvider = this.departureDestinationProvider(),
    departureMoveOnCategory = this.departureMoveOnCategory(),
    departureNotes = this.departureNotes(),
    nonArrivalDate = this.nonArrivalDate(),
    nonArrivalReason = this.nonArrivalReason(),
    nonArrivalNotes = this.nonArrivalNotes(),
    cancellationDate = this.cancellationDate(),
    cancellationReason = this.cancellationReason(),
    cancellationNotes = this.cancellationNotes(),
  )
}
