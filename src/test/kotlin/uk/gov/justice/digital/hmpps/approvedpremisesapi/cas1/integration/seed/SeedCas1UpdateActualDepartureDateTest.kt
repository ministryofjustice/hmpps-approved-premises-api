package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.integration.seed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonDepartedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.seed.Cas1UpdateActualDepartureDateSeedJobCsvRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.seed.CsvBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.Cas1SimpleApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1SpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed.SeedTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class SeedCas1UpdateActualDepartureDateTest : SeedTestBase() {

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

    val departureReason = departureReasonEntityFactory.produceAndPersist {
      withServiceScope("approved-premises")
    }

    simpleApiClient.recordArrival(
      this,
      spaceBooking.premises.id,
      spaceBooking.id,
      arrivalDate = LocalDate.of(2025, 4, 1),
      arrivalTime = LocalTime.of(12, 0, 0),
    )

    simpleApiClient.recordDeparture(
      this,
      spaceBooking.premises.id,
      spaceBooking.id,
      departureDateTime = Instant.parse("2025-05-01T12:00:00.00Z"),
      reasonId = departureReason.id,
    )

    seed(
      SeedFileType.approvedPremisesUpdateActualDepartureDate,
      rowsToCsv(
        listOf(
          Cas1UpdateActualDepartureDateSeedJobCsvRow(
            spaceBookingId = spaceBooking.id,
            currentDepartureDate = LocalDate.of(2025, 5, 1),
            updatedDepartureDate = LocalDate.of(2025, 7, 1),
          ),
        ),
      ),
    )

    val updatedSpaceBooking = cas1SpaceBookingRepository.findById(spaceBooking.id).get()
    assertThat(updatedSpaceBooking.actualDepartureDate).isEqualTo(LocalDate.of(2025, 7, 1))
    assertThat(updatedSpaceBooking.canonicalDepartureDate).isEqualTo(LocalDate.of(2025, 7, 1))

    val notes = applicationTimelineNoteRepository.findApplicationTimelineNoteEntitiesByApplicationIdAndDeletedAtIsNull(updatedSpaceBooking.application!!.id)
    assertThat(notes).hasSize(1)
    assertThat(notes)
      .extracting("body")
      .contains(
        "Actual departure date for booking at 'My Test Premise' has been updated from 2025-05-01 to 2025-07-01 by application support",
      )

    val domainEventAfterUpdate = domainEventRepository.findByApplicationIdAndType(spaceBooking.application!!.id, DomainEventType.APPROVED_PREMISES_PERSON_DEPARTED)[0]

    val unmarshalledData = jsonMapper.readValue(domainEventAfterUpdate.data, PersonDepartedEnvelope::class.java)
    assertThat(unmarshalledData.eventDetails.departedAt).isEqualTo(Instant.parse("2025-07-01T12:00:00.00Z"))
  }

  private fun rowsToCsv(rows: List<Cas1UpdateActualDepartureDateSeedJobCsvRow>): String {
    val builder = CsvBuilder()
      .withUnquotedFields(
        "space_booking_id",
        "current_departure_date",
        "updated_departure_date",
      )
      .newRow()

    rows.forEach {
      builder
        .withQuotedField(it.spaceBookingId.toString())
        .withQuotedField(it.currentDepartureDate.toString())
        .withQuotedField(it.updatedDepartureDate.toString())
        .newRow()
    }

    return builder.build()
  }
}
