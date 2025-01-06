package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.seed.cas1

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1DeliusBookingImportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1BookingManagementInfoService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1BookingToSpaceBookingSeedCsvRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1BookingToSpaceBookingSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EnvironmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService

class Cas1BookingToSpaceBookingSeedJobTest {

  private val approvedPremisesRepository = mockk<ApprovedPremisesRepository>()

  private val seedJob = Cas1BookingToSpaceBookingSeedJob(
    approvedPremisesRepository = approvedPremisesRepository,
    spaceBookingRepository = mockk<Cas1SpaceBookingRepository>(),
    bookingRepository = mockk<BookingRepository>(),
    domainEventRepository = mockk<DomainEventRepository>(),
    domainEventService = mockk<Cas1DomainEventService>(),
    userRepository = mockk<UserRepository>(),
    transactionTemplate = mockk<TransactionTemplate>(),
    cas1DeliusBookingImportRepository = mockk<Cas1DeliusBookingImportRepository>(),
    cas1BookingManagementInfoService = mockk<Cas1BookingManagementInfoService>(),
    environmentService = mockk<EnvironmentService>(),
    placementRequestRepository = mockk<PlacementRequestRepository>(),
  )

  @Test
  fun `fails if premise doesn't support space booking`() {
    every { approvedPremisesRepository.findByQCode("Q123") } returns
      ApprovedPremisesEntityFactory()
        .withSupportsSpaceBookings(false)
        .withDefaults()
        .produce()

    assertThrows<RuntimeException> {
      seedJob.processRow(Cas1BookingToSpaceBookingSeedCsvRow("Q123"))
    }
  }
}
