package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.BookingSearchResultDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BookingSearchResultTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.TestBookingSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.OffsetDateTime
import java.time.ZoneOffset

class BookingSearchTransformerTest {
  private val bookingSearchResultTransformer = BookingSearchResultTransformer()

  @Test
  fun `transformDomainToApi transforms correctly`() {
    val (domainResults, bookingSearchResultDtos) = buildBookingSearchData()
    val result = bookingSearchResultTransformer.transformDomainToApi(bookingSearchResultDtos)

    assertThat(result.resultsCount).isEqualTo(7)

    result.results.forEachIndexed { index: Int, transformedResult: BookingSearchResult ->
      val domainResult = domainResults[index]

      assertThat(transformedResult.person.name).isEqualTo(domainResult.getPersonName())
      assertThat(transformedResult.person.crn).isEqualTo(domainResult.getPersonCrn())
      assertThat(transformedResult.booking.id).isEqualTo(domainResult.getBookingId())
      assertThat(transformedResult.booking.status.value).isEqualTo(domainResult.getBookingStatus())
      assertThat(transformedResult.booking.startDate).isEqualTo(domainResult.getBookingStartDate())
      assertThat(transformedResult.booking.endDate).isEqualTo(domainResult.getBookingEndDate())
      assertThat(transformedResult.booking.createdAt).isEqualTo(domainResult.getBookingCreatedAt())
      assertThat(transformedResult.premises.id).isEqualTo(domainResult.getPremisesId())
      assertThat(transformedResult.premises.name).isEqualTo(domainResult.getPremisesName())
      assertThat(transformedResult.premises.addressLine1).isEqualTo(domainResult.getPremisesAddressLine1())
      assertThat(transformedResult.premises.addressLine2).isEqualTo(domainResult.getPremisesAddressLine2())
      assertThat(transformedResult.premises.town).isEqualTo(domainResult.getPremisesTown())
      assertThat(transformedResult.premises.postcode).isEqualTo(domainResult.getPremisesPostcode())
      assertThat(transformedResult.room.id).isEqualTo(domainResult.getRoomId())
      assertThat(transformedResult.room.name).isEqualTo(domainResult.getRoomName())
      assertThat(transformedResult.bed.id).isEqualTo(domainResult.getBedId())
      assertThat(transformedResult.bed.name).isEqualTo(domainResult.getBedName())
    }
  }

  private fun buildBookingSearchData(): Pair<List<TestBookingSearchResult>, List<BookingSearchResultDto>> {
    val domainResults = listOf(
      TestBookingSearchResult()
        .withPersonName(randomStringMultiCaseWithNumbers(6))
        .withBookingStatus(BookingStatus.PROVISIONAL),
      TestBookingSearchResult()
        .withBookingStatus(BookingStatus.awaitingMinusArrival),
      TestBookingSearchResult()
        .withPersonName(randomStringMultiCaseWithNumbers(6))
        .withBookingStatus(BookingStatus.CONFIRMED),
      TestBookingSearchResult()
        .withBookingStatus(BookingStatus.notMinusArrived),
      TestBookingSearchResult()
        .withPersonName(randomStringMultiCaseWithNumbers(6))
        .withBookingStatus(BookingStatus.ARRIVED),
      TestBookingSearchResult()
        .withBookingStatus(BookingStatus.departed),
      TestBookingSearchResult()
        .withPersonName(randomStringMultiCaseWithNumbers(6))
        .withBookingStatus(BookingStatus.cancelled),
    )
    val bookingSearchResultDtos = domainResults.mapNotNull { rs ->
      BookingSearchResultDto(
        rs.getPersonName(),
        rs.getPersonCrn(),
        rs.getBookingId(),
        rs.getBookingStatus(),
        rs.getBookingStartDate(),
        rs.getBookingEndDate(),
        OffsetDateTime.ofInstant(rs.getBookingCreatedAt(), ZoneOffset.UTC),
        rs.getPremisesId(),
        rs.getPremisesName(),
        rs.getPremisesAddressLine1(),
        rs.getPremisesAddressLine2(),
        rs.getPremisesTown(),
        rs.getPremisesPostcode(),
        rs.getRoomId(),
        rs.getRoomName(),
        rs.getBedId(),
        rs.getBedName(),
      )
    }
    return Pair(domainResults, bookingSearchResultDtos)
  }
}
