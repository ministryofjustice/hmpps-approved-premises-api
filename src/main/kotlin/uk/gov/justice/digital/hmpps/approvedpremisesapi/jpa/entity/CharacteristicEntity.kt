package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

interface CharacteristicRepository : JpaRepository<CharacteristicEntity, UUID> {

  @Query("SELECT c FROM CharacteristicEntity c WHERE c.serviceScope = :xServiceName")
  fun findAllByServiceScope(xServiceName: ServiceName): List<CharacteristicEntity>
}

@Entity
@Table(name = "characteristics")
data class CharacteristicEntity(
  @Id
  var id: UUID,
  var name: String,
  var serviceScope: String,
  var modelScope: String
)
