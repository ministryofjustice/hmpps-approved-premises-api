package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Repository
interface Cas3FutureBookingsReportRepository : JpaRepository<BookingEntity, UUID> {
  @Query(
    """
    SELECT
        CAST(booking.id AS VARCHAR) AS bookingId,
        CAST(app.id AS VARCHAR) AS referralId,
        app.submitted_at AS referralDate,
        cas3_app.risk_ratings->'roshRisks'->'value'->>'overallRisk' AS riskOfSeriousHarm,
        cas3_app.is_registered_sex_offender AS registeredSexOffender,
        cas3_app.is_history_of_sexual_offence AS historyOfSexualOffence,
        cas3_app.is_concerning_sexual_behaviour AS concerningSexualBehaviour,
        cas3_app.is_duty_to_refer_submitted AS dutyToReferMade,
        cas3_app.duty_to_refer_submission_date AS dateDutyToReferMade,
        cas3_app.duty_to_refer_local_authority_area_name AS dutyToReferLocalAuthorityAreaName,
        probation_region.name AS probationRegionName,
        probation_delivery_unit.name AS pduName,
        local_authority_area.name AS localAuthorityAreaName,
        premises.address_line1 AS addressLine1,
        premises.postcode as postCode,
        booking.crn AS crn,
        cas3_app.eligibility_reason AS referralEligibilityReason,
        cas3_app.prison_name_on_creation AS prisonNameOnCreation,
        booking.arrival_date AS startDate,
        cas3_app.arrival_date AS accommodationRequiredDate,
        cas3_assessment.accommodation_required_from_date AS updatedAccommodationRequiredDate,
        CAST(confirmation.id AS VARCHAR) AS confirmationId
    FROM bookings booking
    INNER JOIN premises ON premises.id = booking.premises_id
    INNER JOIN probation_regions probation_region ON probation_region.id = premises.probation_region_id
    LEFT JOIN local_authority_areas local_authority_area ON local_authority_area.id = premises.local_authority_area_id
    LEFT JOIN applications app ON booking.application_id = app.id
    LEFT JOIN temporary_accommodation_applications cas3_app ON booking.application_id = cas3_app.id
    LEFT JOIN assessments assessment ON app.id = assessment.application_id
    LEFT JOIN temporary_accommodation_assessments cas3_assessment on cas3_assessment.assessment_id = assessment.id
    LEFT JOIN confirmations confirmation ON confirmation.booking_id = booking.id
    LEFT JOIN cancellations cancellation ON cancellation.booking_id = booking.id
    LEFT JOIN arrivals arrival ON arrival.booking_id = booking.id
    INNER JOIN temporary_accommodation_premises cas3_premises ON cas3_premises.premises_id = premises.id
    LEFT JOIN probation_delivery_units probation_delivery_unit on probation_delivery_unit.id = cas3_app.probation_delivery_unit_id
    WHERE
      COALESCE(cas3_assessment.accommodation_required_from_date,cas3_app.arrival_date) <= :endDate
      AND COALESCE(cas3_assessment.accommodation_required_from_date,cas3_app.arrival_date) >= :startDate
      AND premises.service = 'temporary-accommodation'
      AND (CAST(:probationRegionId AS UUID) IS NULL OR premises.probation_region_id = :probationRegionId)
      AND cancellation.id IS NULL
      AND arrival.id IS NULL
    ORDER BY probation_region.name,probation_delivery_unit.name,cas3_app.arrival_date
    """,
    nativeQuery = true,
  )
  fun findAllFutureBookings(
    startDate: LocalDate,
    endDate: LocalDate,
    probationRegionId: UUID?,
  ): List<FutureBookingsReportData>
}

interface FutureBookingsReportData {
  val bookingId: String
  val referralId: String?
  val referralDate: Instant?
  val riskOfSeriousHarm: String?
  val registeredSexOffender: Boolean?
  val historyOfSexualOffence: Boolean?
  val concerningSexualBehaviour: Boolean?
  val dutyToReferMade: Boolean?
  val dateDutyToReferMade: LocalDate?
  val dutyToReferLocalAuthorityAreaName: String?
  val probationRegionName: String
  val pduName: String?
  val localAuthorityAreaName: String?
  val addressLine1: String
  val town: String?
  val postCode: String
  val crn: String
  val referralEligibilityReason: String?
  val prisonNameOnCreation: String?
  val startDate: LocalDate
  val accommodationRequiredDate: Instant?
  val updatedAccommodationRequiredDate: LocalDate?
  val confirmationId: String?
}
