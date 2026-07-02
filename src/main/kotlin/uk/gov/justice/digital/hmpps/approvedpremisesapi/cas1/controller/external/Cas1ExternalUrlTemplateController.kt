package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.controller.external

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.GetMapping

@EnableConfigurationProperties(Cas1UrlTemplates::class)
@Cas1ExternalController
class Cas1ExternalUrlTemplateController(private val cas1UrlTemplates: Cas1UrlTemplates) {
  @PreAuthorize("hasRole('APPROVED_PREMISES__SINGLE_ACCOMMODATION_SERVICE')")
  @GetMapping("/url-templates")
  fun getCas1Links(): ResponseEntity<Cas1UrlTemplates> = ResponseEntity.ok(cas1UrlTemplates)
}

@Component
@ConfigurationProperties(prefix = "url-templates.frontend.cas1")
data class Cas1UrlTemplates(var applicationStart: String = "")
