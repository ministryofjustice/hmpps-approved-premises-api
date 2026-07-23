package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.seed

import com.fasterxml.jackson.databind.json.JsonMapper
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonDepartedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.seed.SeedColumns
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.seed.SeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationTimelineNoteService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toInstant
import java.time.LocalDate
import java.util.UUID

@Service
class Cas1UpdateActualDepartureDateSeedJob(
  private val cas1SpaceBookingRepository: Cas1SpaceBookingRepository,
  private val cas1ApplicationTimelineNoteService: Cas1ApplicationTimelineNoteService,
  private val domainEventRepository: DomainEventRepository,
  private val jsonMapper: JsonMapper,
) : SeedJob<Cas1UpdateActualDepartureDateSeedJobCsvRow>(
  requiredHeaders = setOf(
    "space_booking_id",
    "current_departure_date",
    "updated_departure_date",
  ),
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>): Cas1UpdateActualDepartureDateSeedJobCsvRow {
    val seedColumns = SeedColumns(columns)

    return Cas1UpdateActualDepartureDateSeedJobCsvRow(
      spaceBookingId = seedColumns.getUuidOrNull("space_booking_id")!!,
      currentDepartureDate = seedColumns.getLocalDateOrNull("current_departure_date")!!,
      updatedDepartureDate = seedColumns.getLocalDateOrNull("updated_departure_date")!!,
    )
  }

  override fun processRow(row: Cas1UpdateActualDepartureDateSeedJobCsvRow) {
    val id = row.spaceBookingId
    val currentDate = row.currentDepartureDate
    val updatedDate = row.updatedDepartureDate

    val spaceBooking = cas1SpaceBookingRepository.findByIdOrNull(id) ?: error("Could not find space booking with id $id")

    if (spaceBooking.actualDepartureDate != currentDate) {
      error("Expected current actual departure date to be $currentDate, but was actually ${spaceBooking.actualDepartureDate}")
    }

    log.info("Updating actual and canonical departure date on ${spaceBooking.id} to $updatedDate")

    spaceBooking.actualDepartureDate = updatedDate
    spaceBooking.canonicalDepartureDate = updatedDate

    cas1SpaceBookingRepository.save(spaceBooking)

    updateDomainEvent(spaceBooking)

    spaceBooking.application?.let {
      cas1ApplicationTimelineNoteService.saveApplicationTimelineNote(
        applicationId = it.id,
        note = "Actual departure date for booking at '${spaceBooking.premises.name}' has been updated from $currentDate to $updatedDate by application support",
        user = null,
      )
    }
  }

  fun updateDomainEvent(spaceBooking: Cas1SpaceBookingEntity) {
    val domainEvents = domainEventRepository.findByCas1SpaceBookingId(spaceBooking.id)

    val actualDepartureDateTime = spaceBooking.actualDepartureDate!!.atTime(spaceBooking.actualDepartureTime).toInstant()

    val departureDomainEvent = domainEvents.first {
      it.type == DomainEventType.APPROVED_PREMISES_PERSON_DEPARTED
    }

    log.info("Updating domain event ${departureDomainEvent.id} of type APPROVED_PREMISES_PERSON_DEPARTED")
    val envelope = jsonMapper.readValue(departureDomainEvent.data, PersonDepartedEnvelope::class.java)
    val updatedEnvelope = envelope.copy(
      eventDetails = envelope.eventDetails.copy(departedAt = actualDepartureDateTime),
    )
    domainEventRepository.updateData(departureDomainEvent.id, jsonMapper.writeValueAsString(updatedEnvelope))
  }
}

data class Cas1UpdateActualDepartureDateSeedJobCsvRow(
  val spaceBookingId: UUID,
  val currentDepartureDate: LocalDate,
  val updatedDepartureDate: LocalDate,
)
