package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.seed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.Cas1SimpleApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1SpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed.SeedTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.CsvBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1UpdateActualArrivalDateSeedJobCsvRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class SeedCas1UpdateActualArrivalDateTest : SeedTestBase() {

  @Autowired
  lateinit var simpleApiClient: Cas1SimpleApiClient

  @Autowired
  lateinit var domainEventService: Cas1DomainEventService

  @Test
  fun success() {
    val offender = givenAnOffender().first

    val spaceBooking = givenACas1SpaceBooking(
      crn = offender.otherIds.crn,
      deliusEventNumber = "101",
      premises = givenAnApprovedPremises(name = "My Test Premise"),
    )

    simpleApiClient.recordArrival(
      this,
      spaceBooking.premises.id,
      spaceBooking.id,
      arrivalDate = LocalDate.of(2025, 5, 1),
      arrivalTime = LocalTime.of(12, 0, 0),
    )

    seed(
      SeedFileType.approvedPremisesUpdateActualArrivalDate,
      rowsToCsv(
        listOf(
          Cas1UpdateActualArrivalDateSeedJobCsvRow(
            spaceBookingId = spaceBooking.id,
            currentArrivalDate = LocalDate.of(2025, 5, 1),
            updatedArrivalDate = LocalDate.of(2025, 7, 1),
          ),
        ),
      ),
    )

    val updatedSpaceBooking = cas1SpaceBookingRepository.findById(spaceBooking.id).get()
    assertThat(updatedSpaceBooking.actualArrivalDate).isEqualTo(LocalDate.of(2025, 7, 1))
    assertThat(updatedSpaceBooking.canonicalArrivalDate).isEqualTo(LocalDate.of(2025, 7, 1))

    val notes = applicationTimelineNoteRepository.findApplicationTimelineNoteEntitiesByApplicationIdAndDeletedAtIsNull(updatedSpaceBooking.application!!.id)
    assertThat(notes).hasSize(1)
    assertThat(notes)
      .extracting("body")
      .contains(
        "Actual arrival date for booking at 'My Test Premise' has been updated from 2025-05-01 to 2025-07-01 by application support",
      )

    val domainEventAfterUpdate = domainEventRepository.findByApplicationId(spaceBooking.application!!.id)[0]

    val unmarshalledData = objectMapper.readValue(domainEventAfterUpdate.data, PersonArrivedEnvelope::class.java)
    assertThat(unmarshalledData.eventDetails.arrivedAt).isEqualTo(Instant.parse("2025-07-01T12:00:00.00Z"))
  }

  private fun rowsToCsv(rows: List<Cas1UpdateActualArrivalDateSeedJobCsvRow>): String {
    val builder = CsvBuilder()
      .withUnquotedFields(
        "space_booking_id",
        "current_arrival_date",
        "updated_arrival_date",
      )
      .newRow()

    rows.forEach {
      builder
        .withQuotedField(it.spaceBookingId.toString())
        .withQuotedField(it.currentArrivalDate.toString())
        .withQuotedField(it.updatedArrivalDate.toString())
        .newRow()
    }

    return builder.build()
  }
}
