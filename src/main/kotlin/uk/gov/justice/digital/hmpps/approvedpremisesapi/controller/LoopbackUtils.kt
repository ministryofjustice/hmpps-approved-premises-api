package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.UnauthenticatedProblem

fun throwIfNotLoopbackRequest() {
  val request = (RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes).request
  val remoteAddress = request.remoteAddr

  if (! listOf("127.0.0.1", "localhost").contains(remoteAddress)) {
    throw UnauthenticatedProblem("This endpoint can only be called locally, was requested from: $remoteAddress")
  }
}
