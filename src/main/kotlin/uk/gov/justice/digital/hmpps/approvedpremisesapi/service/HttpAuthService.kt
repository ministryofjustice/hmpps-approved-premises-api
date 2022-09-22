package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.AuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem

@Service
class HttpAuthService {
  fun getDeliusPrincipalOrThrow(): AuthAwareAuthenticationToken {
    val principal = SecurityContextHolder.getContext().authentication as AuthAwareAuthenticationToken

    if (principal.token.claims["auth_source"] != "delius") {
      throw ForbiddenProblem()
    }

    return principal
  }
}
