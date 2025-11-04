package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

/**
 *
 */
class UpdateTemporaryAccommodationApplication(

  override val type: UpdateApplicationType,

  override val `data`: kotlin.collections.Map<kotlin.String, kotlin.Any>,
) : UpdateApplication
