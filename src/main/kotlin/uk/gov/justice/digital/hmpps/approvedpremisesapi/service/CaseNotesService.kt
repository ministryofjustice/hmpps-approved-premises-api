package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CaseNotesClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.ExcludedCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.PrisonCaseNotesConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.PrisonCaseNotesConfigBindingModel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.CaseNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.CaseNotesPage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import java.time.LocalDate

@Service
class CaseNotesService(
  private val caseNotesClient: CaseNotesClient,
  prisonCaseNotesConfigBindingModel: PrisonCaseNotesConfigBindingModel,
) {

  private val prisonCaseNotesConfig: PrisonCaseNotesConfig
  init {
    val excludedCategories = prisonCaseNotesConfigBindingModel.excludedCategories
      ?: throw RuntimeException("No prison-case-notes.excluded-categories provided")

    prisonCaseNotesConfig = PrisonCaseNotesConfig(
      lookbackDays = prisonCaseNotesConfigBindingModel.lookbackDays ?: throw RuntimeException("No prison-case-notes.lookback-days configuration provided"),
      prisonApiPageSize = prisonCaseNotesConfigBindingModel.prisonApiPageSize ?: throw RuntimeException("No prison-api-page-size configuration provided"),
      excludedCategories = excludedCategories.mapIndexed { index, categoryConfig ->
        ExcludedCategory(
          category = categoryConfig.category ?: throw RuntimeException("No category provided for prison-case-notes.excluded-categories at index $index"),
          subcategory = categoryConfig.subcategory,
        )
      },
    )
  }

  @SuppressWarnings("TooGenericExceptionThrown")
  fun getFilteredPrisonCaseNotesByNomsNumber(nomsNumber: String, getCas1SpecificNoteTypes: Boolean): CasResult<List<CaseNote>> {
    val cas1PrisonNoteTypesToInclude = listOf(
      "Alert", "Conduct & Behaviour", "Custodial Violence Management", "Negative Behaviours", "Enforcement", "Interventions / Keywork",
      "Mental Health", "Drug Rehabilitation", "Social Care", "Positive Behaviour / Achievements", "Alcohol Treatment",
    )
    val allCaseNotes = mutableListOf<CaseNote>()

    val fromDate = LocalDate.now().minusDays(prisonCaseNotesConfig.lookbackDays.toLong())

    var currentPage: CaseNotesPage?
    var currentPageIndex: Int? = null
    do {
      if (currentPageIndex == null) {
        currentPageIndex = 0
      } else {
        currentPageIndex += 1
      }

      val caseNotesPageResponse = caseNotesClient.getCaseNotesPage(nomsNumber, fromDate, currentPageIndex, prisonCaseNotesConfig.prisonApiPageSize)
      currentPage = when (caseNotesPageResponse) {
        is ClientResult.Success -> caseNotesPageResponse.body
        is ClientResult.Failure.StatusCode -> when (caseNotesPageResponse.status) {
          HttpStatus.NOT_FOUND -> return CasResult.NotFound(entityType = "CaseNotes", id = "nomsNumber")
          HttpStatus.FORBIDDEN -> return CasResult.Unauthorised()
          else -> caseNotesPageResponse.throwException()
        }
        is ClientResult.Failure -> caseNotesPageResponse.throwException()
      }

      allCaseNotes.addAll(
        if (getCas1SpecificNoteTypes) {
          currentPage.content.filter { caseNote ->
            cas1PrisonNoteTypesToInclude.any { it == (caseNote.typeDescription ?: caseNote.type) }
          }
        } else {
          currentPage.content.filter { caseNote ->
            prisonCaseNotesConfig.excludedCategories.none { it.excluded(caseNote.type, caseNote.subType) }
          }
        },
      )
    } while (currentPage != null && currentPage.totalPages > currentPageIndex!! + 1)

    return CasResult.Success(allCaseNotes)
  }
}
