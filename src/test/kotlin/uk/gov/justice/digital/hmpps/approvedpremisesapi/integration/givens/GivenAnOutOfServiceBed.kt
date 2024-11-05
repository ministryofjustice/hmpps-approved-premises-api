package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedEntity
import java.time.LocalDate
import java.time.OffsetDateTime

fun IntegrationTestBase.givenAnOutOfServiceBed(
  bed: BedEntity,
  startDate: LocalDate = LocalDate.now(),
  endDate: LocalDate = LocalDate.now(),
  cancelled: Boolean = false,
): Cas1OutOfServiceBedEntity {
  val outOfServiceBed = cas1OutOfServiceBedEntityFactory.produceAndPersist {
    withCreatedAt(OffsetDateTime.now())
    withBed(bed)
  }

  outOfServiceBed.apply {
    this.revisionHistory += cas1OutOfServiceBedRevisionEntityFactory.produceAndPersist {
      withCreatedBy(givenAUser().first)
      withOutOfServiceBed(this@apply)
      withStartDate(startDate)
      withEndDate(endDate)
      withReason(cas1OutOfServiceBedReasonEntityFactory.produceAndPersist())
    }
  }

  if (cancelled) {
    val cancellation = cas1OutOfServiceBedCancellationEntityFactory.produceAndPersist {
      withOutOfServiceBed(outOfServiceBed)
    }
    outOfServiceBed.cancellation = cancellation
    cas1OutOfServiceBedTestRepository.save(outOfServiceBed)
  }

  return outOfServiceBed
}
