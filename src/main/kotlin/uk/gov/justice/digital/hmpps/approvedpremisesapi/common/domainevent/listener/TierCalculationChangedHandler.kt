package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.domainevent.listener

import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service.CaseService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.HmppsDomainEvent

@Component
class TierCalculationChangedHandler(
  val jsonMapper: JsonMapper,
  val caseService: CaseService,
) : InboxEventHandler {

  companion object {
    const val TIER_CALCULATION_EVENT_TYPE = "tier.calculation.changed"
  }

  override fun supportedEventType(): String = TIER_CALCULATION_EVENT_TYPE

  override fun getPartitionKey(inboxEvent: InboxEventHandler.InboxEvent) = inboxEvent.extractCrn()

  override fun handle(inboxEvent: InboxEventHandler.InboxEvent): InboxEventHandler.Result {
    val crn = inboxEvent.extractCrn()

    return when (caseService.reviseTier(crn!!)) {
      true -> InboxEventHandler.Result.PROCESSED
      false -> InboxEventHandler.Result.IGNORED
    }
  }

  private fun InboxEventHandler.InboxEvent.extractCrn() = jsonMapper.readValue<HmppsDomainEvent>(this.payload).personReference.findCrn()
}
