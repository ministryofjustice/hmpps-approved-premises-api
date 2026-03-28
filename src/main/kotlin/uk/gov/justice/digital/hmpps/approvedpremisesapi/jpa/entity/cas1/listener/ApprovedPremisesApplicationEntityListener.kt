package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.listener

import jakarta.persistence.PostPersist
import jakarta.persistence.PostUpdate
import org.springframework.beans.factory.annotation.Value
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ConfiguredDomainEventWorker

class ApprovedPremisesApplicationEntityListener(
  @Value($$"${ingress.host}") val ingressHost: String,
  domainEventWorker: ConfiguredDomainEventWorker,
) : Cas1ApplicationChangesListener(host = ingressHost, domainEventWorker) {
  @PostPersist
  fun onPostPersist(entity: ApprovedPremisesApplicationEntity) {
    publishCas1ApplicationStatusUpdated(entity)
  }

  @PostUpdate
  fun onPostUpdate(entity: ApprovedPremisesApplicationEntity) {
    if (entity.isStatusChanged()) {
      publishCas1ApplicationStatusUpdated(entity)
    }
  }
}
