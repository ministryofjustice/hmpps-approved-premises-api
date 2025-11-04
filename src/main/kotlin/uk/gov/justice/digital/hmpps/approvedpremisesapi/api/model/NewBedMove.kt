package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

/**
 *
 * @param bedId
 * @param notes
 */
data class NewBedMove(

  val bedId: java.util.UUID,

  val notes: kotlin.String? = null,
)
