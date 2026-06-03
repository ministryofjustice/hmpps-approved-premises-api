package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.controller.external

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@RestController
@RequestMapping(
  value = [ "\${api.base-path:}/cas2-hdc/external"],
  produces = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_PROBLEM_JSON_VALUE],
)
internal annotation class Cas2ExternalController
