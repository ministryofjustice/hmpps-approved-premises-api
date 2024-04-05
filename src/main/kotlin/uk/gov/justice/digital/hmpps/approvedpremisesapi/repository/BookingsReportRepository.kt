package uk.gov.justice.digital.hmpps.approvedpremisesapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import java.sql.Timestamp
import java.time.LocalDate
import java.util.UUID

interface BookingsReportRepository : JpaRepository<BookingEntity, UUID> {
  @Query(
    """
    SELECT
      CAST(booking.id AS VARCHAR) AS bookingId,
      CAST(app.id AS VARCHAR) AS referralId,
      app.submitted_at AS referralDate,
      cas3_app.risk_ratings->'roshRisks'->'value'->>'overallRisk' AS riskOfSeriousHarm,
      cas3_app.is_registered_sex_offender AS registeredSexOffender,
      cas3_app.is_history_of_sexual_offence as historyOfSexualOffence,
      cas3_app.is_concerning_sexual_behaviour as concerningSexualBehaviour,  
      cas3_app.needs_accessible_property AS needForAccessibleProperty,
      cas3_app.has_history_of_arson AS historyOfArsonOffence,
      cas3_app.is_concerning_arson_behaviour as concerningArsonBehaviour,
      cas3_app.is_duty_to_refer_submitted AS dutyToReferMade,
      cas3_app.duty_to_refer_submission_date AS dateDutyToReferMade,
      cas3_app.is_eligible AS isReferralEligibleForCas3,
      cas3_app.eligibility_reason AS referralEligibilityReason,
      probation_region.name AS probationRegionName,
      local_authority_area.name AS localAuthorityAreaName,
      booking.crn AS crn,
      CAST(confirmation.id AS VARCHAR) AS confirmationId,
      CAST(cancellation.id AS VARCHAR) AS cancellationId,
      cancellation_reason.name AS cancellationReason,
      arr.arrival_date AS startDate,
      arr.expected_departure_date AS endDate,
      departure.date_time AS actualEndDate,
      move_on_category.name AS accommodationOutcome,
      cas3_app.duty_to_refer_local_authority_area_name AS dutyToReferLocalAuthorityAreaName,
      pdu.name as pdu,
      premises.town as town,
      premises.postcode as postCode
    FROM
      bookings booking
    LEFT JOIN
      premises premises ON premises.id = booking.premises_id
    LEFT JOIN
      probation_regions probation_region ON probation_region.id = premises.probation_region_id
    LEFT JOIN
      local_authority_areas local_authority_area ON local_authority_area.id = premises.local_authority_area_id  
    LEFT JOIN
      applications app ON booking.application_id = app.id
    LEFT JOIN
      temporary_accommodation_applications cas3_app ON booking.application_id = cas3_app.id
    LEFT JOIN
      confirmations confirmation ON confirmation.booking_id = booking.id
    LEFT JOIN
      departures departure ON departure.booking_id = booking.id AND departure.created_at = (SELECT max(d.created_at) FROM departures d WHERE d.booking_id=booking.id)
    LEFT JOIN
      arrivals arr ON arr.booking_id = booking.id AND arr.created_at = (SELECT max(a.created_at) FROM arrivals a WHERE a.booking_id=booking.id)
    LEFT JOIN
      cancellations cancellation ON cancellation.booking_id = booking.id
    LEFT JOIN
      cancellation_reasons cancellation_reason ON cancellation_reason.id = cancellation.cancellation_reason_id
    LEFT JOIN
      move_on_categories move_on_category ON move_on_category.id = departure.move_on_category_id
    LEFT JOIN 
      temporary_accommodation_premises tap ON tap.premises_id = premises.id  
    LEFT JOIN 
      probation_delivery_units pdu on pdu.id = tap.probation_delivery_unit_id  
    WHERE
      booking.arrival_date <= :endDate
      AND booking.departure_date >= :startDate
      AND premises.service = :serviceName
      AND (CAST(:probationRegionId AS UUID) IS NULL OR premises.probation_region_id = :probationRegionId)
    ORDER BY booking.id
    """,
    nativeQuery = true,
  )
  fun findAllByOverlappingDate(
    startDate: LocalDate,
    endDate: LocalDate,
    serviceName: String,
    probationRegionId: UUID?,
  ): List<BookingsReportData>
}

interface BookingsReportData {
  val bookingId: String
  val referralId: String?
  val referralDate: LocalDate?
  val riskOfSeriousHarm: String?
  val registeredSexOffender: Boolean?
  val historyOfSexualOffence: Boolean?
  val concerningSexualBehaviour: Boolean?
  val needForAccessibleProperty: Boolean?
  val historyOfArsonOffence: Boolean?
  val concerningArsonBehaviour: Boolean?
  val dutyToReferMade: Boolean?
  val dateDutyToReferMade: LocalDate?
  val referralEligibleForCas3: Boolean?
  val referralEligibilityReason: String?
  val probationRegionName: String
  val localAuthorityAreaName: String?
  val crn: String
  val confirmationId: String?
  val cancellationId: String?
  val cancellationReason: String?
  val startDate: LocalDate?
  val endDate: LocalDate?
  val actualEndDate: Timestamp?
  val accommodationOutcome: String?
  val dutyToReferLocalAuthorityAreaName: String?
  val pdu: String?
  val town: String?
  val postCode: String?
}
