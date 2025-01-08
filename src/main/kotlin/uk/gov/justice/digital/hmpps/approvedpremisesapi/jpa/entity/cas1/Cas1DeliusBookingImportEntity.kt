package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface Cas1DeliusBookingImportRepository : JpaRepository<Cas1DeliusBookingImportEntity, UUID> {
  fun findByBookingId(id: UUID): Cas1DeliusBookingImportEntity?

  /**
   * Returns all active bookings created in delius that were not created in CAS1
   *
   * An active booking is one that:
   *
   * 1. does not have a departure recorded (and optionally, no arrival recorded)
   * 2. does not have a non arrival recorded
   *
   * We also exclude any bookings where the departure date is before 1/1/2025 or after 1/1/2035.
   * This filters out several bookings where the dates have been set incorrectly but are clearly inactive/complete,
   * most likely because they were created in older versions of delius that did not capture this data
   *
   * Note that the [Cas1DeliusBookingImportEntity] table only includes accepted bookings (i.e. not rejected),
   * so these are already filtered out
   */
  @Query(
    """
    FROM Cas1DeliusBookingImportEntity i
    WHERE 
    i.bookingId IS NULL AND
    i.premisesQcode = :qCode AND 
    i.departureDate IS NULL AND 
    i.nonArrivalReasonCode IS NULL AND 
    i.expectedDepartureDate > :minExpectedDepartureDate AND
    i.expectedDepartureDate < :maxExpectedDepartureDate
    
  """,
  )
  fun findActiveBookingsCreatedInDelius(
    qCode: String,
    minExpectedDepartureDate: LocalDate,
    maxExpectedDepartureDate: LocalDate,
  ): List<Cas1DeliusBookingImportEntity>
}

@Entity
@Table(name = "cas1_delius_booking_import")
data class Cas1DeliusBookingImportEntity(
  @Id
  val id: UUID,
  val bookingId: UUID?,
  val crn: String,
  val eventNumber: String,
  val keyWorkerStaffCode: String?,
  val keyWorkerForename: String?,
  val keyWorkerMiddleName: String?,
  val keyWorkerSurname: String?,
  val departureReasonCode: String?,
  val moveOnCategoryCode: String?,
  val moveOnCategoryDescription: String?,
  val expectedArrivalDate: LocalDate,
  val arrivalDate: LocalDate?,
  val expectedDepartureDate: LocalDate?,
  val departureDate: LocalDate?,
  val nonArrivalDate: LocalDate?,
  val nonArrivalContactDatetime: OffsetDateTime?,
  val nonArrivalReasonCode: String?,
  val nonArrivalReasonDescription: String?,
  val nonArrivalNotes: String?,
  val premisesQcode: String,
)
