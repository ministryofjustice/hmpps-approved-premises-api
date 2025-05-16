package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedColumns
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationTimelineNoteService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toInstant
import java.time.LocalDate
import java.util.UUID

@Service
class Cas1UpdateActualArrivalDateSeedJob(
  private val cas1SpaceBookingRepository: Cas1SpaceBookingRepository,
  private val applicationTimelineNoteService: ApplicationTimelineNoteService,
  private val domainEventRepository: DomainEventRepository,
  private val objectMapper: ObjectMapper,
) : SeedJob<Cas1UpdateActualArrivalDateSeedJobCsvRow>(
  requiredHeaders = setOf(
    "space_booking_id",
    "current_arrival_date",
    "updated_arrival_date",
  ),
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>): Cas1UpdateActualArrivalDateSeedJobCsvRow {
    val seedColumns = SeedColumns(columns)

    return Cas1UpdateActualArrivalDateSeedJobCsvRow(
      spaceBookingId = seedColumns.getUuidOrNull("space_booking_id")!!,
      currentArrivalDate = seedColumns.getLocalDateOrNull("current_arrival_date")!!,
      updatedArrivalDate = seedColumns.getLocalDateOrNull("updated_arrival_date")!!,
    )
  }

  override fun processRow(row: Cas1UpdateActualArrivalDateSeedJobCsvRow) {
    val id = row.spaceBookingId
    val currentDate = row.currentArrivalDate
    val updatedDate = row.updatedArrivalDate

    val spaceBooking = cas1SpaceBookingRepository.findByIdOrNull(id) ?: error("Could not find space booking with id $id")

    if (spaceBooking.actualArrivalDate != currentDate) {
      error("Expected current actual arrival date to be $currentDate, but was actually ${spaceBooking.actualArrivalDate}")
    }

    log.info("Updating actual and canonical arrival date on ${spaceBooking.id} to $updatedDate")

    spaceBooking.actualArrivalDate = updatedDate
    spaceBooking.canonicalArrivalDate = updatedDate

    cas1SpaceBookingRepository.save(spaceBooking)

    updateDomainEvent(spaceBooking)

    spaceBooking.application?.let {
      applicationTimelineNoteService.saveApplicationTimelineNote(
        applicationId = it.id,
        note = "Actual arrival date for booking at '${spaceBooking.premises.name}' has been updated from $currentDate to $updatedDate by application support",
        user = null,
      )
    }
  }

  fun updateDomainEvent(spaceBooking: Cas1SpaceBookingEntity) {
    val domainEvents = domainEventRepository.findByCas1SpaceBookingId(spaceBooking.id)

    val actualArrivalDateTime = spaceBooking.actualArrivalDate!!.atTime(spaceBooking.actualArrivalTime).toInstant()

    val arrivalDomainEvent = domainEvents.first {
      it.type == DomainEventType.APPROVED_PREMISES_PERSON_ARRIVED
    }

    log.info("Updating domain event ${arrivalDomainEvent.id} of type APPROVED_PREMISES_PERSON_ARRIVED")
    val envelope = objectMapper.readValue(arrivalDomainEvent.data, PersonArrivedEnvelope::class.java)
    val updatedEnvelope = envelope.copy(
      eventDetails = envelope.eventDetails.copy(arrivedAt = actualArrivalDateTime),
    )
    domainEventRepository.updateData(arrivalDomainEvent.id, objectMapper.writeValueAsString(updatedEnvelope))
  }
}

data class Cas1UpdateActualArrivalDateSeedJobCsvRow(
  val spaceBookingId: UUID,
  val currentArrivalDate: LocalDate,
  val updatedArrivalDate: LocalDate,
)
