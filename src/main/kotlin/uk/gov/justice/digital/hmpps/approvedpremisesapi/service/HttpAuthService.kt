package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.AuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem

@Service
class HttpAuthService {

  fun getPrincipalOrThrow(acceptableSources: List<String>): AuthAwareAuthenticationToken {
    val principal = SecurityContextHolder.getContext().authentication as AuthAwareAuthenticationToken

    if (!acceptableSources.contains(principal.token.claims["auth_source"])) {
      throw ForbiddenProblem()
    }

    return principal
  }

  fun getNomisPrincipalOrThrow(): AuthAwareAuthenticationToken = getPrincipalOrThrow(listOf("nomis"))

  fun getCas2AuthenticatedPrincipalOrThrow(): AuthAwareAuthenticationToken = getPrincipalOrThrow(listOf("nomis", "auth"))

  fun getCas2v2AuthenticatedPrincipalOrThrow(): AuthAwareAuthenticationToken = getPrincipalOrThrow(listOf("nomis", "auth", "delius"))

  fun getDeliusPrincipalOrThrow(): AuthAwareAuthenticationToken = getPrincipalOrThrow(listOf("delius"))

  fun getDeliusPrincipalOrNull(): AuthAwareAuthenticationToken? {
    val principal = SecurityContextHolder.getContext().authentication as? AuthAwareAuthenticationToken
    return if (principal?.token?.claims?.get("auth_source") != "delius") null else principal
  }
}
