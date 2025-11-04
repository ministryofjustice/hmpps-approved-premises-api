package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

/**
 *
 * @param alertId
 * @param dateCreated
 * @param comment
 * @param description
 * @param dateExpires
 * @param alertTypeDescription
 */
data class PersonAcctAlert(

  val alertId: kotlin.Long,

  val dateCreated: java.time.LocalDate,

  val comment: kotlin.String? = null,

  val description: kotlin.String? = null,

  val dateExpires: java.time.LocalDate? = null,

  val alertTypeDescription: kotlin.String? = null,
)
