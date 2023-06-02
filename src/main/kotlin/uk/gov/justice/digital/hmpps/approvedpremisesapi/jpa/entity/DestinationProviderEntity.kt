package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Repository
interface DestinationProviderRepository : JpaRepository<DestinationProviderEntity, UUID> {
  fun findByName(name: String): DestinationProviderEntity?
}

@Entity
@Table(name = "destination_providers")
data class DestinationProviderEntity(
  @Id
  val id: UUID,
  val name: String,
  val isActive: Boolean,
) {
  override fun toString() = "DestinationProviderEntity:$id"
}
