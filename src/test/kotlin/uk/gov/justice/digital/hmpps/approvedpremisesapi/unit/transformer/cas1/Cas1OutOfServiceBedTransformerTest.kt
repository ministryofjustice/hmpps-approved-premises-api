package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas1

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OutOfServiceBedCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OutOfServiceBedReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OutOfServiceBedRevision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OutOfServiceBedRevisionType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OutOfServiceBedStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Temporality
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1OutOfServiceBedCancellationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1OutOfServiceBedEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1OutOfServiceBedRevisionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.ApprovedPremisesUserFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1OutOfServiceBedCancellationTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1OutOfServiceBedReasonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1OutOfServiceBedRevisionTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1OutOfServiceBedTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringLowerCase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.roundNanosToMillisToAccountForLossOfPrecisionInPostgres
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class Cas1OutOfServiceBedTransformerTest {
  private val cas1OutOfServiceBedReasonTransformer = mockk<Cas1OutOfServiceBedReasonTransformer>()
  private val cas1OutOfServiceBedCancellationTransformer = mockk<Cas1OutOfServiceBedCancellationTransformer>()
  private val cas1OutOfServiceBedRevisionTransformer = mockk<Cas1OutOfServiceBedRevisionTransformer>()
  private val transformer = Cas1OutOfServiceBedTransformer(
    cas1OutOfServiceBedReasonTransformer,
    cas1OutOfServiceBedCancellationTransformer,
    cas1OutOfServiceBedRevisionTransformer,
  )

  @CsvSource(
    value = [
      "-10,-10,1,PAST",
      "-10,-5,6,PAST",
      "-5,5,11,CURRENT",
      "-1,0,2,CURRENT",
      "0,0,1,CURRENT",
      "0,1,2,CURRENT",
      "5,10,6,FUTURE",
    ],
  )
  @ParameterizedTest
  fun `transformJpaToApi transforms correctly when active`(
    startDateOffsetDays: Long,
    endDateOffsetDays: Long,
    expectedDaysCount: Int,
    expectedTemporality: Temporality,
  ) {
    val today = LocalDate.now()

    val outOfServiceBed = Cas1OutOfServiceBedEntityFactory()
      .withBed {
        withRoom {
          withPremises(
            ApprovedPremisesEntityFactory()
              .withDefaults()
              .produce(),
          )
        }
      }
      .produce()
      .apply {
        this.revisionHistory += Cas1OutOfServiceBedRevisionEntityFactory()
          .withOutOfServiceBed(this)
          .withStartDate(today.plusDays(startDateOffsetDays))
          .withEndDate(today.plusDays(endDateOffsetDays))
          .withNotes("Some notes")
          .produce()
      }

    val reason = Cas1OutOfServiceBedReason(
      id = UUID.randomUUID(),
      name = randomStringLowerCase(12),
      isActive = true,
    )

    val historyItem = Cas1OutOfServiceBedRevision(
      id = UUID.randomUUID(),
      updatedAt = OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres().toInstant(),
      updatedBy = ApprovedPremisesUserFactory().produce(),
      revisionType = listOf(Cas1OutOfServiceBedRevisionType.CREATED),
    )

    every { cas1OutOfServiceBedReasonTransformer.transformJpaToApi(outOfServiceBed.reason) } returns reason
    every { cas1OutOfServiceBedRevisionTransformer.transformJpaToApi(any()) } returns historyItem

    val result = transformer.transformJpaToApi(outOfServiceBed)

    assertThat(result.id).isEqualTo(outOfServiceBed.id)
    assertThat(result.createdAt).isEqualTo(outOfServiceBed.createdAt.toInstant())
    assertThat(result.startDate).isEqualTo(outOfServiceBed.startDate)
    assertThat(result.endDate).isEqualTo(outOfServiceBed.endDate)
    assertThat(result.bed.id).isEqualTo(outOfServiceBed.bed.id)
    assertThat(result.bed.name).isEqualTo(outOfServiceBed.bed.name)
    assertThat(result.room.id).isEqualTo(outOfServiceBed.bed.room.id)
    assertThat(result.room.name).isEqualTo(outOfServiceBed.bed.room.name)
    assertThat(result.premises.id).isEqualTo(outOfServiceBed.premises.id)
    assertThat(result.premises.name).isEqualTo(outOfServiceBed.premises.name)
    assertThat(result.apArea.id).isEqualTo(outOfServiceBed.premises.probationRegion.apArea!!.id)
    assertThat(result.apArea.name).isEqualTo(outOfServiceBed.premises.probationRegion.apArea!!.name)
    assertThat(result.reason).isEqualTo(reason)
    assertThat(result.daysLostCount).isEqualTo(expectedDaysCount)
    assertThat(result.temporality).isEqualTo(expectedTemporality)
    assertThat(result.status).isEqualTo(Cas1OutOfServiceBedStatus.ACTIVE)
    assertThat(result.referenceNumber).isEqualTo(outOfServiceBed.referenceNumber)
    assertThat(result.notes).isEqualTo(outOfServiceBed.notes)
    assertThat(result.cancellation).isNull()

    verify(exactly = 0) { cas1OutOfServiceBedCancellationTransformer.transformJpaToApi(any()) }
  }

  @Test
  fun `transformJpaToApi transforms correctly when cancelled`() {
    val outOfServiceBed = Cas1OutOfServiceBedEntityFactory()
      .withBed {
        withRoom {
          withPremises(
            ApprovedPremisesEntityFactory()
              .withDefaults()
              .produce(),
          )
        }
      }
      .produce()
      .apply {
        this.revisionHistory += Cas1OutOfServiceBedRevisionEntityFactory()
          .withOutOfServiceBed(this)
          .withNotes("Some notes")
          .produce()
      }

    val cancellationEntity = Cas1OutOfServiceBedCancellationEntityFactory()
      .withOutOfServiceBed(outOfServiceBed)
      .produce()

    outOfServiceBed.cancellation = cancellationEntity

    val reason = Cas1OutOfServiceBedReason(
      id = UUID.randomUUID(),
      name = randomStringLowerCase(12),
      isActive = true,
    )

    val cancellation = Cas1OutOfServiceBedCancellation(
      id = UUID.randomUUID(),
      createdAt = Instant.now(),
      notes = randomStringMultiCaseWithNumbers(20),
    )

    val historyItem = Cas1OutOfServiceBedRevision(
      id = UUID.randomUUID(),
      updatedAt = OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres().toInstant(),
      updatedBy = ApprovedPremisesUserFactory().produce(),
      revisionType = listOf(Cas1OutOfServiceBedRevisionType.CREATED),
    )

    every { cas1OutOfServiceBedReasonTransformer.transformJpaToApi(outOfServiceBed.reason) } returns reason
    every { cas1OutOfServiceBedCancellationTransformer.transformJpaToApi(cancellationEntity) } returns cancellation
    every { cas1OutOfServiceBedRevisionTransformer.transformJpaToApi(any()) } returns historyItem

    val result = transformer.transformJpaToApi(outOfServiceBed)

    assertThat(result.status).isEqualTo(Cas1OutOfServiceBedStatus.CANCELLED)
    assertThat(result.cancellation).isEqualTo(cancellation)
  }
}
