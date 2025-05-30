package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@SuppressWarnings("LongParameterList")
@Entity
@Table(name = "cas3_bedspaces")
@Inheritance(strategy = InheritanceType.JOINED)
class Cas3BedspacesEntity(
  @Id
  val id: UUID,

  @ManyToOne
  @JoinColumn(name = "premises_id")
  val premises: Cas3PremisesEntity,

  val reference: String,
  val notes: String?,
  val startDate: LocalDate?,
  val endDate: LocalDate?,
  val createdAt: OffsetDateTime?,
)

@Repository
interface Cas3BedspacesRepository : JpaRepository<Cas3BedspacesEntity, UUID>
