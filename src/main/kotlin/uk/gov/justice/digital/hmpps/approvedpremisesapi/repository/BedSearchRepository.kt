package uk.gov.justice.digital.hmpps.approvedpremisesapi.repository

import org.springframework.jdbc.core.ResultSetExtractor
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.LocalDate
import java.util.UUID

@Repository
class BedSearchRepository(private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate) {
  private val approvedPremisesSearchQuery =
    """
SELECT ST_Distance((SELECT point FROM postcode_districts pd WHERE pd.outcode = :outcode)::geography, ap.point::geography) * 0.000621371 as distance_miles,
       p.id as premises_id,
       p.name as premises_name,
       p.address_line1 as premises_address_line1,
       p.address_line2 as premises_address_line2,
       p.town as premises_town,
       p.postcode as premises_postcode,
       c.property_name as premises_characteristic_property_name,
       (SELECT count(1) FROM beds b2 WHERE b2.room_id IN (SELECT id FROM rooms r2 WHERE r2.premises_id = p.id)) as premises_bed_count,
       c.name as premises_characteristic_name,
       r.id as room_id,
       r.name as room_name,
       c2.property_name as room_characteristic_property_name,
       c2.name as room_characteristic_name,
       b.id as bed_id,
       b.name as bed_name
FROM premises p 
JOIN approved_premises ap ON p.id = ap.premises_id
LEFT JOIN premises_characteristics pc ON p.id = pc.premises_id
LEFT JOIN characteristics c ON pc.characteristic_id = c.id
LEFT JOIN rooms r ON r.premises_id = p.id
LEFT JOIN room_characteristics rc on rc.room_id = r.id
LEFT JOIN characteristics c2 ON rc.characteristic_id = c2.id
LEFT JOIN beds b ON b.room_id = r.id
WHERE
    ST_DWithin((SELECT point FROM postcode_districts pd WHERE pd.outcode = :outcode)::geography, ap.point::geography, (:max_miles + 1) / 0.000621371) AND --miles to meters
    #OPTIONAL_FILTERS
    (SELECT COUNT(1) FROM bookings books
         LEFT JOIN cancellations books_cancel ON books_cancel.booking_id = books.id
         LEFT JOIN non_arrivals books_non_arrival ON books_non_arrival.booking_id = books.id
     WHERE
         books.bed_id = b.id AND
         (books.arrival_date, books.departure_date) OVERLAPS (:start_date, :end_date) AND
         books_cancel.id IS NULL
         AND books_non_arrival.id IS NULL
     ) = 0 AND
    (SELECT COUNT(1) FROM lost_beds lostbeds
         LEFT JOIN lost_bed_cancellations lostbeds_cancel ON lostbeds_cancel.lost_bed_id = lostbeds.id
     WHERE
         lostbeds.bed_id = b.id AND
         (lostbeds.start_date, lostbeds.end_date) OVERLAPS (:start_date, :end_date) AND
         lostbeds_cancel.id IS NULL
     ) = 0 AND 
    p.status = 'active' AND 
    p.service = 'approved-premises' 
ORDER BY distance_miles;
"""

  private val premisesCharacteristicFilter = """
    (SELECT COUNT(1) FROM premises_characteristics pc_filter WHERE pc_filter.characteristic_id IN (:premises_characteristic_ids) AND pc_filter.premises_id = p.id) = :premises_characteristic_ids_count
"""

  private val roomCharacteristicFilter = """
    (SELECT COUNT(1) FROM room_characteristics rc_filter WHERE rc_filter.characteristic_id IN (:room_characteristic_ids) AND rc_filter.room_id = r.id) = :room_characteristic_ids_count
"""

  fun findApprovedPremisesBeds(
    postcodeDistrictOutcode: String,
    maxDistanceMiles: Int,
    startDate: LocalDate,
    durationInDays: Int,
    requiredPremisesCharacteristics: List<UUID>,
    requiredRoomCharacteristics: List<UUID>,
  ): List<ApprovedPremisesBedSearchResult> {
    val params = MapSqlParameterSource().apply {
      addValue("outcode", postcodeDistrictOutcode)
      addValue("max_miles", maxDistanceMiles)
      addValue("premises_characteristic_ids", requiredPremisesCharacteristics)
      addValue("premises_characteristic_ids_count", requiredPremisesCharacteristics.size)
      addValue("room_characteristic_ids", requiredRoomCharacteristics)
      addValue("room_characteristic_ids_count", requiredRoomCharacteristics.size)
      addValue("start_date", startDate)
      addValue("end_date", startDate.plusDays(durationInDays.toLong()))
    }

    var optionalFilters = ""
    if (requiredPremisesCharacteristics.any()) {
      optionalFilters += "$premisesCharacteristicFilter AND\n"
    }

    if (requiredRoomCharacteristics.any()) {
      optionalFilters += "$roomCharacteristicFilter AND\n"
    }

    val query = approvedPremisesSearchQuery.replace("#OPTIONAL_FILTERS", optionalFilters)

    val result = namedParameterJdbcTemplate.query(
      query,
      params,
      ResultSetExtractor { resultSet ->
        val beds = mutableMapOf<UUID, ApprovedPremisesBedSearchResult>()

        while (resultSet.next()) {
          val distanceMiles = resultSet.getDouble("distance_miles")
          val premisesId = UUID.fromString(resultSet.getString("premises_id"))
          val premisesName = resultSet.getString("premises_name")
          val premisesAddressLine1 = resultSet.getString("premises_address_line1")
          val premisesAddressLine2 = resultSet.getString("premises_address_line2")
          val premisesTown = resultSet.getString("premises_town")
          val premisesPostcode = resultSet.getString("premises_postcode")
          val premisesCharacteristicName = resultSet.getString("premises_characteristic_name")
          val premisesCharacteristicPropertyName = resultSet.getString("premises_characteristic_property_name")
          val premisesBedCount = resultSet.getInt("premises_bed_count")
          val roomId = resultSet.getNullableUUID("room_id")
          val roomName = resultSet.getString("room_name")
          val roomCharacteristicName = resultSet.getString("room_characteristic_name")
          val roomCharacteristicPropertyName = resultSet.getString("room_characteristic_property_name")
          val bedId = resultSet.getNullableUUID("bed_id")
          val bedName = resultSet.getString("bed_name")

          if (bedId == null) continue

          if (!beds.containsKey(bedId)) {
            beds[bedId] = ApprovedPremisesBedSearchResult(
              premisesId = premisesId,
              premisesName = premisesName,
              premisesAddressLine1 = premisesAddressLine1,
              premisesAddressLine2 = premisesAddressLine2,
              premisesTown = premisesTown,
              premisesPostcode = premisesPostcode,
              premisesCharacteristics = mutableListOf(),
              premisesBedCount = premisesBedCount,
              bedId = bedId,
              bedName = bedName,
              roomId = roomId!!,
              roomName = roomName,
              roomCharacteristics = mutableListOf(),
              distance = distanceMiles,
            )
          }

          beds[bedId]!!.apply {
            if (premisesCharacteristicName != null) {
              premisesCharacteristics.addIfNoneMatch(CharacteristicNames(premisesCharacteristicPropertyName, premisesCharacteristicName)) {
                it.name == premisesCharacteristicName
              }
            }

            if (roomCharacteristicName != null) {
              roomCharacteristics.addIfNoneMatch(CharacteristicNames(roomCharacteristicPropertyName, roomCharacteristicName)) {
                it.name == roomCharacteristicName
              }
            }
          }
        }

        beds.values.toList()
      },
    )

    return result ?: emptyList()
  }

  /*
    The FilteredPremisesIds is to get premises Ids that match the search property attributes criteria
    In the where condition is checking (FALSE = :filter_by_shared_property AND FALSE = :filter_by_single_occupancy)
    another time to include the properties that don't have property attributes
  */
  private val temporaryAccommodationSearchQuery =
    """
    WITH FilteredPremisesIds AS
        (SELECT distinct pc2.premises_id
             FROM premises_characteristics pc2
             INNER JOIN characteristics c2 on pc2.characteristic_id = c2.id
             INNER JOIN premises p2 ON pc2.premises_id = p2.id
             WHERE service_scope = 'temporary-accommodation'
                      AND p2.probation_region_id = :probation_region_id 
                      AND (c2.is_active = true AND c2.name = 'Single occupancy' OR FALSE = :filter_by_single_occupancy)
                      AND (c2.is_active = true AND c2.name = 'Shared property' OR FALSE = :filter_by_shared_property))
                             
    SELECT p.id as premises_id,
           p.name as premises_name,
           p.address_line1 as premises_address_line1,
           p.address_line2 as premises_address_line2,
           p.town as premises_town,
           p.postcode as premises_postcode,
           c.property_name as premises_characteristic_property_name,
           c.name as premises_characteristic_name,
           r.id as room_id,
           r.name as room_name,
           c2.property_name as room_characteristic_property_name,
           (SELECT count(1) FROM beds b2 WHERE b2.room_id IN (SELECT id FROM rooms r2 WHERE r2.premises_id = p.id) AND ( b2.end_date IS NULL OR b2.end_date > :end_date ) ) as premises_bed_count,
           c2.name as room_characteristic_name,
           b.id as bed_id,
           b.name as bed_name,
           pdu.name as probation_delivery_unit_name
    FROM premises p  
    JOIN temporary_accommodation_premises tap ON tap.premises_id = p.id
    LEFT JOIN premises_characteristics pc ON p.id = pc.premises_id
    LEFT JOIN characteristics c ON pc.characteristic_id = c.id
    LEFT JOIN rooms r ON r.premises_id = p.id
    LEFT JOIN room_characteristics rc on rc.room_id = r.id
    LEFT JOIN characteristics c2 ON rc.characteristic_id = c2.id
    LEFT JOIN beds b ON b.room_id = r.id
    LEFT JOIN probation_delivery_units pdu on pdu.id = tap.probation_delivery_unit_id
    WHERE
        NOT EXISTS (SELECT books.bed_id FROM bookings books
             LEFT JOIN cancellations books_cancel ON books_cancel.booking_id = books.id
         WHERE
             books.bed_id = b.id AND
             (books.arrival_date, books.departure_date) OVERLAPS (:start_date, :end_date) AND
             books_cancel.id IS NULL
        ) AND
        NOT EXISTS (SELECT lostbeds.bed_id FROM lost_beds lostbeds
             LEFT JOIN lost_bed_cancellations lostbeds_cancel ON lostbeds_cancel.lost_bed_id = lostbeds.id
         WHERE
             lostbeds.bed_id = b.id AND
             (lostbeds.start_date, lostbeds.end_date) OVERLAPS (:start_date, :end_date) AND
             lostbeds_cancel.id IS NULL
        ) AND 
        (p.id in (SELECT premises_id FROM FilteredPremisesIds) OR (FALSE = :filter_by_shared_property AND FALSE = :filter_by_single_occupancy)) AND
        pdu.id IN (:probation_delivery_unit_ids) AND
        p.probation_region_id = :probation_region_id AND 
        p.status = 'active' AND 
        p.service = 'temporary-accommodation' AND
        (b.end_date IS NULL OR b.end_date > :end_date);
"""

  fun findTemporaryAccommodationBeds(
    probationDeliveryUnits: List<UUID>,
    startDate: LocalDate,
    endDate: LocalDate,
    probationRegionId: UUID,
    filterBySharedProperty: Boolean,
    filterBySingleOccupancy: Boolean,
  ): List<TemporaryAccommodationBedSearchResult> {
    val params = MapSqlParameterSource().apply {
      addValue("probation_region_id", probationRegionId)
      addValue("probation_delivery_unit_ids", probationDeliveryUnits)
      addValue("start_date", startDate)
      addValue("end_date", endDate)
      addValue("filter_by_shared_property", filterBySharedProperty)
      addValue("filter_by_single_occupancy", filterBySingleOccupancy)
    }

    val result = namedParameterJdbcTemplate.query(
      temporaryAccommodationSearchQuery,
      params,
      ResultSetExtractor { resultSet ->
        val beds = mutableMapOf<UUID, TemporaryAccommodationBedSearchResult>()

        while (resultSet.next()) {
          val premisesId = UUID.fromString(resultSet.getString("premises_id"))
          val premisesName = resultSet.getString("premises_name")
          val premisesAddressLine1 = resultSet.getString("premises_address_line1")
          val premisesAddressLine2 = resultSet.getString("premises_address_line2")
          val premisesTown = resultSet.getString("premises_town")
          val premisesPostcode = resultSet.getString("premises_postcode")
          val premisesCharacteristicName = resultSet.getString("premises_characteristic_name")
          val premisesCharacteristicPropertyName = resultSet.getString("premises_characteristic_property_name")
          val premisesBedCount = resultSet.getInt("premises_bed_count")
          val roomId = resultSet.getNullableUUID("room_id")
          val roomName = resultSet.getString("room_name")
          val roomCharacteristicName = resultSet.getString("room_characteristic_name")
          val roomCharacteristicPropertyName = resultSet.getString("room_characteristic_property_name")
          val bedId = resultSet.getNullableUUID("bed_id")
          val bedName = resultSet.getString("bed_name")
          val probationDeliveryUnitName = resultSet.getString("probation_delivery_unit_name")

          if (bedId == null) continue

          if (!beds.containsKey(bedId)) {
            beds[bedId] = TemporaryAccommodationBedSearchResult(
              premisesId = premisesId,
              premisesName = premisesName,
              premisesAddressLine1 = premisesAddressLine1,
              premisesAddressLine2 = premisesAddressLine2,
              premisesTown = premisesTown,
              premisesPostcode = premisesPostcode,
              probationDeliveryUnitName = probationDeliveryUnitName,
              premisesCharacteristics = mutableListOf(),
              premisesBedCount = premisesBedCount,
              bedId = bedId,
              bedName = bedName,
              roomId = roomId!!,
              roomName = roomName,
              roomCharacteristics = mutableListOf(),
              overlaps = mutableListOf(),
            )
          }

          beds[bedId]!!.apply {
            if (premisesCharacteristicName != null) {
              premisesCharacteristics.addIfNoneMatch(CharacteristicNames(premisesCharacteristicPropertyName, premisesCharacteristicName)) {
                it.name == premisesCharacteristicName
              }
            }

            if (roomCharacteristicName != null) {
              roomCharacteristics.addIfNoneMatch(CharacteristicNames(roomCharacteristicPropertyName, roomCharacteristicName)) {
                it.name == roomCharacteristicName
              }
            }
          }
        }

        beds.values.toList()
      },
    )

    return result ?: emptyList()
  }
}

private fun ResultSet.getNullableUUID(columnName: String): UUID? {
  val stringValue = this.getString(columnName) ?: return null

  return UUID.fromString(stringValue)
}

private inline fun <reified T> MutableList<T>.addIfNoneMatch(entry: T, matcher: (T) -> Boolean) {
  if (this.any { matcher(it) }) return

  this.add(entry)
}

sealed class BedSearchResult(
  val premisesId: UUID,
  val premisesName: String,
  val premisesAddressLine1: String,
  val premisesAddressLine2: String?,
  val premisesTown: String?,
  val premisesPostcode: String,
  val premisesCharacteristics: MutableList<CharacteristicNames>,
  val premisesBedCount: Int,
  val roomId: UUID,
  val roomName: String,
  val bedId: UUID,
  val bedName: String,
  val roomCharacteristics: MutableList<CharacteristicNames>,
)

class ApprovedPremisesBedSearchResult(
  premisesId: UUID,
  premisesName: String,
  premisesAddressLine1: String,
  premisesAddressLine2: String?,
  premisesTown: String?,
  premisesPostcode: String,
  premisesCharacteristics: MutableList<CharacteristicNames>,
  premisesBedCount: Int,
  roomId: UUID,
  roomName: String,
  bedId: UUID,
  bedName: String,
  roomCharacteristics: MutableList<CharacteristicNames>,
  val distance: Double,
) : BedSearchResult(
  premisesId,
  premisesName,
  premisesAddressLine1,
  premisesAddressLine2,
  premisesTown,
  premisesPostcode,
  premisesCharacteristics,
  premisesBedCount,
  roomId,
  roomName,
  bedId,
  bedName,
  roomCharacteristics,
)

@SuppressWarnings("LongParameterList")
class TemporaryAccommodationBedSearchResult(
  premisesId: UUID,
  premisesName: String,
  premisesAddressLine1: String,
  premisesAddressLine2: String?,
  premisesTown: String?,
  premisesPostcode: String,
  premisesCharacteristics: MutableList<CharacteristicNames>,
  premisesBedCount: Int,
  roomId: UUID,
  roomName: String,
  bedId: UUID,
  bedName: String,
  roomCharacteristics: MutableList<CharacteristicNames>,
  val probationDeliveryUnitName: String,
  val overlaps: MutableList<TemporaryAccommodationBedSearchResultOverlap>,
) : BedSearchResult(
  premisesId,
  premisesName,
  premisesAddressLine1,
  premisesAddressLine2,
  premisesTown,
  premisesPostcode,
  premisesCharacteristics,
  premisesBedCount,
  roomId,
  roomName,
  bedId,
  bedName,
  roomCharacteristics,
)

data class CharacteristicNames(
  val propertyName: String?,
  val name: String,
)

data class TemporaryAccommodationBedSearchResultOverlap(
  val crn: String,
  val days: Int,
  val premisesId: UUID,
  val roomId: UUID,
  val bookingId: UUID,
)
