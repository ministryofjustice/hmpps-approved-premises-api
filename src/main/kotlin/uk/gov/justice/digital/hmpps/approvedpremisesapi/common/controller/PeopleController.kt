package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ActiveOffence
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Adjudication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysSections
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonAcctAlert
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PrisonCaseNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Problem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ValidationError
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.CaseNotesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OASysService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderDetailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AdjudicationTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.OASysSectionsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.OffenceTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PrisonCaseNoteTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PrisonerAlertTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult

@SuppressWarnings("ThrowsCount")
@RestController
@Tag(name = "People")
class PeopleController(
  private val offenderService: OffenderService,
  private val personTransformer: PersonTransformer,
  private val prisonCaseNoteTransformer: PrisonCaseNoteTransformer,
  private val adjudicationTransformer: AdjudicationTransformer,
  private val prisonerAlertTransformer: PrisonerAlertTransformer,
  private val oaSysSectionsTransformer: OASysSectionsTransformer,
  private val offenceTransformer: OffenceTransformer,
  private val userService: UserService,
  private val oasysService: OASysService,
  private val offenderDetailService: OffenderDetailService,
  private val caseNotesService: CaseNotesService,
) {

  @Operation(
    summary = "Searches for a Person by their CRN",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation", content = [Content(schema = Schema(implementation = Person::class))]),
      ApiResponse(responseCode = "400", description = "invalid params", content = [Content(schema = Schema(implementation = ValidationError::class))]),
      ApiResponse(responseCode = "404", description = "invalid CRN", content = [Content(schema = Schema(implementation = Problem::class))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/people/search"],
    produces = ["application/json", "application/problem+json"],
  )
  fun peopleSearchGet(
    @RequestParam crn: String,
  ): ResponseEntity<Person> {
    val user = userService.getUserForRequest()

    when (val personInfo = offenderDetailService.getPersonInfoResult(crn, user.cas1LaoStrategy())) {
      is PersonInfoResult.NotFound -> throw NotFoundProblem(crn, "Offender")
      is PersonInfoResult.Unknown -> throw personInfo.throwable ?: RuntimeException("Could not retrieve person info for CRN: $crn")
      is PersonInfoResult.Success -> return ResponseEntity.ok(
        personTransformer.personInfoResultToPerson(personInfo),
      )
    }
  }

  @Operation(
    summary = "Returns the prison case notes for a Person",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation", content = [Content(array = ArraySchema(schema = Schema(implementation = PrisonCaseNote::class)))]),
      ApiResponse(responseCode = "404", description = "invalid CRN", content = [Content(schema = Schema(implementation = Problem::class))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/people/{crn}/prison-case-notes"],
    produces = ["application/json"],
  )
  fun peopleCrnPrisonCaseNotesGet(
    @Parameter(description = "CRN of the Person to fetch prison case notes for")
    @PathVariable
    crn: String,
    @Parameter(
      description = "CAS1 requests may limit returned case note types",
      `in` = ParameterIn.HEADER,
      schema = Schema(allowableValues = ["approved-premises", "cas2", "cas2v2", "temporary-accommodation"]),
    )
    @RequestHeader("X-Service-Name")
    xServiceName: ServiceName,
  ): ResponseEntity<List<PrisonCaseNote>> {
    val nomsNumber = getNomsNumber(crn)

    val prisonCaseNotesResult = caseNotesService.getFilteredPrisonCaseNotesByNomsNumber(
      nomsNumber,
      getCas1SpecificNoteTypes = xServiceName == ServiceName.approvedPremises,
    )

    return ResponseEntity.ok(extractEntityFromCasResult(prisonCaseNotesResult).map(prisonCaseNoteTransformer::transformModelToApi))
  }

  @Operation(
    summary = "Returns the adjudications for a Person",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation", content = [Content(array = ArraySchema(schema = Schema(implementation = Adjudication::class)))]),
      ApiResponse(responseCode = "404", description = "invalid CRN", content = [Content(schema = Schema(implementation = Problem::class))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/people/{crn}/adjudications"],
    produces = ["application/json"],
  )
  fun peopleCrnAdjudicationsGet(
    @Parameter(description = "CRN of the Person to fetch adjudications for")
    @PathVariable
    crn: String,
    @Parameter(
      description = "CAS1 requests may be limited to adjudications for last 12 months only",
      `in` = ParameterIn.HEADER,
      schema = Schema(allowableValues = ["approved-premises", "cas2", "cas2v2", "temporary-accommodation"]),
    )
    @RequestHeader("X-Service-Name")
    xServiceName: ServiceName,
  ): ResponseEntity<List<Adjudication>> {
    val nomsNumber = getNomsNumber(crn)

    val adjudications = when (val adjudicationsResult = offenderService.getAdjudicationsByNomsNumber(nomsNumber)) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(crn, "Inmate")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> adjudicationsResult.entity
    }

    return ResponseEntity.ok(
      adjudicationTransformer.transformToApi(
        adjudications,
        getLast12MonthsOnly = xServiceName == ServiceName.approvedPremises,
      ),
    )
  }

  @Operation(
    summary = "Returns the ACCT alerts for a Person",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation", content = [Content(array = ArraySchema(schema = Schema(implementation = PersonAcctAlert::class)))]),
      ApiResponse(responseCode = "404", description = "invalid CRN", content = [Content(schema = Schema(implementation = Problem::class))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/people/{crn}/acct-alerts"],
    produces = ["application/json"],
  )
  fun peopleCrnAcctAlertsGet(
    @Parameter(description = "CRN of the Person to fetch ACCT alerts for")
    @PathVariable
    crn: String,
  ): ResponseEntity<List<PersonAcctAlert>> {
    val nomsNumber = getNomsNumber(crn)

    val acctAlertsResult = offenderService.getAcctPrisonerAlertsByNomsNumber(nomsNumber)

    return ResponseEntity.ok(extractEntityFromCasResult(acctAlertsResult).map(prisonerAlertTransformer::transformToApi))
  }

  @Operation(
    summary = "Returns OASys sections to support an Application. " +
      " The Supporting Information sections are returned if linked to harm and optionally if their section number appears in the selected-sections query parameter." +
      " CAS1 should use /cas1/people/CRN/oasys/answers. " +
      "CAS3 should use /cas3/people/CRN/oasys/riskManagement",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation", content = [Content(schema = Schema(implementation = OASysSections::class))]),
      ApiResponse(responseCode = "404", description = "invalid CRN", content = [Content(schema = Schema(implementation = Problem::class))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/people/{crn}/oasys/sections"],
    produces = ["application/json"],
  )
  fun peopleCrnOasysSectionsGet(
    @Parameter(description = "CRN of the Person to fetch latest OASys selection")
    @PathVariable
    crn: String,
    @RequestParam("selected-sections")
    selectedSections: List<Int>?,
  ): ResponseEntity<OASysSections> {
    ensureUserCanAccessOffenderInfo(crn)

    val needs = extractEntityFromCasResult(oasysService.getNeedsDetails(crn))

    return runBlocking(context = Dispatchers.IO) {
      val offenceDetailsResult = async {
        oasysService.getOffenceDetails(crn)
      }
      val roshSummaryResult = async {
        oasysService.getRoshSummary(crn)
      }
      val riskToTheIndividualResult = async {
        oasysService.getRiskToTheIndividual(crn)
      }
      val riskManagementPlanResult = async {
        oasysService.getRiskManagementPlan(crn)
      }

      val offenceDetails = extractEntityFromCasResult(offenceDetailsResult.await())
      val roshSummary = extractEntityFromCasResult(roshSummaryResult.await())
      val riskToTheIndividual = extractEntityFromCasResult(riskToTheIndividualResult.await())
      val riskManagementPlan = extractEntityFromCasResult(riskManagementPlanResult.await())

      ResponseEntity.ok(
        oaSysSectionsTransformer.transformToApi(
          offenceDetails,
          roshSummary,
          riskToTheIndividual,
          riskManagementPlan,
          needs,
          selectedSections ?: emptyList(),
        ),
      )
    }
  }

  @Operation(
    summary = "Returns all active offences for a Person.",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation", content = [Content(array = ArraySchema(schema = Schema(implementation = ActiveOffence::class)))]),
      ApiResponse(responseCode = "404", description = "invalid CRN", content = [Content(schema = Schema(implementation = Problem::class))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/people/{crn}/offences"],
    produces = ["application/json"],
  )
  fun peopleCrnOffencesGet(
    @Parameter(description = "CRN of the Person to fetch active offences for")
    @PathVariable
    crn: String,
  ): ResponseEntity<List<ActiveOffence>> {
    ensureUserCanAccessOffenderInfo(crn)

    val caseDetail = offenderService.getCaseDetail(crn)
    return ResponseEntity.ok(
      offenceTransformer.transformToApi(extractEntityFromCasResult(caseDetail)),
    )
  }

  private fun ensureUserCanAccessOffenderInfo(crn: String) {
    if (!offenderService.canAccessOffender(
        crn,
        laoStrategy = LaoStrategy.CheckUserAccess(userService.getDeliusUserNameForRequest()),
      )
    ) {
      throw ForbiddenProblem()
    }
  }

  private fun getNomsNumber(crn: String): String {
    val nomsNumber = when (
      val personSummaryInfoResult = offenderService.getPersonSummaryInfoResult(
        crn = crn,
        laoStrategy = LaoStrategy.CheckUserAccess(userService.getDeliusUserNameForRequest()),
      )
    ) {
      is PersonSummaryInfoResult.Success.Full -> personSummaryInfoResult.summary.nomsId
      is PersonSummaryInfoResult.Success.Restricted -> throw ForbiddenProblem()
      is PersonSummaryInfoResult.NotFound -> throw NotFoundProblem(crn, "Person")
      is PersonSummaryInfoResult.Unknown -> throw NotFoundProblem(crn, "Person")
    }

    if (nomsNumber == null) {
      throw NotFoundProblem(crn, "Noms Number")
    }

    return nomsNumber
  }
}
