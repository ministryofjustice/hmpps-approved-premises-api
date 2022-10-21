package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonNeed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonNeeds
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.assessrisksandneeds.Need
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.assessrisksandneeds.NeedSeverity

@Component
class NeedsTransformer {
  fun transformToApi(needs: List<Need>): PersonNeeds {
    val linkedToRiskOfSeriousHarm = needs.filter { it.riskOfHarm!! && it.severity == NeedSeverity.SEVERE }
    val linkedToReoffending = needs.subtract(linkedToRiskOfSeriousHarm).filter { it.riskOfReoffending!! }
    val notLinkedToSeriousHarmOrReoffending = needs.subtract(linkedToRiskOfSeriousHarm).subtract(linkedToReoffending)

    return PersonNeeds(
      linkedToRiskOfSeriousHarm = linkedToRiskOfSeriousHarm.map(::transformToApi),
      linkedToReoffending = linkedToReoffending.map(::transformToApi),
      notLinkedToSeriousHarmOrReoffending = notLinkedToSeriousHarmOrReoffending.map(::transformToApi)
    )
  }

  private fun transformToApi(need: Need) = PersonNeed(
    section = need.section!!,
    name = need.name!!,
    overThreshold = need.overThreshold!!,
    riskOfHarm = need.riskOfHarm!!,
    flaggedAsNeed = need.flaggedAsNeed!!,
    severity = need.severity.toString(),
    identifiedAsNeed = need.identifiedAsNeed!!,
    needScore = need.needScore!!.toInt(),
    riskOfReoffending = need.riskOfReoffending!!
  )
}
