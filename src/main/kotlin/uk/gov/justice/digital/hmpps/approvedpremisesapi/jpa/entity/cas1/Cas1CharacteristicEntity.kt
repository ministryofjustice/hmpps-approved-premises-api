package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface Cas1CharacteristicRepository : JpaRepository<Cas1CharacteristicEntity, UUID> {

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
  }

  @Query(
    "SELECT c FROM Cas1CharacteristicEntity c " +
      "WHERE (:modelScope = '*' OR c.modelScope = :modelScope OR c.modelScope = '*') AND c.isActive = true",
  )
  fun findActiveByModelScope(modelScope: String): List<Cas1CharacteristicEntity>

  @Query(
    "SELECT c FROM Cas1CharacteristicEntity c " +
      "WHERE c.propertyName = :propertyName",
  )
  fun findByPropertyName(propertyName: String): Cas1CharacteristicEntity?

  @Query(
    "SELECT c FROM Cas1CharacteristicEntity c " +
      "WHERE c.propertyName = :propertyName AND c.modelScope = :modelName",
  )
  fun findByPropertyNameAndModelScope(propertyName: String, modelName: String): Cas1CharacteristicEntity?

  @Query(
    "SELECT c FROM Cas1CharacteristicEntity c " +
      "WHERE c.propertyName IN :names",
  )
  fun findAllWherePropertyNameIn(names: List<String>): List<Cas1CharacteristicEntity>

  @Query(
    """
      SELECT c.*
      FROM cas1_characteristics c
      LEFT JOIN room_characteristics rc ON rc.characteristic_id = c.id
      WHERE rc.room_id = :roomId
    """,
    nativeQuery = true,
  )
  fun findAllForRoomId(roomId: UUID): List<Cas1CharacteristicEntity>
}

@Entity
@Table(name = "cas1_characteristics")
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
data class Cas1CharacteristicEntity(
  @Id
  var id: UUID,
  var propertyName: String?,
  var name: String,
  var modelScope: String,
  var isActive: Boolean,
) {
  fun isModelScopeRoom() = modelMatches("room")
  fun modelMatches(entityModelScope: String) = modelScope == "*" || entityModelScope == modelScope
  fun isModelScopePremises() = modelMatches("premises")
}
