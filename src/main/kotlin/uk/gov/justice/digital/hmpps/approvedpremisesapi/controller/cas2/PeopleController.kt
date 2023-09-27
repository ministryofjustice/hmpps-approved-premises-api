package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas2

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas2.PeopleCas2Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysRiskToSelf
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.NomisUserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.OASysSectionsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService as OASysOffenderService

@Service("Cas2PeopleController")
class PeopleController(
  private val offenderService: OffenderService,
  private val oaSysOffenderService: OASysOffenderService,
  private val oaSysSectionsTransformer: OASysSectionsTransformer,
  private val personTransformer: PersonTransformer,
  private val userService: NomisUserService,
) : PeopleCas2Delegate {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun peopleSearchGet(crn: String): ResponseEntity<Person> {
    val user = userService.getUserForRequest()

    val personInfo = offenderService.getInfoForPerson(crn, user.nomisUsername)

    when (personInfo) {
      is PersonInfoResult.NotFound -> throw NotFoundProblem(crn, "Offender")
      is PersonInfoResult.Unknown -> throw personInfo.throwable ?: RuntimeException("Could not retrieve person info for CRN: $crn")
      is PersonInfoResult.Success -> return ResponseEntity.ok(
        personTransformer.transformModelToPersonApi(personInfo),
      )
    }
  }

  override fun peopleCrnOasysRiskToSelfGet(crn: String): ResponseEntity<OASysRiskToSelf> {
    getOffenderDetails(crn)

    return runBlocking(context = Dispatchers.IO) {
      val offenceDetailsResult = async {
        oaSysOffenderService.getOASysOffenceDetails(crn)
      }

      val riskToTheIndividualResult = async {
        oaSysOffenderService.getOASysRiskToTheIndividual(crn)
      }

      val offenceDetails = getSuccessEntityOrThrow(crn, offenceDetailsResult.await())
      val riskToTheIndividual = getSuccessEntityOrThrow(crn, riskToTheIndividualResult.await())

      ResponseEntity.ok(
        oaSysSectionsTransformer.transformRiskToIndividual(offenceDetails, riskToTheIndividual),
      )
    }
  }

  private fun getOffenderDetails(crn: String): OffenderDetailSummary {
    val offenderDetails = when (val offenderDetailsResult = offenderService.getOffenderByCrn(crn)) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(crn, "Person")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> offenderDetailsResult.entity
    }

    return offenderDetails
  }

  private fun <T> getSuccessEntityOrThrow(crn: String, authorisableActionResult: AuthorisableActionResult<T>): T = when (authorisableActionResult) {
    is AuthorisableActionResult.NotFound -> throw NotFoundProblem(crn, "Person")
    is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
    is AuthorisableActionResult.Success -> authorisableActionResult.entity
  }
}
