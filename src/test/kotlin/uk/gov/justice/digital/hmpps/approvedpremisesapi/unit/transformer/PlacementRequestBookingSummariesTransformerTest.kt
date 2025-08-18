package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestBookingSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequirementsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PlacementRequestBookingSummariesTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PlacementRequestBookingSummaryTransformer
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class PlacementRequestBookingSummariesTransformerTest {

  private val bookingSummaryTransformer = mockk<PlacementRequestBookingSummaryTransformer>()

  private val placementRequestBookingSummaryTransformer = PlacementRequestBookingSummariesTransformer(
    bookingSummaryTransformer,
  )

  val application = ApprovedPremisesApplicationEntityFactory()
    .withCreatedByUser(
      UserEntityFactory()
        .withYieldedProbationRegion {
          ProbationRegionEntityFactory()
            .withYieldedApArea { ApAreaEntityFactory().produce() }
            .produce()
        }
        .produce(),
    )
    .produce()

  val assessment = ApprovedPremisesAssessmentEntityFactory()
    .withApplication(application)
    .produce()

  val placementRequirements = PlacementRequirementsEntityFactory()
    .withApplication(application)
    .withAssessment(assessment)
    .produce()

  val premises = ApprovedPremisesEntityFactory()
    .withDefaults()
    .produce()

  val booking = BookingEntityFactory()
    .withId(UUID.randomUUID())
    .withPremises(premises)
    .withArrivalDate(LocalDate.now().minusDays(10))
    .withDepartureDate(LocalDate.now().plusDays(5))
    .withCreatedAt(OffsetDateTime.now())
    .produce()

  val placementRequest =
    PlacementRequestEntityFactory()
      .withPlacementRequirements(placementRequirements)
      .withApplication(application)
      .withAssessment(assessment)
      .withBooking(booking)
      .withSpaceBookings(mutableListOf())
      .produce()

  val spaceBooking = Cas1SpaceBookingEntityFactory()
    .withId(UUID.randomUUID())
    .withPremises(premises)
    .withCanonicalArrivalDate(LocalDate.now().minusDays(10))
    .withCanonicalDepartureDate(LocalDate.now().plusDays(5))
    .withCreatedAt(OffsetDateTime.now())
    .produce()

  @Test
  fun `Transforms placement request booking summary correctly when booking is space booking`() {
    val spaceBookingSummary = PlacementRequestBookingSummary(
      spaceBooking.id,
      spaceBooking.premises.id,
      spaceBooking.premises.name,
      spaceBooking.canonicalArrivalDate,
      spaceBooking.canonicalDepartureDate,
      spaceBooking.createdAt.toInstant(),
      PlacementRequestBookingSummary.Type.space,
    )

    val placementWithSpaceBooking = placementRequest.copy(
      spaceBookings = mutableListOf(spaceBooking),
    )

    every { bookingSummaryTransformer.transformJpaToApi(spaceBooking) } returns spaceBookingSummary

    val result = placementRequestBookingSummaryTransformer.getBookingSummary(placementWithSpaceBooking)

    assertThat(result!!.id).isEqualTo(spaceBooking.id)
    assertThat(result.premisesId).isEqualTo(spaceBooking.premises.id)
    assertThat(result.premisesName).isEqualTo(spaceBooking.premises.name)
    assertThat(result.arrivalDate).isEqualTo(spaceBooking.canonicalArrivalDate)
    assertThat(result.departureDate).isEqualTo(spaceBooking.canonicalDepartureDate)
    assertThat(result.createdAt).isEqualTo(spaceBooking.createdAt.toInstant())
    assertThat(result.type).isEqualTo(PlacementRequestBookingSummary.Type.space)
  }

  @Test
  fun `Transform placement request booking summary returns null when no space bookings`() {
    val placementWithSpaceBooking = placementRequest.copy()

    val result = placementRequestBookingSummaryTransformer.getBookingSummary(placementWithSpaceBooking)

    assertThat(result).isNull()
  }

  @Test
  fun `Transform placement request booking summary returns null when no active space bookings`() {
    val placementWithCancelledSpaceBooking = placementRequest.copy(
      spaceBookings = mutableListOf(
        spaceBooking.copy(
          cancellationOccurredAt = LocalDate.now(),
        ),
      ),
    )
    val result = placementRequestBookingSummaryTransformer.getBookingSummary(placementWithCancelledSpaceBooking)

    assertThat(result).isNull()
  }
}
