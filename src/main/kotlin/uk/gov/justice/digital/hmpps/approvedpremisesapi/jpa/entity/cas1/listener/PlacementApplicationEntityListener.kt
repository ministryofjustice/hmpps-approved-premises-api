package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.listener

import jakarta.persistence.PostPersist
import jakarta.persistence.PostUpdate
import org.springframework.beans.factory.annotation.Value
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ConfiguredDomainEventWorker

class PlacementApplicationEntityListener(
  @Value($$"${ingress.host}") val ingressHost: String,
  domainEventWorker: ConfiguredDomainEventWorker,
) : Cas1ApplicationChangesListener(host = ingressHost, domainEventWorker) {

  @PostPersist
  fun onPostPersist(entity: PlacementApplicationEntity) {
    publishCas1ApplicationStatusUpdated(entity.application)
  }

  @PostUpdate
  fun onPostUpdate(entity: PlacementApplicationEntity) {
    publishCas1ApplicationStatusUpdated(entity.application)
  }
}
