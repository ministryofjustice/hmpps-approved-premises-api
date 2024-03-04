package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Repository
interface Cas1ApplicationUserDetailsRepository : JpaRepository<Cas1ApplicationUserDetailsEntity, UUID>

@Entity
@Table(name = "cas_1_application_user_details")
class Cas1ApplicationUserDetailsEntity(
  @Id
  val id: UUID,
  val name: String,
  val email: String,
  val telephoneNumber: String,
)
