package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.RequestContextService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.addRoleForUnitTest
import java.util.UUID

class Cas1LegacyManagerUserAccessTest {
  private val userService = mockk<UserService>()
  private val offenderService = mockk<OffenderService>()
  private val requestContextService = mockk<RequestContextService>()

  private val userAccessService = UserAccessService(
    userService,
    offenderService,
    requestContextService,
  )

  private val probationRegionId = UUID.randomUUID()
  private val probationRegion = ProbationRegionEntityFactory()
    .withId(probationRegionId)
    .withApArea(
      ApAreaEntityFactory()
        .produce(),
    )
    .produce()

  private val legacyManager = UserEntityFactory()
    .withProbationRegion(probationRegion)
    .produce()

  val approvedPremises = ApprovedPremisesEntityFactory()
    .withProbationRegion(probationRegion)
    .withLocalAuthorityArea(
      LocalAuthorityEntityFactory()
        .produce(),
    )
    .produce()

  @BeforeEach
  fun setup() {
    legacyManager.addRoleForUnitTest(UserRole.CAS1_LEGACY_MANAGER)
    every { userService.getUserForRequest() } returns legacyManager
    every { requestContextService.getServiceForRequest() } returns ServiceName.approvedPremises
  }

  @Nested
  inner class Cas1LegacyManager {

    @Test
    fun `may view premises`() {
      assertThat(userAccessService.userCanViewPremises(legacyManager, approvedPremises)).isTrue
    }

    @Test
    fun `may manage premises`() {
      assertThat(userAccessService.userCanManagePremises(legacyManager, approvedPremises)).isTrue
    }

    @Test
    fun `may view booking`() {
      val booking = BookingEntityFactory()
        .withPremises(approvedPremises)
        .produce()

      assertThat(userAccessService.userCanViewBooking(legacyManager, booking)).isTrue
    }

    @Test
    fun `may manage premises booking`() {
      assertThat(userAccessService.userCanManagePremisesBookings(legacyManager, approvedPremises)).isTrue
    }

    @Test
    fun `may manage premises out-of-service (aka lost) beds`() {
      assertThat(userAccessService.userCanManagePremisesLostBeds(legacyManager, approvedPremises)).isTrue
    }

    @Test
    fun `may view premises capacity`() {
      assertThat(userAccessService.userCanViewPremisesCapacity(legacyManager, approvedPremises)).isTrue
    }

    @Test
    fun `may view premises staff`() {
      assertThat(userAccessService.userCanViewPremisesStaff(legacyManager, approvedPremises)).isTrue
    }
  }
}
