package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.seed.cas1

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1DeliusBookingImportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1BackfillActiveSpaceBookingsCreatedInDelius
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1BookingManagementInfoService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1CreateMissingReferralsSeedCsvRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EnvironmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService

class Cas1BackfillActiveSpaceBookingsCreatedInDeliusTest {

  private val approvedPremisesRepository = mockk<ApprovedPremisesRepository>()
  private val environmentService = mockk<EnvironmentService>()

  private val seedJob = Cas1BackfillActiveSpaceBookingsCreatedInDelius(
    approvedPremisesRepository = approvedPremisesRepository,
    cas1DeliusBookingImportRepository = mockk<Cas1DeliusBookingImportRepository>(),
    cas1BookingManagementInfoService = mockk<Cas1BookingManagementInfoService>(),
    offenderService = mockk<OffenderService>(),
    offlineApplicationRepository = mockk<OfflineApplicationRepository>(),
    spaceBookingRepository = mockk<Cas1SpaceBookingRepository>(),
    transactionTemplate = mockk<TransactionTemplate>(),
    environmentService = environmentService,
  )

  @Test
  fun `fails if premise doesn't support space booking`() {
    every { approvedPremisesRepository.findByQCode("Q123") } returns
      ApprovedPremisesEntityFactory()
        .withSupportsSpaceBookings(false)
        .withDefaults()
        .produce()

    assertThrows<RuntimeException> {
      seedJob.processRow(Cas1CreateMissingReferralsSeedCsvRow("Q123"))
    }
  }

  @Test
  fun `pre-seed fails if in prod`() {
    every { environmentService.isProd() } returns true

    assertThatThrownBy {
      seedJob.preSeed()
    }.hasMessage("Cannot run seed job in prod")
  }

  @Test
  fun `pre-seed succeeds if not in prod`() {
    every { environmentService.isProd() } returns false

    seedJob.preSeed()
  }
}
