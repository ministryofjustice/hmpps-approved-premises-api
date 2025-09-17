package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.migration

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspaceCharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspaceCharacteristicRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspacesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspacesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesCharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesCharacteristicRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationInBatchesJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationLogger
import java.util.UUID

@Component
class Cas3MigrateNewBedspaceModelDataJob(
  private val temporaryAccommodationPremisesRepository: TemporaryAccommodationPremisesRepository,
  private val cas3PremisesRepository: Cas3PremisesRepository,
  private val cas3BedspacesRepository: Cas3BedspacesRepository,
  private val cas3PremisesCharacteristicRepository: Cas3PremisesCharacteristicRepository,
  private val cas3BedspaceCharacteristicRepository: Cas3BedspaceCharacteristicRepository,
  private val characteristicRepository: CharacteristicRepository,
  private val cas3BedspaceCharacteristicMappingRepository: Cas3BedspaceCharacteristicMappingRepository,
  private val cas3PremisesCharacteristicMappingRepository: Cas3PremisesCharacteristicMappingRepository,
  private val migrationLogger: MigrationLogger,
  transactionTemplate: TransactionTemplate,
) : MigrationInBatchesJob(migrationLogger, transactionTemplate) {
  override val shouldRunInTransaction: Boolean = java.lang.Boolean.FALSE

  override fun process(pageSize: Int) {
    migrationLogger.info("Starting migration process...")
    migrateDataToCas3CharacteristicsTables()
    val cas3PremisesIds = temporaryAccommodationPremisesRepository.findTemporaryAccommodationPremisesIds()
    super.processInBatches(cas3PremisesIds, batchSize = 100) { batchIds ->
      migrateDataToNewBedspaceModelTables(batchIds)
    }
    migrationLogger.info("Completed migration process...")
  }

  private fun migrateDataToCas3CharacteristicsTables() {
    val cas3PremisesCharacteristicsReferenceData = generateCas3PremisesCharacteristics()
    cas3PremisesCharacteristicRepository.saveAllAndFlush(cas3PremisesCharacteristicsReferenceData)
    val cas3BedspaceCharacteristicsReferenceData = generateCas3BedspacesCharacteristics()
    cas3BedspaceCharacteristicRepository.saveAllAndFlush(cas3BedspaceCharacteristicsReferenceData)
  }

  private fun migrateDataToNewBedspaceModelTables(premiseIds: List<UUID>) {
    migrationLogger.info("Starting bedspace model migration with batch size of ${premiseIds.size}...")
    val temporaryAccommodationPremisesBatch = temporaryAccommodationPremisesRepository.findTemporaryAccommodationPremisesByIds(premiseIds)
    val cas3PremisesBatch = generateCas3PremisesBatch(temporaryAccommodationPremisesBatch)
    cas3PremisesRepository.saveAllAndFlush(cas3PremisesBatch)
    val cas3BedspacesBatch = generateCas3BedspacesBatch(cas3PremisesBatch, temporaryAccommodationPremisesBatch)
    cas3BedspacesRepository.saveAllAndFlush(cas3BedspacesBatch)
    val cas3BedspacesCharacteristicsBatch = generateCas3BedspacesCharacteristicsMappingBatch(cas3BedspacesBatch)
    cas3BedspaceCharacteristicMappingRepository.saveAllAndFlush(cas3BedspacesCharacteristicsBatch)
    val cas3PremisesCharacteristicsBatch = generateCas3PremisesCharacteristicsMappingBatch(cas3PremisesBatch)
    cas3PremisesCharacteristicMappingRepository.saveAllAndFlush(cas3PremisesCharacteristicsBatch)
  }

  private fun generateCas3PremisesBatch(temporaryAccommodationPremises: List<TemporaryAccommodationPremisesEntity>) = temporaryAccommodationPremises.map { premise ->
    Cas3PremisesEntity(
      id = premise.id,
      name = premise.name,
      postcode = premise.postcode,
      addressLine1 = premise.addressLine1,
      addressLine2 = premise.addressLine2,
      town = premise.town,
      localAuthorityArea = premise.localAuthorityArea,
      turnaroundWorkingDays = premise.turnaroundWorkingDays,
      status = premise.cas3PremisesStatus,
      notes = premise.notes,
      probationDeliveryUnit = premise.probationDeliveryUnit!!,
      bedspaces = emptyList<Cas3BedspacesEntity>().toMutableList(),
      characteristics = emptyList<Cas3PremisesCharacteristicEntity>().toMutableList(),
      bookings = emptyList<Cas3BookingEntity>().toMutableList(),
      startDate = premise.startDate,
      endDate = premise.endDate,
    )
  }

  private fun generateCas3BedspacesBatch(
    cas3PremisesBatch: List<Cas3PremisesEntity>,
    temporaryAccommodationPremisesBatch: List<TemporaryAccommodationPremisesEntity>,
  ) = cas3PremisesBatch.map { cas3Premises ->
    temporaryAccommodationPremisesBatch
      .first { it.id == cas3Premises.id }
      .rooms.map { room ->
        val bed = room.beds.first()
        Cas3BedspacesEntity(
          id = bed.id,
          premises = cas3Premises,
          reference = room.name,
          notes = room.notes,
          startDate = bed.startDate!!,
          endDate = bed.endDate,
          createdAt = bed.createdAt,
          createdDate = bed.createdDate!!,
          characteristics = emptyList<Cas3BedspaceCharacteristicEntity>().toMutableList(),
        )
      }
  }.flatten()

  private fun generateCas3BedspacesCharacteristics(): List<Cas3BedspaceCharacteristicEntity> = characteristicRepository.findAllByServiceAndModelScope(
    modelScope = "room",
    serviceScope = ServiceName.temporaryAccommodation.value,
  ).map {
    Cas3BedspaceCharacteristicEntity(
      id = it.id,
      name = it.propertyName,
      description = it.name,
      isActive = it.isActive,
    )
  }

  private fun generateCas3PremisesCharacteristics() = characteristicRepository.findAllByServiceAndModelScope(
    modelScope = "premises",
    serviceScope = ServiceName.temporaryAccommodation.value,
  ).map {
    Cas3PremisesCharacteristicEntity(
      id = it.id,
      name = it.propertyName!!,
      description = it.name,
      isActive = it.isActive,
    )
  }

  private fun generateCas3BedspacesCharacteristicsMappingBatch(cas3BedspacesBatch: List<Cas3BedspacesEntity>) = characteristicRepository.findBedspaceCharacteristicsMappingsByBedIds(cas3BedspacesBatch.map { it.id })
    .map { Cas3BedspaceCharacteristicAssignmentEntity(id = it) }

  private fun generateCas3PremisesCharacteristicsMappingBatch(cas3PremisesBatch: List<Cas3PremisesEntity>) = characteristicRepository.findPremisesCharacteristicsMappingsByPremiseIds(cas3PremisesBatch.map { it.id })
    .map { Cas3PremisesCharacteristicAssignmentEntity(id = it) }
}

@Repository
interface TemporaryAccommodationPremisesRepository : JpaRepository<TemporaryAccommodationPremisesEntity, UUID> {
  @Query("SELECT tap.id FROM TemporaryAccommodationPremisesEntity tap")
  fun findTemporaryAccommodationPremisesIds(): List<UUID>

  @Query("SELECT tap FROM TemporaryAccommodationPremisesEntity tap WHERE tap.id IN :ids")
  fun findTemporaryAccommodationPremisesByIds(ids: List<UUID>): List<TemporaryAccommodationPremisesEntity>

  @Query("SELECT tap FROM TemporaryAccommodationPremisesEntity tap WHERE tap.createdAt IS NULL")
  fun <T : TemporaryAccommodationPremisesEntity> findTemporaryAccommodationPremisesByCreatedAtNull(
    type: Class<T>,
    pageable: Pageable?,
  ): Slice<TemporaryAccommodationPremisesEntity>
}

@Repository
interface Cas3BedspaceCharacteristicMappingRepository : JpaRepository<Cas3BedspaceCharacteristicAssignmentEntity, UUID>

@Repository
interface Cas3PremisesCharacteristicMappingRepository : JpaRepository<Cas3PremisesCharacteristicAssignmentEntity, UUID>

@Entity
@Table(name = "cas3_bedspace_characteristic_assignments")
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
data class Cas3BedspaceCharacteristicAssignmentEntity(
  @EmbeddedId
  val id: Cas3BedspaceCharacteristicAssignmentId,
)

@Entity
@Table(name = "cas3_premises_characteristic_assignments")
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
data class Cas3PremisesCharacteristicAssignmentEntity(
  @EmbeddedId
  val id: Cas3PremisesCharacteristicAssignmentId,
)

@Embeddable
data class Cas3BedspaceCharacteristicAssignmentId(
  @Column(name = "bedspace_id")
  private val bedspaceId: UUID,
  @Column(name = "bedspace_characteristics_id")
  private val bedspaceCharacteristicsId: UUID,
)

@Embeddable
data class Cas3PremisesCharacteristicAssignmentId(
  @Column(name = "premises_id")
  private val premisesId: UUID,
  @Column(name = "premises_characteristics_id")
  private val premisesCharacteristicsId: UUID,
)
