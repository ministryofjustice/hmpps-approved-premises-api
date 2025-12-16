package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.repository

import org.springframework.jdbc.core.ResultSetExtractor
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonType
import java.sql.ResultSet
import java.time.LocalDate
import java.util.UUID

@Repository
class Cas3BedspaceSearchRepository(private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate) {

  private val temporaryAccommodationSearchQuery =
    """
    WITH BookedBedspaces AS
        (SELECT distinct b.bed_id as bedspace_id, b.premises_id 
             FROM bookings b
             INNER JOIN cas3_premises p ON b.premises_id = p.id
             INNER JOIN probation_delivery_units pdu on pdu.id = p.probation_delivery_unit_id
             LEFT JOIN cancellations bc ON bc.booking_id = b.id
         WHERE pdu.probation_region_id = :probation_region_id
             AND (b.arrival_date, b.departure_date) OVERLAPS (:start_date, :end_date)
             AND bc.id IS NULL)
                             
    SELECT p.id as premises_id,
           p.name as premises_name,
           p.address_line1 as premises_address_line1,
           p.address_line2 as premises_address_line2,
           p.town as premises_town,
           p.postcode as premises_postcode,
           p.notes as premises_notes,
           pc.name as premises_characteristic_name,
           pc.description as premises_characteristic_description,
           bc.name as bedspace_characteristic_name,
           bc.description as bedspace_characteristic_description,
           (
            SELECT count(1)
            FROM cas3_bedspaces b2
            WHERE b2.premises_id = p.id
            AND ( b2.end_date IS NULL OR b2.end_date > :end_date)
           ) as premises_bedspace_count,
           (SELECT count(bedspace_id) FROM BookedBedspaces bb WHERE bb.premises_id = p.id) as booked_bedspace_count,
           b.id as bedspace_id,
           b.reference as bedspace_reference,
           pdu.name as probation_delivery_unit_name
    FROM cas3_premises p  
    LEFT JOIN cas3_premises_characteristic_assignments pca ON p.id = pca.premises_id
    LEFT JOIN cas3_premises_characteristics pc ON pca.premises_characteristics_id = pc.id
    LEFT JOIN cas3_bedspaces b ON b.premises_id = p.id
    LEFT JOIN cas3_bedspace_characteristic_assignments bca on bca.bedspace_id = b.id
    LEFT JOIN cas3_bedspace_characteristics bc ON bca.bedspace_characteristics_id = bc.id
    LEFT JOIN probation_delivery_units pdu on pdu.id = p.probation_delivery_unit_id
    WHERE
        NOT EXISTS (SELECT bb.bedspace_id FROM BookedBedspaces bb
         WHERE bb.bedspace_id = b.id
        ) AND
        NOT EXISTS (
         SELECT void_bedspace.bedspace_id FROM cas3_void_bedspaces void_bedspace
         WHERE
             void_bedspace.bedspace_id = b.id AND
             (void_bedspace.start_date, void_bedspace.end_date) OVERLAPS (:start_date, :end_date) AND
             void_bedspace.cancellation_date IS NULL
        ) AND 
        pdu.id IN (:probation_delivery_unit_ids) AND
        pdu.probation_region_id = :probation_region_id AND 
        (p.end_date IS NULL OR p.end_date > :start_date) AND 
        (b.start_date <= :start_date AND (b.end_date IS NULL OR b.end_date > :end_date))
        ORDER BY pdu.name, p.name, b.reference;
"""

  fun searchBedspaces(
    probationDeliveryUnits: List<UUID>,
    startDate: LocalDate,
    endDate: LocalDate,
    probationRegionId: UUID,
  ): List<Cas3v2CandidateBedspace> {
    val params = MapSqlParameterSource().apply {
      addValue("probation_region_id", probationRegionId)
      addValue("probation_delivery_unit_ids", probationDeliveryUnits)
      addValue("start_date", startDate)
      addValue("end_date", endDate)
    }

    val result = namedParameterJdbcTemplate.query(
      temporaryAccommodationSearchQuery,
      params,
      ResultSetExtractor { resultSet ->
        val bedspaces = mutableMapOf<UUID, Cas3v2CandidateBedspace>()

        while (resultSet.next()) {
          val premisesId = UUID.fromString(resultSet.getString("premises_id"))
          val premisesName = resultSet.getString("premises_name")
          val premisesAddressLine1 = resultSet.getString("premises_address_line1")
          val premisesAddressLine2 = resultSet.getString("premises_address_line2")
          val premisesTown = resultSet.getString("premises_town")
          val premisesPostcode = resultSet.getString("premises_postcode")
          val premisesNotes = resultSet.getString("premises_notes")

          val premisesCharacteristicName = resultSet.getString("premises_characteristic_name")
          val premisesCharacteristicDescription = resultSet.getString("premises_characteristic_description")
          val bedspaceCharacteristicName = resultSet.getString("bedspace_characteristic_name")
          val bedspaceCharacteristicDescription = resultSet.getString("bedspace_characteristic_description")

          val premisesBedspaceCount = resultSet.getInt("premises_bedspace_count")
          val bookedBedspaceCount = resultSet.getInt("booked_bedspace_count")
          val bedspaceId = resultSet.getNullableUUID("bedspace_id")
          val bedspaceReference = resultSet.getString("bedspace_reference")
          val probationDeliveryUnitName = resultSet.getString("probation_delivery_unit_name")

          if (bedspaceId == null) continue

          if (!bedspaces.containsKey(bedspaceId)) {
            bedspaces[bedspaceId] = Cas3v2CandidateBedspace(
              premisesId = premisesId,
              premisesName = premisesName,
              premisesAddressLine1 = premisesAddressLine1,
              premisesAddressLine2 = premisesAddressLine2,
              premisesTown = premisesTown,
              premisesPostcode = premisesPostcode,
              premisesNotes = premisesNotes,
              probationDeliveryUnitName = probationDeliveryUnitName,
              premisesCharacteristics = mutableListOf(),
              premisesBedspaceCount = premisesBedspaceCount,
              bookedBedspaceCount = bookedBedspaceCount,
              bedspaceId = bedspaceId,
              bedspaceReference = bedspaceReference,
              bedspaceCharacteristics = mutableListOf(),
              overlaps = mutableListOf(),
            )
          }

          bedspaces[bedspaceId]!!.apply {
            if (premisesCharacteristicDescription != null) {
              premisesCharacteristics.addIfNoneMatch(Cas3CharacteristicNames(premisesCharacteristicName, premisesCharacteristicDescription)) {
                it.description == premisesCharacteristicDescription
              }
            }

            if (bedspaceCharacteristicName != null) {
              bedspaceCharacteristics.addIfNoneMatch(Cas3CharacteristicNames(bedspaceCharacteristicName, bedspaceCharacteristicDescription)) {
                it.description == bedspaceCharacteristicDescription
              }
            }
          }
        }

        bedspaces.values.toList()
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

@SuppressWarnings("LongParameterList")
class Cas3v2CandidateBedspace(
  val premisesId: UUID,
  val premisesName: String,
  val premisesAddressLine1: String,
  val premisesAddressLine2: String?,
  val premisesTown: String?,
  val premisesPostcode: String,
  val premisesCharacteristics: MutableList<Cas3CharacteristicNames>,
  val premisesBedspaceCount: Int,
  val bedspaceId: UUID,
  val bedspaceReference: String,
  val bedspaceCharacteristics: MutableList<Cas3CharacteristicNames>,
  val probationDeliveryUnitName: String,
  val premisesNotes: String?,
  val bookedBedspaceCount: Int,
  val overlaps: MutableList<Cas3v2CandidateBedspaceOverlap>,
)

data class Cas3CharacteristicNames(
  val name: String,
  val description: String,
)

data class Cas3v2CandidateBedspaceOverlap(
  val name: String,
  val crn: String,
  val personType: PersonType,
  val sex: String?,
  val days: Int,
  val premisesId: UUID,
  val bedspaceId: UUID,
  val bookingId: UUID,
  val assessmentId: UUID?,
  val isSexualRisk: Boolean,
)
