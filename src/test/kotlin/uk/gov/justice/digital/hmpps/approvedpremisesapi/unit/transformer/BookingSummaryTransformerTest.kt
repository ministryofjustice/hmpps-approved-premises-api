package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BookingSummaryTransformer
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class BookingSummaryTransformerTest {
  private val bookingSummaryTransformer = BookingSummaryTransformer()

  @Test
  fun `transformJpaToApi transforms correctly`() {
    val approvedPremisesEntityFactory = ApprovedPremisesEntityFactory()
    val premises = approvedPremisesEntityFactory
      .withUnitTestControlTestProbationAreaAndLocalAuthority()
      .produce()

    val booking = BookingEntity(
      id = UUID.fromString("c0cffa2a-490a-4e8b-a970-80aea3922a18"),
      arrivalDate = LocalDate.parse("2022-08-10"),
      departureDate = LocalDate.parse("2022-08-30"),
      keyWorkerStaffCode = "789",
      crn = "CRN123",
      arrivals = mutableListOf(),
      departures = mutableListOf(),
      nonArrival = null,
      cancellations = mutableListOf(),
      confirmation = null,
      extensions = mutableListOf(),
      dateChanges = mutableListOf(),
      premises = premises,
      bed = null,
      service = ServiceName.approvedPremises.value,
      originalArrivalDate = LocalDate.parse("2022-08-10"),
      originalDepartureDate = LocalDate.parse("2022-08-30"),
      createdAt = OffsetDateTime.parse("2022-07-01T12:34:56.789Z"),
      application = null,
      offlineApplication = null,
      turnarounds = mutableListOf(),
      nomsNumber = "NOMS123",
      placementRequest = null,
      status = null,
    )

    assertThat(bookingSummaryTransformer.transformJpaToApi(booking)).isEqualTo(
      BookingSummary(
        id = booking.id,
        premisesId = premises.id,
        premisesName = premises.name,
        arrivalDate = booking.arrivalDate,
        departureDate = booking.departureDate,
        createdAt = booking.createdAt.toInstant(),
        type = BookingSummary.Type.legacy,
      ),
    )
  }
}
