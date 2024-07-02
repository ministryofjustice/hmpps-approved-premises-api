package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface Cas1ApplicationUserDetailsRepository : JpaRepository<Cas1ApplicationUserDetailsEntity, UUID>

@Entity
@Table(name = "cas_1_application_user_details")
class Cas1ApplicationUserDetailsEntity(
  @Id
  val id: UUID,
  val name: String,
  val email: String?,
  val telephoneNumber: String?,
)
