package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesType
import java.sql.ResultSet
import java.time.LocalDate
import java.util.UUID

private const val AP_TYPE_FILTER = """
  AND result.ap_type IN (:apTypes)
"""

private const val GENDER_FILTER = """
  AND EXISTS (
    SELECT 1
    FROM premises_characteristics pc
    WHERE
      pc.premises_id = result.premises_id
      AND pc.characteristic_id IN (:genderCharacteristics)
  )
"""

private const val PREMISES_CHARACTERISTICS_FILTER = """
  AND (
    SELECT COUNT(*)
    FROM premises_characteristics pc
    WHERE
      pc.premises_id = result.premises_id
      AND pc.characteristic_id IN (:premisesCharacteristics)
  ) = :premisesCharacteristicCount
"""

private const val ROOM_CHARACTERISTICS_FILTER = """
  AND (
    SELECT COUNT(*)
    FROM room_characteristics rc
    JOIN rooms r
    ON rc.room_id = r.id
    WHERE
      r.premises_id = result.premises_id
      AND rc.characteristic_id IN (:roomCharacteristics)
  ) = :roomCharacteristicCount
"""

private const val CANDIDATE_PREMISES_QUERY_TEMPLATE = """
SELECT
  result.*
FROM
(
  SELECT
    p.id AS premises_id,
    ST_Distance(
      (SELECT point FROM postcode_districts pd WHERE pd.outcode = :outcode)::geography,
      ap.point::geography
    ) * 0.000621371 AS distance_in_miles,
    ap.ap_code AS ap_code,
    ap.q_code AS delius_q_code,
    CASE
      WHEN EXISTS (
        SELECT 1
        FROM premises_characteristics pc
        JOIN characteristics c
        ON pc.characteristic_id = c.id
        WHERE
          c.property_name = 'isPIPE'
          AND pc.premises_id = p.id
      ) THEN 'PIPE'
      WHEN EXISTS (
        SELECT 1
        FROM premises_characteristics pc
        JOIN characteristics c
        ON pc.characteristic_id = c.id
        WHERE
          c.property_name = 'isESAP'
          AND pc.premises_id = p.id
      ) THEN 'ESAP'
      WHEN EXISTS (
        SELECT 1
        FROM premises_characteristics pc
        JOIN characteristics c
        ON pc.characteristic_id = c.id
        WHERE
          c.property_name = 'isRecoveryFocussed'
          AND pc.premises_id = p.id
      ) THEN 'RFAP'
      WHEN EXISTS (
        SELECT 1
        FROM premises_characteristics pc
        JOIN characteristics c
        ON pc.characteristic_id = c.id
        WHERE
          c.property_name = 'isSemiSpecialistMentalHealth'
          AND pc.premises_id = p.id
      ) THEN 'MHAP'
      ELSE 'NORMAL'
    END AS ap_type,
    p.name AS name,
    p.address_line1 AS address_line1,
    p.address_line2 AS address_line2,
    p.town AS town,
    p.postcode AS postcode,
    aa.id AS ap_area_id,
    aa.name AS ap_area_name,
    (
      SELECT COUNT(*)
      FROM beds b
      JOIN rooms r
      ON b.room_id = r.id
      WHERE r.premises_id = p.id
    ) AS total_spaces_count
  FROM approved_premises ap
  JOIN premises p
  ON ap.premises_id = p.id
  JOIN probation_regions pr
  ON p.probation_region_id = pr.id
  JOIN ap_areas aa
  ON pr.ap_area_id = aa.id
  WHERE ap.supports_space_bookings = true
) AS result
WHERE
  1 = 1
#GENDER_FILTER#
#AP_TYPE_FILTER#
#PREMISES_CHARACTERISTICS_FILTER#
#ROOM_CHARACTERISTICS_FILTER#
ORDER BY result.distance_in_miles
"""

private const val SPACE_AVAILABILITY_QUERY = """
SELECT
  p.id AS premises_id
FROM premises p
WHERE id IN (:premisesIds)
"""

@Repository
class Cas1SpaceSearchRepository(
  private val jdbcTemplate: NamedParameterJdbcTemplate,
) {
  fun findAllPremisesWithCharacteristicsByDistance(
    targetPostcodeDistrict: String,
    apTypes: List<ApprovedPremisesType>,
    genderCharacteristics: List<UUID>,
    premisesCharacteristics: List<UUID>,
    roomCharacteristics: List<UUID>,
  ): List<CandidatePremises> {
    val (query, parameters) = resolveCandidatePremisesQueryTemplate(
      targetPostcodeDistrict,
      apTypes,
      genderCharacteristics,
      premisesCharacteristics,
      roomCharacteristics,
    )

    return jdbcTemplate.query(query, parameters) { rs, _ ->
      val apType = when (val apType = rs.getString("ap_type")) {
        "MHAP" -> ApprovedPremisesType.MHAP_ST_JOSEPHS
        else -> ApprovedPremisesType.valueOf(apType)
      }

      CandidatePremises(
        rs.getUUID("premises_id"),
        rs.getFloat("distance_in_miles"),
        rs.getString("ap_code"),
        rs.getString("delius_q_code"),
        apType,
        rs.getString("name"),
        rs.getString("address_line1"),
        rs.getString("address_line2"),
        rs.getString("town"),
        rs.getString("postcode"),
        rs.getUUID("ap_area_id"),
        rs.getString("ap_area_name"),
        rs.getInt("total_spaces_count"),
      )
    }
  }

  private fun resolveCandidatePremisesQueryTemplate(
    targetPostcodeDistrict: String,
    apTypes: List<ApprovedPremisesType>,
    genderCharacteristics: List<UUID>,
    premisesCharacteristics: List<UUID>,
    roomCharacteristics: List<UUID>,
  ): Pair<String, Map<String, Any>> {
    var query = CANDIDATE_PREMISES_QUERY_TEMPLATE
    val params = mutableMapOf<String, Any>(
      "outcode" to targetPostcodeDistrict,
    )

    val transformedApTypes = apTypes.transformForQuery()

    when {
      transformedApTypes.isEmpty() -> {
        query = query.replace("#AP_TYPE_FILTER#", "")
      }
      else -> {
        query = query.replace("#AP_TYPE_FILTER#", AP_TYPE_FILTER)
        params["apTypes"] = transformedApTypes
      }
    }

    when {
      genderCharacteristics.isEmpty() -> {
        query = query.replace("#GENDER_FILTER#", "")
      }
      else -> {
        query = query.replace("#GENDER_FILTER#", GENDER_FILTER)
        params["genderCharacteristics"] = genderCharacteristics
      }
    }

    when {
      premisesCharacteristics.isEmpty() -> {
        query = query.replace("#PREMISES_CHARACTERISTICS_FILTER#", "")
      }
      else -> {
        query = query.replace("#PREMISES_CHARACTERISTICS_FILTER#", PREMISES_CHARACTERISTICS_FILTER)
        params["premisesCharacteristics"] = premisesCharacteristics
        params["premisesCharacteristicCount"] = premisesCharacteristics.count()
      }
    }

    when {
      roomCharacteristics.isEmpty() -> {
        query = query.replace("#ROOM_CHARACTERISTICS_FILTER#", "")
      }
      else -> {
        query = query.replace("#ROOM_CHARACTERISTICS_FILTER#", ROOM_CHARACTERISTICS_FILTER)
        params["roomCharacteristics"] = roomCharacteristics
        params["roomCharacteristicCount"] = roomCharacteristics.count()
      }
    }

    return query to params
  }

  fun getSpaceAvailabilityForCandidatePremises(
    premisesIds: List<UUID>,
    startDate: LocalDate,
    durationInDays: Int,
  ): List<SpaceAvailability> = jdbcTemplate.query(
    SPACE_AVAILABILITY_QUERY,
    mapOf<String, Any>(
      "premisesIds" to premisesIds,
      "startDate" to startDate,
      "duration" to durationInDays,
    ),
  ) { rs, _ ->
    SpaceAvailability(
      rs.getUUID("premises_id"),
    )
  }

  private fun ResultSet.getUUID(columnLabel: String) = UUID.fromString(this.getString(columnLabel))

  private fun List<ApprovedPremisesType>.transformForQuery() = this.map {
    when (it) {
      ApprovedPremisesType.MHAP_ST_JOSEPHS, ApprovedPremisesType.MHAP_ELLIOTT_HOUSE -> "MHAP"
      else -> it.name
    }
  }
}

data class CandidatePremises(
  val premisesId: UUID,
  val distanceInMiles: Float,
  val apCode: String,
  val deliusQCode: String,
  val apType: ApprovedPremisesType,
  val name: String,
  val addressLine1: String,
  val addressLine2: String?,
  val town: String?,
  val postcode: String,
  val apAreaId: UUID,
  val apAreaName: String,
  val totalSpaceCount: Int,
)

data class SpaceAvailability(
  val premisesId: UUID,
)
