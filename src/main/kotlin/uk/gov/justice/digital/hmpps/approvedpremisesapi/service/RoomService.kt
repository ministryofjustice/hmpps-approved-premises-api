package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspacesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import java.util.UUID

@Service
class RoomService(
  private val roomRepository: RoomRepository,
  private val bedRepository: BedRepository,
  private val bookingRepository: BookingRepository,
  private val cas3VoidBedspacesRepository: Cas3VoidBedspacesRepository,
) {
  fun getRoom(roomId: UUID) = roomRepository.findByIdOrNull(roomId)

  @Transactional
  fun deleteRoom(room: RoomEntity): ValidatableActionResult<Unit> = validated {
    val bedIds = room.beds.map { it.id }

    if (bookingRepository.findByBedIds(bedIds).any()) {
      return room.id hasConflictError "A room cannot be hard-deleted if it has any bookings associated with it"
    }

    cas3VoidBedspacesRepository.findByBedIds(bedIds).forEach { voidBedspace ->
      cas3VoidBedspacesRepository.delete(voidBedspace)
    }

    room.beds.forEach { bed ->
      bedRepository.delete(bed)
    }
    roomRepository.delete(room)

    success(Unit)
  }
}
