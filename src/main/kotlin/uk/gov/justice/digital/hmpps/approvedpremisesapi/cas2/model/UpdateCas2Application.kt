package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApplicationType

/**
 *
 */
class UpdateCas2Application(

  override val type: UpdateApplicationType,

  override val `data`: Map<String, Any>,
) : UpdateApplication
