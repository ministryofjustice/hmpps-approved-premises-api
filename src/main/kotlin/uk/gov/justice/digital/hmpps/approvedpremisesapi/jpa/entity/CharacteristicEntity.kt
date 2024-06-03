package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface CharacteristicRepository : JpaRepository<CharacteristicEntity, UUID> {

  @Query("SELECT c FROM CharacteristicEntity c WHERE c.serviceScope = :serviceName")
  fun findAllByServiceScope(serviceName: String): List<CharacteristicEntity>

  @Query("SELECT c FROM CharacteristicEntity c WHERE c.serviceScope = :serviceName AND c.isActive = true")
  fun findActiveByServiceScope(serviceName: String): List<CharacteristicEntity>

  @Query("SELECT c FROM CharacteristicEntity c WHERE c.isActive = true")
  fun findActive(): List<CharacteristicEntity>

  fun findByName(name: String): CharacteristicEntity?

  fun findByPropertyName(propertyName: String): CharacteristicEntity?

  @Query(
    "SELECT c FROM CharacteristicEntity c " +
      "WHERE c.serviceScope = :serviceName AND c.modelScope = :modelName " +
      "AND c.propertyName = :propertyName",
  )
  fun findByPropertyNameAndScopes(propertyName: String, serviceName: String, modelName: String): CharacteristicEntity?

  fun findAllByName(name: String): List<CharacteristicEntity>

  @Query(
    "SELECT c FROM CharacteristicEntity c " +
      "WHERE c.propertyName IN :names",
  )
  fun findAllWherePropertyNameIn(names: List<String>): List<CharacteristicEntity>

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
}

@Entity
@Table(name = "characteristics")
data class CharacteristicEntity(
  @Id
  var id: UUID,
  var propertyName: String?,
  var name: String,
  var serviceScope: String,
  var modelScope: String,
  var isActive: Boolean,
) {
  fun matches(entityServiceScope: String, entityModelScope: String) = serviceMatches(entityServiceScope) && modelMatches(entityModelScope)
  fun serviceMatches(entityServiceScope: String) = serviceScope == "*" || entityServiceScope == serviceScope
  fun modelMatches(entityModelScope: String) = modelScope == "*" || entityModelScope == modelScope
}
