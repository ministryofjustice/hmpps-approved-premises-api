package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.domainevent

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventBookingSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NamedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1TimelineEventPayloadBookingSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.GetCas1DomainEvent

object DomainEventUtils {
  fun mapApprovedPremisesEntityToPremises(aPEntity: ApprovedPremisesEntity) = Premises(
    id = aPEntity.id,
    name = aPEntity.name,
    apCode = aPEntity.apCode,
    legacyApCode = aPEntity.qCode,
    localAuthorityAreaName = aPEntity.localAuthorityArea!!.name,
  )
}

fun Premises.toNamedId() = NamedId(
  id = this.id,
  name = this.name,
)

fun EventBookingSummary.toTimelinePayloadSummary() = Cas1TimelineEventPayloadBookingSummary(
  bookingId = this.id,
  premises = NamedId(this.premises.id, this.premises.name),
  arrivalDate = this.arrivalDate,
  departureDate = this.departureDate,
)

fun <T> GetCas1DomainEvent<T>?.describe(describe: (T) -> String?): String? = this?.let { describe(it.data) }
