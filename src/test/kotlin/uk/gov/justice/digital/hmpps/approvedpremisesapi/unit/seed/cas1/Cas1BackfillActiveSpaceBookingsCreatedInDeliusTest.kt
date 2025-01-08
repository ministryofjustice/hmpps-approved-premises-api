package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.seed.cas1

import io.mockk.every
import io.mockk.mockk
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

  private val seedJob = Cas1BackfillActiveSpaceBookingsCreatedInDelius(
    approvedPremisesRepository = approvedPremisesRepository,
    cas1DeliusBookingImportRepository = mockk<Cas1DeliusBookingImportRepository>(),
    cas1BookingManagementInfoService = mockk<Cas1BookingManagementInfoService>(),
    environmentService = mockk<EnvironmentService>(),
    offenderService = mockk<OffenderService>(),
    offlineApplicationRepository = mockk<OfflineApplicationRepository>(),
    spaceBookingRepository = mockk<Cas1SpaceBookingRepository>(),
    transactionTemplate = mockk<TransactionTemplate>(),
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
}
