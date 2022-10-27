package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

interface CharacteristicRepository : JpaRepository<CharacteristicEntity, UUID>

@Entity
@Table(name = "characteristics")
data class CharacteristicEntity(
  @Id
  var id: UUID,
  var name: String,
  var serviceScope: String,
  var modelScope: String
)
