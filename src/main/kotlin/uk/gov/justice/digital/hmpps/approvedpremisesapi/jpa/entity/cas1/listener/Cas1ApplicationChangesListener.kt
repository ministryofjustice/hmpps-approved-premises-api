package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.listener

import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEventAdditionalInformation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEventPersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEventPersonReferenceCollection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ConfiguredDomainEventWorker
import java.time.OffsetDateTime

open class Cas1ApplicationChangesListener(
  private val host: String,
  private val domainEventWorker: ConfiguredDomainEventWorker,
) {

  protected fun publishCas1ApplicationStatusUpdated(approvedPremisesApplicationEntity: ApprovedPremisesApplicationEntity) {
    val detailUrl = "$host/cases/${approvedPremisesApplicationEntity.crn}/applications/suitable"
    domainEventWorker.emitEvent(
      SnsEvent(
        eventType = DomainEventType.CAS1_APPLICATION_STATUS_UPDATED.typeName,
        version = 1,
        description = DomainEventType.CAS1_APPLICATION_STATUS_UPDATED.typeDescription,
        detailUrl = detailUrl,
        occurredAt = OffsetDateTime.now(),
        additionalInformation = SnsEventAdditionalInformation(
          applicationId = approvedPremisesApplicationEntity.id,
        ),
        personReference = SnsEventPersonReferenceCollection(
          identifiers = listOfNotNull(
            SnsEventPersonReference("CRN", approvedPremisesApplicationEntity.crn),
            approvedPremisesApplicationEntity.nomsNumber?.let { SnsEventPersonReference("NOMS", it) },
          ),
        ),
      ),
      domainEventId = approvedPremisesApplicationEntity.id,
    )
  }
}
