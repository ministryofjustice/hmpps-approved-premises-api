package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.time.LocalDate

data class UpdateCas2v2Application(

  override val type: UpdateApplicationType,

  override val `data`: Map<String, Any>,

  val bailHearingDate: LocalDate? = null,
) : UpdateApplication
