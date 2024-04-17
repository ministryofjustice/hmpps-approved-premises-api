package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ActiveOffence
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.Offence

@Component
class ActiveOffenceTransformer {
  fun transformToApi(activeOffence: Offence) =
    ActiveOffence(
      deliusEventNumber = activeOffence.eventNumber,
      offenceDescription = activeOffence.description,
      offenceId = null,
      convictionId = null,
      offenceDate = activeOffence.date,
    )
}
