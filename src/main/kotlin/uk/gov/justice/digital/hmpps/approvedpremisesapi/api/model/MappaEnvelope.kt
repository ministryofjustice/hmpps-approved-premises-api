package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

/**
 *
 * @param status
 * @param &#x60;value&#x60;
 */
data class MappaEnvelope(

  val status: RiskEnvelopeStatus,

  val `value`: Mappa? = null,
)
