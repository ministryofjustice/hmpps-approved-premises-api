package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas3

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BookingSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BookingSearchResultDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3BookingSearchResultTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.TestCas3BookingSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.OffsetDateTime
import java.time.ZoneOffset

class Cas3BookingSearchTransformerTest {
  private val bookingSearchResultTransformer = Cas3BookingSearchResultTransformer()

  @Test
  fun `transformDomainToApi transforms correctly`() {
    val (domainResults, bookingSearchResultDtos) = buildBookingSearchData()
    val result = bookingSearchResultTransformer.transformDomainToApi(bookingSearchResultDtos)

    assertThat(result.resultsCount).isEqualTo(6)

    result.results.forEachIndexed { index: Int, transformedResult: Cas3BookingSearchResult ->
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
      assertThat(transformedResult.bedspace.id).isEqualTo(domainResult.getBedspaceId())
      assertThat(transformedResult.bedspace.reference).isEqualTo(domainResult.getBedspaceReference())
    }
  }

  private fun buildBookingSearchData(): Pair<List<TestCas3BookingSearchResult>, List<Cas3BookingSearchResultDto>> {
    val domainResults = listOf(
      TestCas3BookingSearchResult()
        .withPersonName(randomStringMultiCaseWithNumbers(6))
        .withBookingStatus(Cas3BookingStatus.provisional),
      TestCas3BookingSearchResult()
        .withPersonName(randomStringMultiCaseWithNumbers(6))
        .withBookingStatus(Cas3BookingStatus.confirmed),
      TestCas3BookingSearchResult()
        .withBookingStatus(Cas3BookingStatus.notMinusArrived),
      TestCas3BookingSearchResult()
        .withPersonName(randomStringMultiCaseWithNumbers(6))
        .withBookingStatus(Cas3BookingStatus.arrived),
      TestCas3BookingSearchResult()
        .withBookingStatus(Cas3BookingStatus.departed),
      TestCas3BookingSearchResult()
        .withPersonName(randomStringMultiCaseWithNumbers(6))
        .withBookingStatus(Cas3BookingStatus.cancelled),
    )
    val bookingSearchResultDtos = domainResults.map { rs ->
      Cas3BookingSearchResultDto(
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
        rs.getBedspaceId(),
        rs.getBedspaceReference(),
      )
    }
    return Pair(domainResults, bookingSearchResultDtos)
  }
}
