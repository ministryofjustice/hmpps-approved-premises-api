package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas3.Cas3BedspaceCharacteristicMappingId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas3.Cas3PremisesCharacteristicMappingId
import java.util.UUID

@Repository
interface CharacteristicRepository : JpaRepository<CharacteristicEntity, UUID> {

  companion object Constants {
    const val CAS1_PROPERTY_NAME_PREMISES_PIPE = "isPIPE"
    const val CAS1_PROPERTY_NAME_PREMISES_ESAP = "isESAP"
    const val CAS1_PROPERTY_NAME_PREMISES_RECOVERY_FOCUSSED = "isRecoveryFocussed"
    const val CAS1_PROPERTY_NAME_PREMISES_SEMI_SPECIALIST_MENTAL_HEALTH = "isSemiSpecialistMentalHealth"
    const val CAS1_PROPERTY_NAME_PREMISES_ACCEPTS_CHILD_SEX_OFFENDERS = "acceptsChildSexOffenders"
    const val CAS1_PROPERTY_NAME_PREMISES_ACCEPTS_NON_SEXUAL_CHILD_OFFENDERS = "acceptsNonSexualChildOffenders"
    const val CAS1_PROPERTY_NAME_PREMISES_ACCEPTS_SEX_OFFENDERS = "acceptsSexOffenders"
    const val CAS1_PROPERTY_NAME_PREMISES_CATERED = "isCatered"
    const val CAS1_PROPERTY_NAME_PREMISES_SUITABLE_FOR_VULNERABLE = "isSuitableForVulnerable"
    const val CAS1_PROPERTY_NAME_PREMISES_ELLIOT_HOUSE = "isMHAPElliottHouse"
    const val CAS1_PROPERTY_NAME_PREMISES_ST_JOSEPHS = "isMHAPStJosephs"

    const val CAS1_PROPERTY_NAME_ARSON_SUITABLE = "isArsonSuitable"
    const val CAS1_PROPERTY_NAME_ENSUITE = "hasEnSuite"
    const val CAS1_PROPERTY_NAME_SINGLE_ROOM = "isSingle"
    const val CAS1_PROPERTY_NAME_STEP_FREE_DESIGNATED = "isStepFreeDesignated"
    const val CAS1_PROPERTY_NAME_SUITED_FOR_SEX_OFFENDERS = "isSuitedForSexOffenders"
    const val CAS1_PROPERTY_NAME_WHEELCHAIR_DESIGNATED = "isWheelchairDesignated"

    const val CAS3_PROPERTY_NAME_SINGLE_OCCUPANCY = "isSingleOccupancy"
    const val CAS3_PROPERTY_NAME_SHARED_PROPERTY = "isSharedProperty"
    const val CAS3_PROPERTY_NAME_WHEELCHAIR_ACCESSIBLE = "isWheelchairAccessible"
    const val CAS3_PROPERTY_NAME_MEN_ONLY = "isMenOnly"
    const val CAS3_PROPERTY_NAME_WOMEN_ONLY = "isWomenOnly"
    const val CAS3_PROPERTY_NAME_PUB_NEAR_BY = "isPubNearBy"
  }

  @Query(
    "SELECT c FROM CharacteristicEntity c WHERE (c.serviceScope = :serviceScope OR :serviceScope = '*' )" +
      "AND (:modelScope = '*' OR c.modelScope = :modelScope OR c.modelScope = '*') AND c.isActive = true",
  )
  fun findActiveByServiceScopeAndModelScope(serviceScope: String, modelScope: String): List<CharacteristicEntity>

  fun findByName(name: String): CharacteristicEntity?

  @Query(
    "SELECT c FROM CharacteristicEntity c " +
      "WHERE c.propertyName = :propertyName AND c.serviceScope = :serviceName",
  )
  fun findByPropertyName(propertyName: String, serviceName: String): CharacteristicEntity?

  @Query(
    "SELECT c FROM CharacteristicEntity c " +
      "WHERE c.propertyName = :propertyName AND c.serviceScope = 'approved-premises'",
  )
  fun findCas1ByPropertyName(propertyName: String): CharacteristicEntity?

  @Query(
    "SELECT c FROM CharacteristicEntity c " +
      "WHERE c.propertyName = :propertyName AND c.modelScope = :modelName AND c.serviceScope = 'approved-premises'",
  )
  fun findCas1ByPropertyNameAndScope(propertyName: String, modelName: String): CharacteristicEntity?

  @Query(
    "SELECT c FROM CharacteristicEntity c " +
      "WHERE c.serviceScope = :serviceName AND c.modelScope = :modelName " +
      "AND c.propertyName = :propertyName",
  )
  fun findByPropertyNameAndScopes(propertyName: String, serviceName: String, modelName: String): CharacteristicEntity?

  fun findAllByName(name: String): List<CharacteristicEntity>

  @Query(
    "SELECT c FROM CharacteristicEntity c " +
      "WHERE c.propertyName IN :names AND c.serviceScope = :serviceName",
  )
  fun findAllWherePropertyNameIn(names: List<String>, serviceName: String): List<CharacteristicEntity>

  @Query(
    """
      SELECT c.*
      FROM characteristics c
      LEFT JOIN room_characteristics rc ON rc.characteristic_id = c.id
      WHERE rc.room_id = :roomId
    """,
    nativeQuery = true,
  )
  fun findAllForRoomId(roomId: UUID): List<CharacteristicEntity>

  @Query(
    """
      SELECT b.id AS bedspaceId, c.id AS bedspaceCharacteristicsId 
      FROM characteristics c
      JOIN room_characteristics rc ON rc.characteristic_id = c.id
      JOIN beds b ON b.room_id = rc.room_id
      WHERE b.id IN :bedIds
    """,
    nativeQuery = true,
  )
  fun findBedspaceCharacteristicsMappingsByBedIds(bedIds: List<UUID>): List<Cas3BedspaceCharacteristicMappingId>

  @Query(
    """
      SELECT p.id AS premisesId, c.id AS premisesCharacteristicsId
      FROM characteristics c
      JOIN premises_characteristics pc ON pc.characteristic_id = c.id
      JOIN premises p ON p.id = pc.premises_id
      WHERE p.id IN :premisesIds
    """,
    nativeQuery = true,
  )
  fun findPremisesCharacteristicsMappingsByPremiseIds(premisesIds: List<UUID>): List<Cas3PremisesCharacteristicMappingId>

  @Query(
    """
      SELECT c.*
      FROM characteristics c
      WHERE (model_scope = '*' OR model_scope = :modelScope)
      AND (service_scope = '*' OR service_scope = :serviceScope)
    """,
    nativeQuery = true,
  )
  fun findAllCharacteristicsReferenceData(modelScope: String, serviceScope: String): List<CharacteristicEntity>
}

@Entity
@Table(name = "characteristics")
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
data class CharacteristicEntity(
  @Id
  var id: UUID,
  var propertyName: String?,
  var name: String,
  var serviceScope: String,
  var modelScope: String,
  var isActive: Boolean,
) {
  fun isModelScopeRoom() = modelMatches("room")
  fun matches(entityServiceScope: String, entityModelScope: String) = serviceMatches(entityServiceScope) && modelMatches(entityModelScope)
  fun serviceMatches(entityServiceScope: String) = serviceScope == "*" || entityServiceScope == serviceScope
  fun modelMatches(entityModelScope: String) = modelScope == "*" || entityModelScope == modelScope
  fun isModelScopePremises() = modelMatches("premises")
}
