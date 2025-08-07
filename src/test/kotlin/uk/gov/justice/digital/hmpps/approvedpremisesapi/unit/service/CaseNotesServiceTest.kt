package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CaseNotesClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult.Failure.StatusCode
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.CaseNotesPage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.ExcludedCategoryBindingModel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.PrisonCaseNotesConfigBindingModel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseNoteFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.CaseNotesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertThatCasResult
import java.time.LocalDate

class CaseNotesServiceTest {
  private val mockCaseNotesClient = mockk<CaseNotesClient>()

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

  private val service = CaseNotesService(mockCaseNotesClient, prisonCaseNotesConfigBindingModel)

  @Nested
  inner class GetFilteredPrisonCaseNotesByNomsNumber {

    @Test
    fun `getFilteredPrisonCaseNotesByNomsNumber returns NotFound when Case Notes request returns a 404`() {
      val nomsNumber = "NOMS456"

      every {
        mockCaseNotesClient.getCaseNotesPage(
          nomsNumber = nomsNumber,
          from = LocalDate.now().minusDays(prisonCaseNotesConfigBindingModel.lookbackDays!!.toLong()),
          page = 0,
          pageSize = 2,
        )
      } returns StatusCode(HttpMethod.GET, "/api/offenders/$nomsNumber/v2", HttpStatus.NOT_FOUND, null)

      val result = service.getFilteredPrisonCaseNotesByNomsNumber(nomsNumber, false)
      assertThatCasResult(result).isNotFound("CaseNotes", nomsNumber)
    }

    @Test
    fun `getFilteredPrisonCaseNotesByNomsNumber returns Unauthorised when Case Notes request returns a 403`() {
      val nomsNumber = "NOMS456"

      every {
        mockCaseNotesClient.getCaseNotesPage(
          nomsNumber = nomsNumber,
          from = LocalDate.now().minusDays(prisonCaseNotesConfigBindingModel.lookbackDays!!.toLong()),
          page = 0,
          pageSize = 2,
        )
      } returns StatusCode(HttpMethod.GET, "/api/offenders/$nomsNumber/v2", HttpStatus.FORBIDDEN, null)

      val result = service.getFilteredPrisonCaseNotesByNomsNumber(nomsNumber, false)
      assertThatCasResult(result).isUnauthorised()
    }

    @Test
    fun `getFilteredPrisonCaseNotesByNomsNumber returns Success, traverses pages from Client & excludes categories + subcategories`() {
      val nomsNumber = "NOMS456"

      val caseNotesPageOne = listOf(
        CaseNoteFactory().produce(),
        CaseNoteFactory().produce(),
        CaseNoteFactory().withType("EXCLUDED_TYPE").produce(),
      )

      val caseNotesPageTwo = listOf(
        CaseNoteFactory().produce(),
        CaseNoteFactory().produce(),
        CaseNoteFactory().withType("TYPE").withSubType("EXCLUDED_SUBTYPE").produce(),
      )

      every {
        mockCaseNotesClient.getCaseNotesPage(
          nomsNumber = nomsNumber,
          from = LocalDate.now().minusDays(prisonCaseNotesConfigBindingModel.lookbackDays!!.toLong()),
          page = 0,
          pageSize = 2,
        )
      } returns ClientResult.Success(
        HttpStatus.OK,
        CaseNotesPage(
          totalElements = 6,
          totalPages = 2,
          number = 1,
          content = caseNotesPageOne,
        ),
      )

      every {
        mockCaseNotesClient.getCaseNotesPage(
          nomsNumber = nomsNumber,
          from = LocalDate.now().minusDays(prisonCaseNotesConfigBindingModel.lookbackDays!!.toLong()),
          page = 1,
          pageSize = 2,
        )
      } returns ClientResult.Success(
        HttpStatus.OK,
        CaseNotesPage(
          totalElements = 4,
          totalPages = 2,
          number = 2,
          content = caseNotesPageTwo,
        ),
      )

      val result = service.getFilteredPrisonCaseNotesByNomsNumber(nomsNumber, false)

      assertThatCasResult(result).isSuccess().with {
        assertThat(it).containsAll(caseNotesPageOne.subList(0, 1) + caseNotesPageTwo.subList(0, 1))
      }
    }

    @Test
    fun `getFilteredPrisonCaseNotesByNomsNumber returns specified case note types when getSpecificNoteTypes is true`() {
      val nomsNumber = "NOMS456"

      val caseNotesPageOne = listOf(
        CaseNoteFactory().produce(),
        CaseNoteFactory().produce(),
        CaseNoteFactory().withTypeDescription(null).withType("Enforcement").produce(),
      )

      val caseNotesPageTwo = listOf(
        CaseNoteFactory().produce(),
        CaseNoteFactory().produce(),
        CaseNoteFactory().withTypeDescription("Alert").produce(),
      )

      every {
        mockCaseNotesClient.getCaseNotesPage(
          nomsNumber = nomsNumber,
          from = LocalDate.now().minusDays(prisonCaseNotesConfigBindingModel.lookbackDays!!.toLong()),
          page = 0,
          pageSize = 2,
        )
      } returns ClientResult.Success(
        HttpStatus.OK,
        CaseNotesPage(
          totalElements = 6,
          totalPages = 2,
          number = 1,
          content = caseNotesPageOne,
        ),
      )

      every {
        mockCaseNotesClient.getCaseNotesPage(
          nomsNumber = nomsNumber,
          from = LocalDate.now().minusDays(prisonCaseNotesConfigBindingModel.lookbackDays!!.toLong()),
          page = 1,
          pageSize = 2,
        )
      } returns ClientResult.Success(
        HttpStatus.OK,
        CaseNotesPage(
          totalElements = 4,
          totalPages = 2,
          number = 2,
          content = caseNotesPageTwo,
        ),
      )

      val result = service.getFilteredPrisonCaseNotesByNomsNumber(nomsNumber, true)

      assertThatCasResult(result).isSuccess().with {
        assertThat(it).containsAll(caseNotesPageTwo.subList(2, 3) + caseNotesPageTwo.subList(2, 3))
      }
    }
  }
}
