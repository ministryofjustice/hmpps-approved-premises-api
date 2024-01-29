package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id

@Repository
interface Cas1WithdrawableDateRepository : JpaRepository<Cas1WithdrawableDateRepository.Cas1WithdrawableDateEntity, Unit> {

  @Query(
    value = """
        SELECT cast(pr.id as text) as id,
               'PLACEMENT_REQUEST' as type,
               pr.expected_arrival as startDate,
               pr.expected_arrival + (interval '1' day * pr.duration) as endDate
        FROM   placement_requests pr
        WHERE  pr.application_id = :applicationId AND 
               pr.reallocated_at IS NULL AND 
               pr.booking_id IS NULL AND
               pr.is_withdrawn IS FALSE
      UNION ALL
        SELECT cast(pa.id as text) as id,
               'PLACEMENT_APPLICATION' as type,
               pad.expected_arrival as startDate,
               pad.expected_arrival + (interval '1' day * pad.duration) as endDate
        FROM   placement_applications pa
        INNER JOIN placement_application_dates pad ON pad.placement_application_id = pa.id 
        WHERE  pa.application_id = :applicationId AND
               pa.submitted_at IS NOT NULL AND
               pa.reallocated_at IS NULL AND
               pa.decision IS NULL
      UNION ALL
        SELECT cast(b.id as text) as id,
               'BOOKING' as type,
               b.arrival_date as startDate,
               b.departure_date as endDate
        FROM   bookings b       
        WHERE  b.application_id = :applicationId AND
               (b.status IS NULL or b.status NOT IN ('cancelled')) AND
               NOT EXISTS (SELECT 1 from arrivals WHERE arrivals.booking_id = b.id)
    """,
    nativeQuery = true,
  )
  fun getWithdrawablesForApplication(applicationId: UUID): List<Cas1WithdrawableDate>

  interface Cas1WithdrawableDate {
    val id: UUID
    val type: WithdrawableDateType
    val startDate: LocalDate
    val endDate: LocalDate
  }

  enum class WithdrawableDateType {
    PLACEMENT_REQUEST,
    PLACEMENT_APPLICATION,
    BOOKING,
  }

  @Entity
  data class Cas1WithdrawableDateEntity(
    @Id
    val id: UUID,
  )
}
