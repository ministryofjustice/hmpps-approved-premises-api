package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas3

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedLogger
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import javax.transaction.Transactional

@SuppressWarnings("MagicNumber", "MaxLineLength")
@Component
class Cas3AutoScript(
  private val seedLogger: SeedLogger,
  private val applicationService: ApplicationService,
  private val userService: UserService,
  private val offenderService: OffenderService,
) {

  @SuppressWarnings("TooGenericExceptionCaught")
  @Transactional
  fun script() {
    seedLogger.info("Auto-Scripting for CAS3")

    seedUsers()

    createApplication(deliusUserName = "JIMSNOWLDAP", crn = "X320741")
    createApplication(deliusUserName = "LAOFULLACCESS", crn = "X400000")
    createApplication(deliusUserName = "LAOFULLACCESS", crn = "X400001")
  }

  @SuppressWarnings("TooGenericExceptionCaught")
  private fun createApplication(deliusUserName: String, crn: String) {
    try {
      createApplicationInternal(deliusUserName = deliusUserName, crn = crn)
    } catch (e: Exception) {
      seedLogger.error("Creating application with crn $crn failed", e)
    }
  }

  private fun seedUsers() {
    usersToSeed().forEach { seedUser(it) }
  }

  @SuppressWarnings("TooGenericExceptionCaught")
  private fun seedUser(seedUser: SeedUser) {
    try {
      val user = userService
        .getExistingUserOrCreate(username = seedUser.username, throwExceptionOnStaffRecordNotFound = true)
        .user

      user?.let {
        seedUser.roles.forEach { role ->
          userService.addRoleToUser(user = user, role = role)
        }
        val roles = user.roles.map { it.role }.joinToString(", ")
        seedLogger.info("  -> User '${user.name}' (${user.deliusUsername}) seeded with roles $roles")
      }
    } catch (e: Exception) {
      seedLogger.error("Seeding user with ${seedUser.username} failed", e)
    }
  }

  private fun usersToSeed(): List<SeedUser> {
    return listOf(
      SeedUser(
        username = "JIMSNOWLDAP",
        roles = listOf(
          UserRole.CAS3_ASSESSOR,
        ),
        documentation = "For local use in development and testing",
      ),
      SeedUser(
        username = "LAOFULLACCESS",
        roles = listOf(
          UserRole.CAS3_REFERRER,
        ),
        documentation = "For local use in development and testing. This user has an exclusion (whitelisted) for LAO CRN X400000",
      ),
    )
  }

  private fun createApplicationInternal(deliusUserName: String, crn: String) {
    if (applicationService.getApplicationsForCrn(crn, ServiceName.temporaryAccommodation).isNotEmpty()) {
      seedLogger.info("Already have CAS3 application for $crn, not seeding new applications")
      return
    }

    seedLogger.info("Auto creating a CAS3 application for $crn")

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

    val createdByUser = userService.getExistingUserOrCreate(deliusUserName)

    applicationService.createTemporaryAccommodationApplication(
      crn = crn,
      user = createdByUser,
      convictionId = 2500295345,
      deliusEventNumber = "2",
      offenceId = "M2500295343",
      createWithRisks = true,
      personInfo = personInfo,
    )
  }
}

data class SeedUser(
  val username: String,
  val roles: List<UserRole>,
  val documentation: String,
)
