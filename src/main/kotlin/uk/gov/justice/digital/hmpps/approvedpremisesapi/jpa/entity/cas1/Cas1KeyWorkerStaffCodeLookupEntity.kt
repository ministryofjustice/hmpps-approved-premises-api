package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface Cas1KeyWorkerStaffCodeLookupRepository : JpaRepository<Cas1KeyWorkerStaffCodeLookupEntity, UUID> {
  @Query("SELECT l FROM Cas1KeyWorkerStaffCodeLookupEntity l WHERE UPPER(l.staffCode1) = UPPER(:staffCode)")
  fun findByStaffCode1(staffCode: String): Cas1KeyWorkerStaffCodeLookupEntity?
}

@Entity
@Table(name = "cas1_key_worker_staff_code_lookup")
data class Cas1KeyWorkerStaffCodeLookupEntity(
  @Id
  @Column("staff_code_1")
  val staffCode1: String,
  @Column("staff_code_2")
  val staffCode2: String,
)
