package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.jdbc.core.ResultSetExtractor
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.LocalDate
import java.util.UUID

@Repository
class BedSearchRepository(private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate) {
  private val searchQuery =
    """
  SELECT ST_Distance((SELECT point FROM postcode_districts pd WHERE pd.outcode = :outcode)::geography, p.point::geography) * 0.000621371 as distance_miles,
       p.id as premises_id,
       p.name as premises_name,
       c.property_name as premises_characteristic_name,
       r.id as room_id,
       r.name as room_name,
       c2.property_name as room_characteristic_name,
       b.id as bed_id,
       b.name as bed_name
FROM premises p
LEFT JOIN premises_characteristics pc ON p.id = pc.premises_id
LEFT JOIN characteristics c ON pc.characteristic_id = c.id
LEFT JOIN rooms r ON r.premises_id = p.id
LEFT JOIN room_characteristics rc on rc.room_id = r.id
LEFT JOIN characteristics c2 ON rc.characteristic_id = c2.id
LEFT JOIN beds b ON b.room_id = r.id
WHERE
    ST_DWithin((SELECT point FROM postcode_districts pd WHERE pd.outcode = :outcode)::geography, p.point::geography, (:max_miles + 1) / 0.000621371) AND --miles to meters
    (SELECT COUNT(1) FROM premises_characteristics pc_filter WHERE pc_filter.characteristic_id IN (:premises_characteristic_ids) AND pc_filter.premises_id = p.id) = :premises_characteristic_ids_count AND 
    (SELECT COUNT(1) FROM room_characteristics rc_filter WHERE rc_filter.characteristic_id IN (:room_characteristic_ids) AND rc_filter.room_id = r.id) = :room_characteristic_ids_count AND 
    (SELECT COUNT(1) FROM bookings books
         LEFT JOIN cancellations books_cancel ON books_cancel.booking_id = books.id
     WHERE
         books.bed_id = b.id AND
         (books.arrival_date, books.departure_date) OVERLAPS (:start_date, :end_date) AND
         books_cancel.id IS NULL
     ) = 0 AND
    (SELECT COUNT(1) FROM lost_beds lostbeds
         LEFT JOIN lost_bed_cancellations lostbeds_cancel ON lostbeds_cancel.lost_bed_id = lostbeds.id
     WHERE
         lostbeds.bed_id = b.id AND
         (lostbeds.start_date, lostbeds.end_date) OVERLAPS (:start_date, :end_date) AND
         lostbeds_cancel.id IS NULL
     ) = 0 AND
    p.service = :service
ORDER BY distance_miles;
"""

  fun findBeds(
    postcodeDistrictOutcode: String,
    maxDistanceMiles: Int,
    startDate: LocalDate,
    durationInDays: Int,
    requiredPremisesCharacteristics: List<UUID>,
    requiredRoomCharacteristics: List<UUID>,
    service: String
  ): List<BedSearchResult> {
    val params = MapSqlParameterSource().apply {
      addValue("outcode", postcodeDistrictOutcode)
      addValue("max_miles", maxDistanceMiles)
      addValue("premises_characteristic_ids", requiredPremisesCharacteristics)
      addValue("premises_characteristic_ids_count", requiredPremisesCharacteristics.size)
      addValue("room_characteristic_ids", requiredRoomCharacteristics)
      addValue("room_characteristic_ids_count", requiredRoomCharacteristics.size)
      addValue("start_date", startDate)
      addValue("end_date", startDate.plusDays(durationInDays.toLong()))
      addValue("service", service)
    }

    val result = namedParameterJdbcTemplate.query(
      searchQuery,
      params,
      ResultSetExtractor { resultSet ->
        val beds = mutableMapOf<UUID, BedSearchResult>()

        while (resultSet.next()) {
          val distanceMiles = resultSet.getDouble("distance_miles")
          val premisesId = UUID.fromString(resultSet.getString("premises_id"))
          val premisesName = resultSet.getString("premises_name")
          val premisesCharacteristicName = resultSet.getString("premises_characteristic_name")
          val roomId = resultSet.getNullableUUID("room_id")
          val roomName = resultSet.getString("room_name")
          val roomCharacteristicName = resultSet.getString("room_characteristic_name")
          val bedId = resultSet.getNullableUUID("bed_id")
          val bedName = resultSet.getString("bed_name")

          if (bedId == null) continue

          if (!beds.containsKey(bedId)) {
            beds[bedId] = BedSearchResult(
              premisesId = premisesId,
              premisesName = premisesName,
              premisesCharacteristicPropertyNames = mutableListOf(),
              bedId = bedId,
              bedName = bedName,
              roomId = roomId!!,
              roomName = roomName,
              roomCharacteristicPropertyNames = mutableListOf(),
              distance = distanceMiles
            )
          }

          beds[bedId]!!.apply {
            premisesCharacteristicPropertyNames.addIfNotNullAndNotDuplicate(premisesCharacteristicName)
            roomCharacteristicPropertyNames.addIfNotNullAndNotDuplicate(roomCharacteristicName)
          }
        }

        beds.values.toList()
      }
    )

    return result ?: emptyList()
  }
}

private fun ResultSet.getNullableUUID(columnName: String): UUID? {
  val stringValue = this.getString(columnName) ?: return null

  return UUID.fromString(stringValue)
}

private inline fun <reified T> MutableList<T>.addIfNotNullAndNotDuplicate(entry: T?) {
  if (entry == null) return

  if (this.contains(entry)) return

  this.add(entry)
}

data class BedSearchResult(
  val premisesId: UUID,
  val premisesName: String,
  val premisesCharacteristicPropertyNames: MutableList<String>,
  val roomId: UUID,
  val roomName: String,
  val bedId: UUID,
  val bedName: String,
  val roomCharacteristicPropertyNames: MutableList<String>,
  val distance: Double,
)
