package uk.gov.justice.digital.hmpps.approvedpremisesapi.problem

import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.zalando.problem.StatusType
import org.zalando.problem.ThrowableProblem
import org.zalando.problem.spring.common.AdviceTrait
import org.zalando.problem.spring.web.advice.ProblemHandling

@ControllerAdvice
class ExceptionHandling : ProblemHandling {
  override fun toProblem(throwable: Throwable, status: StatusType): ThrowableProblem? {
    if (throwable is AuthenticationCredentialsNotFoundException) {
      return UnauthenticatedProblem()
    }

    if (throwable is AccessDeniedException) {
      return ForbiddenProblem()
    }

    return AdviceTraitDefault().toProblem(throwable, status)
  }
}

private class AdviceTraitDefault : AdviceTrait
