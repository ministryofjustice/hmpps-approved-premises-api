package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

/**
 *
 * @param status
 * @param &#x60;value&#x60;
 */
data class FlagsEnvelope(

  val status: RiskEnvelopeStatus,

  val `value`: kotlin.collections.List<kotlin.String>? = null,
)
