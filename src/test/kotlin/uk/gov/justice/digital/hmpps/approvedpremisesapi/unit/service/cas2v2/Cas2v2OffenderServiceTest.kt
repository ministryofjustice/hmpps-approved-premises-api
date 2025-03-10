package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas2v2

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CaseNotesClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonerAlertsApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonsApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.ExcludedCategoryBindingModel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.PrisonAdjudicationsConfigBindingModel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.PrisonCaseNotesConfigBindingModel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.datasource.OffenderDetailsDataSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas2v2.Cas2v2UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2DeliusUserLaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.ObjectMapperFactory

class Cas2v2OffenderServiceTest {
  private val mockPrisonsApiClient = mockk<PrisonsApiClient>()
  private val mockPrisonerAlertsApiClient = mockk<PrisonerAlertsApiClient>()
  private val mockCaseNotesClient = mockk<CaseNotesClient>()
  private val mockApDeliusContextApiClient = mockk<ApDeliusContextApiClient>()
  private val mockOffenderDetailsDataSource = mockk<OffenderDetailsDataSource>()
  private val mockPersonTransformer = mockk<PersonTransformer>()

  private val prisonCaseNotesConfigBindingModel = PrisonCaseNotesConfigBindingModel().apply {
    lookbackDays = 30
    prisonApiPageSize = 2
    excludedCategories = listOf(
      ExcludedCategoryBindingModel().apply {
        this.category = "CATEGORY"
        this.subcategory = "EXCLUDED_SUBTYPE"
      },
      ExcludedCategoryBindingModel().apply {
        this.category = "EXCLUDED_CATEGORY"
        this.subcategory = null
      },
    )
  }
  private val adjudicationsConfigBindingModel = PrisonAdjudicationsConfigBindingModel().apply {
    prisonApiPageSize = 2
  }

  private val objectMapper = ObjectMapperFactory.createRuntimeLikeObjectMapper()

  private val offenderService = OffenderService(
    mockPrisonsApiClient,
    mockPrisonerAlertsApiClient,
    mockCaseNotesClient,
    mockApDeliusContextApiClient,
    mockOffenderDetailsDataSource,
    mockPersonTransformer,
    prisonCaseNotesConfigBindingModel,
    adjudicationsConfigBindingModel,
  )

  val deliusUser = Cas2v2UserEntityFactory()
    .withUserType(Cas2v2UserType.DELIUS)
    .produce()

  val nomisUser = Cas2v2UserEntityFactory()
    .withUserType(Cas2v2UserType.NOMIS)
    .produce()

  @Nested
  inner class CheckRestrictedOffender {

    private fun mockRestrictedLaoOffender() {
      val resultBody = OffenderDetailsSummaryFactory()
        .withCrn("a-crn")
        .withFirstName("Bob")
        .withLastName("Doe")
        .withCurrentRestriction(true)
        .withCurrentExclusion(false)
        .produce()

      every {
        mockOffenderDetailsDataSource.getOffenderDetailSummary("a-crn")
      } returns ClientResult.Success(HttpStatus.OK, resultBody)
    }

    @Test
    fun `Check a nomis user cannot view an offender with a currentRestriction`() {
      val personInfo = offenderService.getPersonInfoResult("a-crn", deliusUser.cas2DeliusUserLaoStrategy())
    }

    @Test
    fun `Check a delius user cannot view an offender with a currentRestriction`() {
      val personInfo = offenderService.getPersonInfoResult("a-crn", nomisUser.cas2DeliusUserLaoStrategy())
    }
  }

  @Nested
  inner class CheckExcludedOffender {

    private fun mockExcludedLaoOffender() {
      val resultBody = OffenderDetailsSummaryFactory()
        .withCrn("a-crn")
        .withFirstName("Bob")
        .withLastName("Doe")
        .withCurrentRestriction(false)
        .withCurrentExclusion(true)
        .produce()

      every {
        mockOffenderDetailsDataSource.getOffenderDetailSummary("a-crn")
      } returns ClientResult.Success(HttpStatus.OK, resultBody)
    }

    @Test
    fun `Check a nomis user can view an offender with a currentExclusion`() {}

    @Test
    fun `Check a delius user cannot view an offender with a currentExclusion if restricted by LAO`() {}

    @Test
    fun `Check a delius user can view an offender with a currentExclusion if allowed by LAO`() {}
  }
}
