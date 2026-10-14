package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.controller.external

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.GetMapping

@Cas2ExternalController
class Cas2ExternalUrlTemplateController(private val cas2UrlTemplates: Cas2UrlTemplates) {
  @PreAuthorize("hasRole('APPROVED_PREMISES__SINGLE_ACCOMMODATION_SERVICE')")
  @GetMapping("/url-templates")
  fun getCas2Links(): ResponseEntity<Cas2UrlTemplates> = ResponseEntity.ok(cas2UrlTemplates)
}

@Component
data class Cas2UrlTemplates(
  @Value($$"${url-templates.frontend.cas2v2.application-start}") val cas2ApplicationStart: String,
)
