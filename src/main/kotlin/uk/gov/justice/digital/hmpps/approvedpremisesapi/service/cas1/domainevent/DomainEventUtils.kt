package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.domainevent

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventBookingSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventTransferInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventTransferType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEventPayloadBookingSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEventTransferInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEventTransferType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NamedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.GetCas1DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.domainevent.DomainEventUtils.mapApprovedPremisesEntityToPremises

object DomainEventUtils {
  fun mapApprovedPremisesEntityToPremises(aPEntity: ApprovedPremisesEntity) = Premises(
    id = aPEntity.id,
    name = aPEntity.name,
    apCode = aPEntity.apCode,
    legacyApCode = aPEntity.qCode,
    localAuthorityAreaName = aPEntity.localAuthorityArea!!.name,
  )
}

fun Cas1SpaceBookingEntity.toEventBookingSummary() = EventBookingSummary(
  id = this.id,
  premises = mapApprovedPremisesEntityToPremises(this.premises),
  arrivalDate = this.canonicalArrivalDate,
  departureDate = this.canonicalDepartureDate,
)

fun EventBookingSummary.toTimelinePayloadSummary() = Cas1TimelineEventPayloadBookingSummary(
  bookingId = this.id,
  premises = NamedId(this.premises.id, this.premises.name),
  arrivalDate = this.arrivalDate,
  departureDate = this.departureDate,
)

fun Premises.toNamedId() = NamedId(
  id = this.id,
  name = this.name,
)

fun EventTransferInfo.toTimelineTransferInfo() = Cas1TimelineEventTransferInfo(
  type = when (type) {
    EventTransferType.PLANNED -> Cas1TimelineEventTransferType.PLANNED
    EventTransferType.EMERGENCY -> Cas1TimelineEventTransferType.EMERGENCY
  },
  booking = booking.toTimelinePayloadSummary(),
  changeRequestId = changeRequestId,
)

fun <T> GetCas1DomainEvent<T>?.describe(describe: (T) -> String?): String? = this?.let { describe(it.data) }
