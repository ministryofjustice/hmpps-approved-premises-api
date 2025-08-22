package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import kotlinx.datetime.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CaseNotesClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult.Failure.StatusCode
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.CaseNotesPage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.CaseNotesRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.PageMetaData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.ExcludedCategoryBindingModel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.PrisonCaseNotesConfigBindingModel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseNoteFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.CaseNotesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertThatCasResult
import java.time.LocalDate
import java.time.LocalTime

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

      val caseNotesRequest = CaseNotesRequest(
        page = 1,
        includeSensitive = true,
        occurredFrom = java.time.LocalDateTime.of(LocalDate.now().minusDays(prisonCaseNotesConfigBindingModel.lookbackDays!!.toLong()), LocalTime.MIN),
        size = 2,
        sort = "occurredAt,desc",
      )

      every {
        mockCaseNotesClient.getCaseNotesPage(
          personIdentifier = nomsNumber,
          caseNotesRequest = caseNotesRequest,

        )
      } returns StatusCode(HttpMethod.POST, "/search/case-notes/$nomsNumber", HttpStatus.NOT_FOUND, null)

      val result = service.getFilteredPrisonCaseNotesByNomsNumber(nomsNumber, false)
      assertThatCasResult(result).isNotFound("CaseNotes", nomsNumber)
    }

    @Test
    fun `getFilteredPrisonCaseNotesByNomsNumber returns Unauthorised when Case Notes request returns a 403`() {
      val nomsNumber = "NOMS456"

      val caseNotesRequest = CaseNotesRequest(
        page = 1,
        includeSensitive = true,
        occurredFrom = java.time.LocalDateTime.of(LocalDate.now().minusDays(prisonCaseNotesConfigBindingModel.lookbackDays!!.toLong()), LocalTime.MIN),
        size = 2,
        sort = "occurredAt,desc",
      )

      every {
        mockCaseNotesClient.getCaseNotesPage(
          personIdentifier = nomsNumber,
          caseNotesRequest = caseNotesRequest,
        )
      } returns StatusCode(HttpMethod.POST, "/search/case-notes/$nomsNumber", HttpStatus.FORBIDDEN, null)

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

      val caseNotesRequest = CaseNotesRequest(
        page = 1,
        includeSensitive = true,
        occurredFrom = java.time.LocalDateTime.of(LocalDate.now().minusDays(prisonCaseNotesConfigBindingModel.lookbackDays!!.toLong()), LocalTime.MIN),
        size = 2,
        sort = "occurredAt,desc",
      )

      every {
        mockCaseNotesClient.getCaseNotesPage(
          personIdentifier = nomsNumber,
          caseNotesRequest = caseNotesRequest,
        )
      } returns ClientResult.Success(
        HttpStatus.OK,
        CaseNotesPage(
          content = caseNotesPageOne,
          hasCaseNotes = true,
          metadata = PageMetaData(
            totalElements = 6,
            page = 1,
            size = 1,
          ),
        ),
      )

      val caseNotesRequest2 = CaseNotesRequest(
        page = 2,
        includeSensitive = true,
        occurredFrom = java.time.LocalDateTime.of(LocalDate.now().minusDays(prisonCaseNotesConfigBindingModel.lookbackDays!!.toLong()), LocalTime.MIN),
        size = 2,
        sort = "occurredAt,desc",
      )

      every {
        mockCaseNotesClient.getCaseNotesPage(
          personIdentifier = nomsNumber,
          caseNotesRequest = caseNotesRequest2,

        )
      } returns ClientResult.Success(
        HttpStatus.OK,
        CaseNotesPage(
          content = caseNotesPageTwo,
          hasCaseNotes = true,
          metadata = PageMetaData(
            page = 2,
            size = 2,
            totalElements = 4,
          ),
        ),
      )

      val result = service.getFilteredPrisonCaseNotesByNomsNumber(nomsNumber, false)

      assertThatCasResult(result).isSuccess().with {
        assertThat(it).containsAll(caseNotesPageOne.subList(1, 1) + caseNotesPageTwo.subList(2, 2))
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

      val caseNotesRequest = CaseNotesRequest(
        page = 1,
        includeSensitive = true,
        occurredFrom = java.time.LocalDateTime.of(LocalDate.now().minusDays(prisonCaseNotesConfigBindingModel.lookbackDays!!.toLong()), LocalTime.MIN),
        size = 2,
        sort = "occurredAt,desc",
      )

      every {
        mockCaseNotesClient.getCaseNotesPage(
          personIdentifier = nomsNumber,
          caseNotesRequest = caseNotesRequest,

        )
      } returns ClientResult.Success(
        HttpStatus.OK,
        CaseNotesPage(
          content = caseNotesPageOne,
          hasCaseNotes = true,
          metadata = PageMetaData(
            totalElements = 6,
            page = 1,
            size = 2,
          ),
        ),
      )

      val caseNotesRequest2 = CaseNotesRequest(
        page = 2,
        includeSensitive = true,
        occurredFrom = java.time.LocalDateTime.of(LocalDate.now().minusDays(prisonCaseNotesConfigBindingModel.lookbackDays!!.toLong()), LocalTime.MIN),
        size = 2,
        sort = "occurredAt,desc",
      )

      every {
        mockCaseNotesClient.getCaseNotesPage(
          personIdentifier = nomsNumber,
          caseNotesRequest = caseNotesRequest2,

        )
      } returns ClientResult.Success(
        HttpStatus.OK,
        CaseNotesPage(
          content = caseNotesPageTwo,
          hasCaseNotes = true,
          metadata = PageMetaData(
            page = 2,
            size = 2,
            totalElements = 4,
          ),
        ),
      )

      val result = service.getFilteredPrisonCaseNotesByNomsNumber(nomsNumber, true)

      assertThatCasResult(result).isSuccess().with {
        assertThat(it).containsAll(caseNotesPageTwo.subList(2, 3) + caseNotesPageTwo.subList(2, 3))
      }
    }
  }
}
