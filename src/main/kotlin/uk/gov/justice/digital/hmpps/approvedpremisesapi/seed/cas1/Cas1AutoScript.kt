package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationTimelinessCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedLogger
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.ensureEntityFromNestedAuthorisableValidatableActionResultIsSuccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromValidatableActionResult
import java.time.LocalDate

@SuppressWarnings("MagicNumber", "MaxLineLength")
@Component
class Cas1AutoScript(
  private val seedLogger: SeedLogger,
  private val applicationService: ApplicationService,
  private val userService: UserService,
  private val offenderService: OffenderService,
) {

  val crn = "X320741"

  fun script() {
    seedLogger.info("Auto-Scripting for CAS1")
    autoCreateApplication()
  }

  private fun autoCreateApplication() {
    if (applicationService.getApplicationsForCrn(crn, ServiceName.approvedPremises).isNotEmpty()) {
      seedLogger.info("Already have CAS1 application for $crn, not seeding new applications")
      return
    }

    seedLogger.info("Auto creating a CAS1 application")

    val personInfo =
      when (
        val personInfoResult = offenderService.getInfoForPerson(
          crn = crn,
          deliusUsername = null,
          ignoreLaoRestrictions = true,
        )
      ) {
        is PersonInfoResult.NotFound, is PersonInfoResult.Unknown -> throw NotFoundProblem(
          personInfoResult.crn,
          "Offender",
        )

        is PersonInfoResult.Success.Restricted -> throw ForbiddenProblem()
        is PersonInfoResult.Success.Full -> personInfoResult
      }

    val createdByUser = userService.getExistingUserOrCreate("JIMSNOWLDAP")

    val newApplicationEntity = extractEntityFromValidatableActionResult(
      applicationService.createApprovedPremisesApplication(
        offenderDetails = personInfo.offenderDetailSummary,
        user = createdByUser,
        convictionId = 2500295345,
        deliusEventNumber = "2",
        offenceId = "M2500295343",
        createWithRisks = true,
      ),
    )

    val updateResult = applicationService.updateApprovedPremisesApplication(
      applicationId = newApplicationEntity.id,
      updateFields = ApplicationService.Cas1ApplicationUpdateFields(
        isWomensApplication = false,
        isPipeApplication = null,
        isEmergencyApplication = false,
        isEsapApplication = null,
        apType = ApType.normal,
        releaseType = "licence",
        arrivalDate = LocalDate.of(2025, 12, 12),
        data = applicationDataFor(crn),
        isInapplicable = false,
        noticeType = Cas1ApplicationTimelinessCategory.standard,
      ),
      userForRequest = createdByUser,
    )

    ensureEntityFromNestedAuthorisableValidatableActionResultIsSuccess(updateResult)

    applicationService.addNoteToApplication(
      applicationId = newApplicationEntity.id,
      note = "Application automatically created by Cas1 Auto Script",
      user = null,
    )
  }

  private fun applicationDataFor(crn: String): String {
    return dataFixtureFor(questionnaire = "application", crn = crn)
  }

  private fun dataFixtureFor(questionnaire: String, crn: String): String {
    return loadFixtureAsResource("${questionnaire}_data_$crn.json")
  }

  private fun loadFixtureAsResource(filename: String): String {
    val path = "/db/seed/local+dev+test/approved_premises/$filename"
    return this.javaClass::class.java.getResource(path)?.readText() ?: error("Resource not found: $path")
  }
}
