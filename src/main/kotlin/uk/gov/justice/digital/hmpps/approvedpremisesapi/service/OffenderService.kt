package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.AssessRisksAndNeedsApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.HMPPSTierApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.shouldNotBeReached
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.Mappa
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskTier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RoshRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.assessrisksandneeds.RiskLevel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import java.time.LocalDate

@Service
class OffenderService(
  private val communityApiClient: CommunityApiClient,
  private val assessRisksAndNeedsApiClient: AssessRisksAndNeedsApiClient,
  private val hmppsTierApiClient: HMPPSTierApiClient
) {
  fun getOffenderByCrn(crn: String, userDistinguishedName: String): OffenderDetailSummary? {
    val offender = when (val offenderResponse = communityApiClient.getOffenderDetailSummary(crn)) {
      is ClientResult.Success -> offenderResponse.body
      is ClientResult.StatusCodeFailure -> if (offenderResponse.status == HttpStatus.NOT_FOUND) return null else offenderResponse.throwException()
      is ClientResult.Failure -> offenderResponse.throwException()
      else -> shouldNotBeReached()
    }

    if (offender.currentExclusion || offender.currentRestriction) {
      val access =
        when (val accessResponse = communityApiClient.getUserAccessForOffenderCrn(userDistinguishedName, crn)) {
          is ClientResult.Success -> accessResponse.body
          is ClientResult.Failure -> accessResponse.throwException()
          else -> shouldNotBeReached()
        }

      if (access.userExcluded || access.userRestricted) {
        throw ForbiddenProblem()
      }
    }

    return offender
  }

  fun getRiskByCrn(crn: String, jwt: String, userDistinguishedName: String): AuthorisableActionResult<PersonRisks> {
    // TODO: Move this into another function maybe?   Not sure if it'll be possible to do nicely with all the wheres
    val offender = when (val offenderResponse = communityApiClient.getOffenderDetailSummary(crn)) {
      is ClientResult.Success -> offenderResponse.body
      is ClientResult.StatusCodeFailure -> if (offenderResponse.status == HttpStatus.NOT_FOUND) return AuthorisableActionResult.NotFound() else offenderResponse.throwException()
      is ClientResult.Failure -> offenderResponse.throwException()
      else -> shouldNotBeReached()
    }

    if (offender.currentExclusion || offender.currentRestriction) {
      val access =
        when (val accessResponse = communityApiClient.getUserAccessForOffenderCrn(userDistinguishedName, crn)) {
          is ClientResult.Success -> accessResponse.body
          is ClientResult.Failure -> accessResponse.throwException()
          else -> shouldNotBeReached()
        }

      if (access.userExcluded || access.userRestricted) {
        return AuthorisableActionResult.Unauthorised()
      }
    }
    // End TODO

    // TODO: Don't just fail if we can't get one of the responses

    val roshRisks = when (val roshRisksResponse = assessRisksAndNeedsApiClient.getRoshRisks(crn, jwt)) {
      is ClientResult.Success -> roshRisksResponse.body
      is ClientResult.StatusCodeFailure -> if (roshRisksResponse.status == HttpStatus.FORBIDDEN) {
        return AuthorisableActionResult.Unauthorised()
      } else {
        roshRisksResponse.throwException()
      }
      is ClientResult.Failure -> roshRisksResponse.throwException()
      else -> shouldNotBeReached()
    }

    val tier = when (val tierResponse = hmppsTierApiClient.getTier(crn)) {
      is ClientResult.Success -> tierResponse.body
      is ClientResult.StatusCodeFailure -> if (tierResponse.status == HttpStatus.FORBIDDEN) {
        return AuthorisableActionResult.Unauthorised()
      } else {
        tierResponse.throwException()
      }
      is ClientResult.Failure -> tierResponse.throwException()
      else -> shouldNotBeReached()
    }

    // TODO: Get MAPPA from respective service

    return AuthorisableActionResult.Success(
      PersonRisks(
        crn = crn,
        roshRisks = RoshRisks(
          overallRisk = getOrThrow("overallRiskLevel") { roshRisks.summary.overallRiskLevel?.value },
          riskToChildren = getRiskOrThrow("Children", roshRisks.summary.riskInCommunity),
          riskToPublic = getRiskOrThrow("Public", roshRisks.summary.riskInCommunity),
          riskToKnownAdult = getRiskOrThrow("Known Adult", roshRisks.summary.riskInCommunity),
          riskToStaff = getRiskOrThrow("Staff", roshRisks.summary.riskInCommunity),
          lastUpdated = roshRisks.summary.assessedOn?.toLocalDate()
        ),
        mappa = Mappa(
          level = "",
          isNominal = false,
          lastUpdated = LocalDate.now() // TODO: Actually get from MAPPA
        ),
        tier = RiskTier(
          level = tier.tierScore,
          lastUpdated = tier.calculationDate.toLocalDate()
        )
      )
    )
  }

  private fun <T> getOrThrow(thing: String, getter: () -> T?): T {
    return getter() ?: throw RuntimeException("Value unexpectedly missing when getting $thing")
  }

  private fun getRiskOrThrow(category: String, risks: Map<RiskLevel?, List<String>>): String {
    risks.forEach {
      if (it.value.contains(category)) {
        return it.key?.value ?: throw RuntimeException("Risk level unexpectedly null when getting $category")
      }
    }

    throw RuntimeException("Category not present in any Risk level when getting $category")
  }
}
