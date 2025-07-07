package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.time.OffsetDateTime

fun IntegrationTestBase.givenAnOutOfServiceBedWithMultipleRevisions(
  bed: BedEntity,
  revisions: List<OutOfServiceBedRevision>,
): Cas1OutOfServiceBedEntity {
  val outOfServiceBed = cas1OutOfServiceBedEntityFactory.produceAndPersist {
    withCreatedAt(OffsetDateTime.now())
    withBed(bed)
  }

  revisions.forEach { rev ->
    outOfServiceBed.revisionHistory += cas1OutOfServiceBedRevisionEntityFactory.produceAndPersist {
      withCreatedAt(rev.createdAt)
      withCreatedBy(givenAUser().first)
      withOutOfServiceBed(outOfServiceBed)
      withStartDate(rev.startDate)
      withEndDate(rev.endDate)
      withReason(
        cas1OutOfServiceBedReasonEntityFactory.produceAndPersist {
          withName(rev.reason)
        },
      )
    }
  }

  return outOfServiceBed
}

fun IntegrationTestBase.givenAnOutOfServiceBed(
  bed: BedEntity,
  startDate: LocalDate = LocalDate.now(),
  endDate: LocalDate = LocalDate.now(),
  cancelled: Boolean = false,
  reason: String = randomStringMultiCaseWithNumbers(6),
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
      withReason(
        cas1OutOfServiceBedReasonEntityFactory.produceAndPersist {
          withName(reason)
        },
      )
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

data class OutOfServiceBedRevision(
  val createdAt: OffsetDateTime,
  val startDate: LocalDate,
  val endDate: LocalDate,
  val reason: String = randomStringMultiCaseWithNumbers(6),
)
