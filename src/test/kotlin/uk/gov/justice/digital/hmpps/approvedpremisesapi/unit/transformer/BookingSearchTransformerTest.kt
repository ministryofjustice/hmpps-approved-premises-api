package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingSearchResultFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BookingSearchResultTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers

class BookingSearchTransformerTest {
  private val bookingSearchResultTransformer = BookingSearchResultTransformer()

  @Test
  fun `transformDomainToApi transforms correctly`() {
    val domainResults = listOf(
      BookingSearchResultFactory()
        .withPersonName(randomStringMultiCaseWithNumbers(6))
        .withBookingStatus(BookingStatus.provisional)
        .produce(),
      BookingSearchResultFactory()
        .withBookingStatus(BookingStatus.awaitingMinusArrival)
        .produce(),
      BookingSearchResultFactory()
        .withPersonName(randomStringMultiCaseWithNumbers(6))
        .withBookingStatus(BookingStatus.confirmed)
        .produce(),
      BookingSearchResultFactory()
        .withBookingStatus(BookingStatus.notMinusArrived)
        .produce(),
      BookingSearchResultFactory()
        .withPersonName(randomStringMultiCaseWithNumbers(6))
        .withBookingStatus(BookingStatus.arrived)
        .produce(),
      BookingSearchResultFactory()
        .withBookingStatus(BookingStatus.departed)
        .produce(),
      BookingSearchResultFactory()
        .withPersonName(randomStringMultiCaseWithNumbers(6))
        .withBookingStatus(BookingStatus.cancelled)
        .produce(),
    )

    val result = bookingSearchResultTransformer.transformDomainToApi(domainResults)

    assertThat(result.resultsCount).isEqualTo(7)

    result.results.forEachIndexed { index, it ->
      val domainResult = domainResults[index]

      assertThat(it.person.name).isEqualTo(domainResult.personName)
      assertThat(it.person.crn).isEqualTo(domainResult.personCrn)
      assertThat(it.booking.id).isEqualTo(domainResult.bookingId)
      assertThat(it.booking.status.value).isEqualTo(domainResult.bookingStatus)
      assertThat(it.booking.startDate).isEqualTo(domainResult.bookingStartDate)
      assertThat(it.booking.endDate).isEqualTo(domainResult.bookingEndDate)
      assertThat(it.booking.createdAt).isEqualTo(domainResult.bookingCreatedAt.toInstant())
      assertThat(it.premises.id).isEqualTo(domainResult.premisesId)
      assertThat(it.premises.name).isEqualTo(domainResult.premisesName)
      assertThat(it.premises.addressLine1).isEqualTo(domainResult.premisesAddressLine1)
      assertThat(it.premises.addressLine2).isEqualTo(domainResult.premisesAddressLine2)
      assertThat(it.premises.town).isEqualTo(domainResult.premisesTown)
      assertThat(it.premises.postcode).isEqualTo(domainResult.premisesPostcode)
      assertThat(it.room.id).isEqualTo(domainResult.roomId)
      assertThat(it.room.name).isEqualTo(domainResult.roomName)
      assertThat(it.bed.id).isEqualTo(domainResult.bedId)
      assertThat(it.bed.name).isEqualTo(domainResult.bedName)
    }
  }
}
