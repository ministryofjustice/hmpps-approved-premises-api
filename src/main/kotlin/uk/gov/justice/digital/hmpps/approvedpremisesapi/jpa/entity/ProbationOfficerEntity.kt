package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.OneToMany
import javax.persistence.Table

@Repository
interface ProbationOfficerRepository : JpaRepository<ProbationOfficerEntity, UUID> {
  fun findByDistinguishedName(distinguishedName: String): ProbationOfficerEntity?
}

@Entity
@Table(name = "probation_officers")
data class ProbationOfficerEntity(
  @Id
  val id: UUID,
  val name: String,
  val distinguishedName: String,
  var isActive: Boolean,
  @OneToMany(mappedBy = "createdByProbationOfficer")
  val applications: MutableList<ApplicationEntity>
)
