package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Offence
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.DeliusContextOffence
@Component
class OffenceTransformer {
  fun transformToApi(offence: DeliusContextOffence) = Offence(
    deliusEventNumber = offence.eventNumber,
    description = offence.description,
    active = true,
    main = offence.main,
    offenceDate = offence.date,
    code = offence.code,
  )
}
