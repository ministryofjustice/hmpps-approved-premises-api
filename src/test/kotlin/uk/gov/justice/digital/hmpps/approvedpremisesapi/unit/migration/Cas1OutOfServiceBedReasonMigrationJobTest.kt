package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.migration

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1OutOfServiceBedReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LostBedReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1.Cas1OutOfServiceBedReasonMigrationJob
import java.time.OffsetDateTime
import java.util.UUID

class Cas1OutOfServiceBedReasonMigrationJobTest {
  private val lostBedReasonRepository = mockk<LostBedReasonRepository>()
  private val cas1OutOfServiceBedReasonRepository = mockk<Cas1OutOfServiceBedReasonRepository>()

  private val job = Cas1OutOfServiceBedReasonMigrationJob(
    lostBedReasonRepository,
    cas1OutOfServiceBedReasonRepository,
  )

  @Test
  fun `The job migrates all Approved Premises lost beds reasons to the out of service beds table`() {
    val lostBedReasons = listOf(
      LostBedReasonEntityFactory()
        .withId(UUID(1L, 1L))
        .withName("Active Reason")
        .withIsActive(true)
        .withServiceScope(ServiceName.approvedPremises.value)
        .produce(),
      LostBedReasonEntityFactory()
        .withId(UUID(2L, 2L))
        .withName("Inactive Reason")
        .withIsActive(false)
        .withServiceScope(ServiceName.approvedPremises.value)
        .produce(),
    )

    val now = OffsetDateTime.now()

    val outOfServiceBedReasons = listOf(
      Cas1OutOfServiceBedReasonEntityFactory()
        .withId(UUID(1L, 1L))
        .withCreatedAt(now)
        .withName("Active Reason")
        .withIsActive(true)
        .produce(),
      Cas1OutOfServiceBedReasonEntityFactory()
        .withId(UUID(2L, 2L))
        .withCreatedAt(now)
        .withName("Inactive Reason")
        .withIsActive(false)
        .produce(),
    )

    mockkStatic(OffsetDateTime::class)
    every { OffsetDateTime.now() } returns now

    every { lostBedReasonRepository.findAllByServiceScope(ServiceName.approvedPremises.value) } returns lostBedReasons
    every { cas1OutOfServiceBedReasonRepository.saveAll<Cas1OutOfServiceBedReasonEntity>(any()) } returnsArgument 0

    job.process()

    unmockkStatic(OffsetDateTime::class)

    verify { cas1OutOfServiceBedReasonRepository.saveAll(outOfServiceBedReasons) }
  }
}
