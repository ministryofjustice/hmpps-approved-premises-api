package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1SeedRoomsFromSiteSurveyXlsxJob.Companion.buildRoomCode

@Component
class Cas1UpdateRoomCodesJob(
  private val approvedPremisesRepository: ApprovedPremisesRepository,
  private val roomRepository: RoomRepository,
  override val shouldRunInTransaction: Boolean = true,
) : MigrationJob() {

  private val log = LoggerFactory.getLogger(this::class.java)

  override fun process(pageSize: Int) {
    approvedPremisesRepository.findAll().forEach { premises ->

      if (premises.rooms.isNotEmpty()) {
        premises.rooms.forEach { updateRoomIfRequired(premises, it) }
      }
    }
  }

  private fun updateRoomIfRequired(premises: ApprovedPremisesEntity, room: RoomEntity) {
    val actualRoomCode = room.code
    val expectedRoomCode = buildRoomCode(
      qCode = premises.qCode,
      roomNumber = room.name,
    )

    if (actualRoomCode != expectedRoomCode) {
      log.info("Updating premise ${premises.name} room code from $actualRoomCode to $expectedRoomCode")
      roomRepository.updateCode(room.id, expectedRoomCode)
    }
  }
}
