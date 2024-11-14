package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1

import org.jetbrains.kotlinx.dataframe.DataFrame
import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.ExcelSeedJob

class ApprovedPremisesRoomsSeedFromXLSXJob(
  fileName: String,
  sheetName: String,
  private val premisesRepository: PremisesRepository,
  private val roomRepository: RoomRepository,
  private val bedRepository: BedRepository,
  private val characteristicRepository: CharacteristicRepository,
) : ExcelSeedJob(
  fileName = fileName,
  sheetName = sheetName,
) {
  private val log = LoggerFactory.getLogger(this::class.java)
  override fun processDataFrame(dataFrame: DataFrame<*>) {
    TODO()
  }
}
