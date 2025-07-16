package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.controller.cas3

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3UnarchivePremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas3.Cas3PremisesController
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.Cas3BookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.Cas3PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3BedspaceTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3DepartureTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3FutureBookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3PremisesSearchResultsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3PremisesSummaryTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3PremisesTransformer
import java.time.LocalDate
import java.util.UUID

class Cas3PremisesControllerTest {
  private val cas3PremisesService = mockk<Cas3PremisesService>()
  private val cas3BookingService = mockk<Cas3BookingService>()
  private val userService = mockk<UserService>()
  private val userAccessService = mockk<UserAccessService>()
  private val cas3FutureBookingTransformer = mockk<Cas3FutureBookingTransformer>()
  private val cas3PremisesSummaryTransformer = mockk<Cas3PremisesSummaryTransformer>()
  private val cas3PremisesSearchResultsTransformer = mockk<Cas3PremisesSearchResultsTransformer>()
  private val cas3DepartureTransformer = mockk<Cas3DepartureTransformer>()
  private val cas3PremisesTransformer = mockk<Cas3PremisesTransformer>()
  private val cas3BedspaceTransformer = mockk<Cas3BedspaceTransformer>()

  private val controller = Cas3PremisesController(
    userService,
    userAccessService,
    cas3BookingService,
    cas3PremisesService,
    cas3FutureBookingTransformer,
    cas3PremisesSummaryTransformer,
    cas3PremisesSearchResultsTransformer,
    cas3DepartureTransformer,
    cas3PremisesTransformer,
    cas3BedspaceTransformer,
  )

  @Nested
  inner class UnarchivePremises {
    @Test
    fun `unarchivePremises returns 200 OK when premises is successfully unarchived`() {
      val premisesId = UUID.randomUUID()
      val unarchiveRequest = Cas3UnarchivePremises(
        restartDate = LocalDate.now().plusDays(1),
      )

      val probationRegion = ProbationRegionEntityFactory()
        .withApArea(ApAreaEntityFactory().produce())
        .produce()
      val localAuthority = LocalAuthorityEntityFactory().produce()

      val premises = TemporaryAccommodationPremisesEntityFactory()
        .withProbationRegion(probationRegion)
        .withLocalAuthorityArea(localAuthority)
        .produce()

      val unarchivedPremises = TemporaryAccommodationPremisesEntityFactory()
        .withProbationRegion(probationRegion)
        .withLocalAuthorityArea(localAuthority)
        .produce()
      val transformedPremises = mockk<Cas3Premises>()

      every { cas3PremisesService.getPremises(premisesId) } returns premises
      every { userAccessService.currentUserCanManagePremises(premises) } returns true
      every { cas3PremisesService.unarchivePremises(premisesId, unarchiveRequest.restartDate) } returns CasResult.Success(unarchivedPremises)
      every { cas3PremisesTransformer.transformDomainToApi(unarchivedPremises) } returns transformedPremises

      val response = controller.unarchivePremises(premisesId, unarchiveRequest)

      assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
      assertThat(response.body).isEqualTo(transformedPremises)

      verify(exactly = 1) {
        cas3PremisesService.unarchivePremises(premisesId, unarchiveRequest.restartDate)
      }
    }

    @Test
    fun `unarchivePremises throws NotFoundProblem when premises does not exist`() {
      val premisesId = UUID.randomUUID()
      val unarchiveRequest = Cas3UnarchivePremises(
        restartDate = LocalDate.now().plusDays(1),
      )

      every { cas3PremisesService.getPremises(premisesId) } returns null

      assertThrows<NotFoundProblem> {
        controller.unarchivePremises(premisesId, unarchiveRequest)
      }
    }

    @Test
    fun `unarchivePremises throws ForbiddenProblem when user access is denied`() {
      val premisesId = UUID.randomUUID()
      val unarchiveRequest = Cas3UnarchivePremises(
        restartDate = LocalDate.now().plusDays(1),
      )

      val probationRegion = ProbationRegionEntityFactory()
        .withApArea(ApAreaEntityFactory().produce())
        .produce()
      val localAuthority = LocalAuthorityEntityFactory().produce()

      val premises = TemporaryAccommodationPremisesEntityFactory()
        .withProbationRegion(probationRegion)
        .withLocalAuthorityArea(localAuthority)
        .produce()

      every { cas3PremisesService.getPremises(premisesId) } returns premises
      every { userAccessService.currentUserCanManagePremises(premises) } returns false

      assertThrows<ForbiddenProblem> {
        controller.unarchivePremises(premisesId, unarchiveRequest)
      }
    }
  }
}
