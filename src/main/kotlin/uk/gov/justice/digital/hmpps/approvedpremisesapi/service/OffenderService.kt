package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.shouldNotBeReached
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary

@Service
class OffenderService(private val communityApiClient: CommunityApiClient) {
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
}
