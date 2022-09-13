package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.AssessRisksAndNeedsApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.HMPPSTierApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonsApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.shouldNotBeReached
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.Mappa
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskTier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskWithStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RoshRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.assessrisksandneeds.RiskLevel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.Registrations
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail

@Service
class OffenderService(
  private val communityApiClient: CommunityApiClient,
  private val assessRisksAndNeedsApiClient: AssessRisksAndNeedsApiClient,
  private val hmppsTierApiClient: HMPPSTierApiClient,
  private val prisonsApiClient: PrisonsApiClient
) {
  private val ignoredRegisterTypesForFlags = listOf("RVHR", "RHRH", "RMRH", "RLRH", "MAPP")

  fun getOffenderByCrn(crn: String, userDistinguishedName: String): AuthorisableActionResult<OffenderDetailSummary> {
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

    return AuthorisableActionResult.Success(offender)
  }

  fun getInmateDetailByNomsNumber(nomsNumber: String): AuthorisableActionResult<InmateDetail> {
    val inmateDetail = when (val offenderResponse = prisonsApiClient.getInmateDetails(nomsNumber)) {
      is ClientResult.Success -> offenderResponse.body
      is ClientResult.StatusCodeFailure -> when (offenderResponse.status) {
        HttpStatus.NOT_FOUND -> return AuthorisableActionResult.NotFound()
        HttpStatus.FORBIDDEN -> return AuthorisableActionResult.Unauthorised()
        else -> offenderResponse.throwException()
      }
      is ClientResult.Failure -> offenderResponse.throwException()
      else -> shouldNotBeReached()
    }

    return AuthorisableActionResult.Success(inmateDetail)
  }

  fun getRiskByCrn(crn: String, jwt: String, userDistinguishedName: String): AuthorisableActionResult<PersonRisks> {
    return when (getOffenderByCrn(crn, userDistinguishedName)) {
      is AuthorisableActionResult.NotFound -> AuthorisableActionResult.NotFound()
      is AuthorisableActionResult.Unauthorised -> AuthorisableActionResult.Unauthorised()
      is AuthorisableActionResult.Success -> {
        val registrationsResponse = communityApiClient.getRegistrationsForOffenderCrn(crn)

        val risks = PersonRisks(
          crn = crn,
          roshRisks = getRoshRisksEnvelope(crn, jwt),
          mappa = getMappaEnvelope(registrationsResponse),
          tier = getRiskTierEnvelope(crn),
          flags = getFlagsEnvelope(registrationsResponse)
        )

        AuthorisableActionResult.Success(
          risks
        )
      }
    }
  }

  private fun getRoshRisksEnvelope(crn: String, jwt: String): RiskWithStatus<RoshRisks> {
    when (val roshRisksResponse = assessRisksAndNeedsApiClient.getRoshRisks(crn, jwt)) {
      is ClientResult.Success -> {
        val summary = roshRisksResponse.body.summary
        return RiskWithStatus(
          status = RiskStatus.Retrieved,
          value = RoshRisks(
            overallRisk = getOrThrow("overallRiskLevel") { summary.overallRiskLevel?.value },
            riskToChildren = getRiskOrThrow("Children", summary.riskInCommunity),
            riskToPublic = getRiskOrThrow("Public", summary.riskInCommunity),
            riskToKnownAdult = getRiskOrThrow("Known Adult", summary.riskInCommunity),
            riskToStaff = getRiskOrThrow("Staff", summary.riskInCommunity),
            lastUpdated = summary.assessedOn?.toLocalDate()
          )
        )
      }
      is ClientResult.StatusCodeFailure -> return if (roshRisksResponse.status == HttpStatus.NOT_FOUND) {
        RiskWithStatus(
          status = RiskStatus.NotFound,
          value = null
        )
      } else {
        RiskWithStatus(
          status = RiskStatus.Error,
          value = null
        )
      }
      is ClientResult.Failure -> return RiskWithStatus(
        status = RiskStatus.Error,
        value = null
      )
      else -> shouldNotBeReached()
    }
  }

  private fun getMappaEnvelope(registrationsResponse: ClientResult<Registrations>): RiskWithStatus<Mappa> {
    when (registrationsResponse) {
      is ClientResult.Success -> {
        return RiskWithStatus(
          value = registrationsResponse.body.registrations.firstOrNull { it.type.code == "MAPP" }?.let { registration ->
            Mappa(
              level = "CAT ${registration.registerCategory!!.code}/LEVEL ${registration.registerLevel!!.code}",
              lastUpdated = registration.registrationReviews?.filter { it.completed }?.maxOfOrNull { it.reviewDate } ?: registration.startDate
            )
          }
        )
      }
      is ClientResult.StatusCodeFailure -> return if (registrationsResponse.status == HttpStatus.NOT_FOUND) {
        RiskWithStatus(status = RiskStatus.NotFound)
      } else {
        RiskWithStatus(status = RiskStatus.Error)
      }
      is ClientResult.Failure -> {
        return RiskWithStatus(status = RiskStatus.Error)
      }
      else -> shouldNotBeReached()
    }
  }

  private fun getFlagsEnvelope(registrationsResponse: ClientResult<Registrations>): RiskWithStatus<List<String>> {
    when (registrationsResponse) {
      is ClientResult.Success -> {
        return RiskWithStatus(
          value = registrationsResponse.body.registrations.filter { !ignoredRegisterTypesForFlags.contains(it.type.code) }.map { it.type.description }
        )
      }
      is ClientResult.StatusCodeFailure -> return if (registrationsResponse.status == HttpStatus.NOT_FOUND) {
        RiskWithStatus(status = RiskStatus.NotFound)
      } else {
        RiskWithStatus(status = RiskStatus.Error)
      }
      is ClientResult.Failure -> {
        return RiskWithStatus(status = RiskStatus.Error)
      }
      else -> shouldNotBeReached()
    }
  }

  private fun getRiskTierEnvelope(crn: String): RiskWithStatus<RiskTier> {
    when (val tierResponse = hmppsTierApiClient.getTier(crn)) {
      is ClientResult.Success -> {
        return RiskWithStatus(
          status = RiskStatus.Retrieved,
          value = RiskTier(
            level = tierResponse.body.tierScore,
            lastUpdated = tierResponse.body.calculationDate.toLocalDate()
          )
        )
      }
      is ClientResult.StatusCodeFailure -> return if (tierResponse.status == HttpStatus.NOT_FOUND) {
        RiskWithStatus(status = RiskStatus.NotFound)
      } else {
        RiskWithStatus(status = RiskStatus.Error)
      }
      is ClientResult.Failure -> {
        return RiskWithStatus(status = RiskStatus.Error)
      }
      else -> shouldNotBeReached()
    }
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
