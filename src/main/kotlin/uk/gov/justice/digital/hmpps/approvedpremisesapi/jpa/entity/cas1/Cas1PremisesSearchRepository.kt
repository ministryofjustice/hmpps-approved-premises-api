package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesGender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_PREMISES_ESAP
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_PREMISES_PIPE
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_PREMISES_RECOVERY_FOCUSSED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_PREMISES_SEMI_SPECIALIST_MENTAL_HEALTH
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.SqlUtil
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.SqlUtil.getUUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesType
import java.util.UUID

private const val CANDIDATE_PREMISES_QUERY = """
SELECT
  result.*
FROM
(
  SELECT
    p.id AS premises_id,
    CASE 
        WHEN :outcode != 'ANY' THEN 
            ST_Distance(
              (SELECT point FROM postcode_districts pd WHERE pd.outcode = :outcode)::geography,
              ap.point::geography
            ) * 0.000621371
    END AS distance_in_miles,
    CASE
      WHEN ('$CAS1_PROPERTY_NAME_PREMISES_PIPE'=ANY(ARRAY_AGG (premises_chars_resolved.property_name))) THEN 'PIPE'
      WHEN ('$CAS1_PROPERTY_NAME_PREMISES_ESAP'=ANY(ARRAY_AGG (premises_chars_resolved.property_name))) THEN 'ESAP'
      WHEN ('$CAS1_PROPERTY_NAME_PREMISES_RECOVERY_FOCUSSED'=ANY(ARRAY_AGG (premises_chars_resolved.property_name))) THEN 'RFAP'
      WHEN ('$CAS1_PROPERTY_NAME_PREMISES_SEMI_SPECIALIST_MENTAL_HEALTH'=ANY(ARRAY_AGG (premises_chars_resolved.property_name))) THEN 'MHAP'
      ELSE 'NORMAL'
    END AS ap_type,
    p.name AS name,
    ap.full_address AS full_address,
    p.address_line1 AS address_line1,
    p.address_line2 AS address_line2,
    p.town AS town,
    p.postcode AS postcode,
    aa.id AS ap_area_id,
    aa.name AS ap_area_name,
    aa.identifier AS ap_area_identifier,
    ARRAY_REMOVE(ARRAY_AGG (DISTINCT premises_chars_resolved.property_name), null) as premises_characteristics,
    ARRAY_REMOVE(ARRAY_AGG (DISTINCT room_chars_resolved.property_name), null) as room_characteristics,
    ARRAY_REMOVE(ARRAY_AGG (DISTINCT restrictions.description), null) as local_restrictions
  FROM approved_premises ap
  INNER JOIN premises p ON ap.premises_id = p.id
  INNER JOIN probation_regions pr ON p.probation_region_id = pr.id
  INNER JOIN ap_areas aa ON pr.ap_area_id = aa.id
  LEFT OUTER JOIN rooms ON rooms.premises_id = p.id
  LEFT OUTER JOIN premises_characteristics premises_chars ON premises_chars.premises_id = p.id
  LEFT OUTER JOIN characteristics premises_chars_resolved ON premises_chars_resolved.id = premises_chars.characteristic_id
  LEFT OUTER JOIN room_characteristics room_chars ON room_chars.room_id = rooms.id
  LEFT OUTER JOIN characteristics room_chars_resolved ON room_chars_resolved.id = room_chars.characteristic_id
  LEFT OUTER JOIN LATERAL (
    SELECT lr.description
    FROM cas1_premises_local_restrictions lr
    WHERE lr.archived = false AND lr.approved_premises_id = p.id
    ORDER BY lr.created_at DESC
) restrictions ON TRUE
  WHERE 
    p.status != 'archived' AND
    ap.supports_space_bookings = true AND
    (:gender = 'ANY' OR (ap.gender = :gender)) AND
    (:cruManagementAreaIdsCount = 0 OR (ap.cas1_cru_management_area_id IN (:cruManagementAreaIds)))
  GROUP BY p.id, ap.point, p.name, ap.full_address, p.address_line1, p.address_line2, p.town, p.postcode, aa.id, aa.name  
) AS result
WHERE
(
  :premisesCharacteristicsCount = 0 OR 
  :premisesCharacteristicsCount = (
      SELECT COUNT(distinct pc.characteristic_id)
      FROM premises_characteristics pc
      WHERE pc.premises_id = result.premises_id AND pc.characteristic_id IN (:premisesCharacteristics)
  )
) 
AND
(
  :roomCharacteristicsCount = 0 OR 
  :roomCharacteristicsCount = (
    SELECT COUNT(distinct rc.characteristic_id)
    FROM room_characteristics rc
    JOIN rooms r ON rc.room_id = r.id
    WHERE r.premises_id = result.premises_id AND rc.characteristic_id IN (:roomCharacteristics)
  )
)
ORDER BY 
    CASE 
        WHEN :outcode = 'ANY' THEN ap_area_identifier
        ELSE ''
    END,
    CASE 
        WHEN :outcode != 'ANY' THEN distance_in_miles
        ELSE 0
    END,
    name
"""

@Repository
class Cas1SpaceSearchRepository(
  private val jdbcTemplate: NamedParameterJdbcTemplate,
) {
  fun findAllPremisesWithCharacteristicsByDistance(
    targetPostcodeDistrict: String?,
    gender: ApprovedPremisesGender?,
    premisesCharacteristics: List<UUID>,
    roomCharacteristics: List<UUID>,
    cruManagementAreaIds: List<UUID>,
  ): List<CandidatePremises> {
    val parameters = mutableMapOf(
      "outcode" to (targetPostcodeDistrict ?: "ANY"),
      "gender" to (gender?.name ?: "ANY"),
      "premisesCharacteristicsCount" to premisesCharacteristics.size,
      "premisesCharacteristics" to premisesCharacteristics.ifEmpty { null },
      "roomCharacteristicsCount" to roomCharacteristics.size,
      "roomCharacteristics" to roomCharacteristics.ifEmpty { null },
      "cruManagementAreaIdsCount" to cruManagementAreaIds.size,
      "cruManagementAreaIds" to cruManagementAreaIds.ifEmpty { null },
    )

    return jdbcTemplate.query(CANDIDATE_PREMISES_QUERY, parameters) { rs, _ ->
      val apType = when (val apType = rs.getString("ap_type")) {
        "MHAP" -> ApprovedPremisesType.MHAP_ST_JOSEPHS
        else -> ApprovedPremisesType.valueOf(apType)
      }

      CandidatePremises(
        rs.getUUID("premises_id"),
        if (targetPostcodeDistrict == null) null else rs.getFloat("distance_in_miles"),
        apType,
        name = rs.getString("name"),
        fullAddress = rs.getString("full_address"),
        addressLine1 = rs.getString("address_line1"),
        addressLine2 = rs.getString("address_line2"),
        town = rs.getString("town"),
        postcode = rs.getString("postcode"),
        apAreaId = rs.getUUID("ap_area_id"),
        apAreaName = rs.getString("ap_area_name"),
        apAreaIdentifier = rs.getString("ap_area_identifier"),
        characteristics = (
          SqlUtil.toStringList(rs.getArray("premises_characteristics")) +
            SqlUtil.toStringList(rs.getArray("room_characteristics"))
          ),
        localRestrictions = SqlUtil.toStringList(rs.getArray("local_restrictions")),
      )
    }
  }
}

data class CandidatePremises(
  val premisesId: UUID,
  val distanceInMiles: Float?,
  val apType: ApprovedPremisesType,
  val name: String,
  val fullAddress: String?,
  val addressLine1: String,
  val addressLine2: String?,
  val town: String?,
  val postcode: String,
  val apAreaId: UUID,
  val apAreaName: String,
  val apAreaIdentifier: String,
  val characteristics: List<String>,
  val localRestrictions: List<String>,
)
