package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.controller

import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@RestController
@RequestMapping(
  "\${api.base-path:}/cas2",
  produces = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_PROBLEM_JSON_VALUE],
)
@Parameter(
  name = "X-Service-Name",
  `in` = ParameterIn.HEADER,
  description = "ServiceName for the CAS",
  required = true,
  content = [Content(schema = Schema(implementation = ServiceName::class))],
)
internal annotation class Cas2Controller
