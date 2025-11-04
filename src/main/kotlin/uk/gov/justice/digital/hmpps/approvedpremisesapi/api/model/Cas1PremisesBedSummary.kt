package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

/**
 *
 * @param id
 * @param roomName
 * @param bedName
 * @param characteristics
 */
data class Cas1PremisesBedSummary(

  val id: java.util.UUID,

  val roomName: kotlin.String,

  val bedName: kotlin.String,

  val characteristics: kotlin.collections.List<Cas1SpaceCharacteristic>,
)
