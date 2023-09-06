package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.AuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem

@Service
class HttpAuthService {
  fun getDeliusPrincipalOrThrow(): AuthAwareAuthenticationToken {
    val principal = SecurityContextHolder.getContext().authentication as AuthAwareAuthenticationToken

    if (!isValidAuthSource(principal.token.claims["auth_source"])) {
      throw ForbiddenProblem()
    }

    return principal
  }

  fun getDeliusPrincipalOrNull(): AuthAwareAuthenticationToken? {
    val principal = SecurityContextHolder.getContext().authentication as? AuthAwareAuthenticationToken ?: return null

    if (!isValidAuthSource(principal.token.claims["auth_source"])) {
      return null
    }

    return principal
  }

  internal fun isValidAuthSource(authSource: Any?): Boolean {
    return listOf("delius", "nomis").contains(authSource)
  }
}
