package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.controller.external

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.GetMapping

@EnableConfigurationProperties(Cas3UrlTemplates::class)
@Cas3ExternalController
class Cas3ExternalUrlTemplateController(private val cas3UrlTemplates: Cas3UrlTemplates) {
  @PreAuthorize("hasRole('APPROVED_PREMISES__SINGLE_ACCOMMODATION_SERVICE')")
  @GetMapping("/url-templates")
  fun getCas1Links(): ResponseEntity<Cas3UrlTemplates> = ResponseEntity.ok(cas3UrlTemplates)
}

@Component
@ConfigurationProperties(prefix = "url-templates.frontend.cas3")
data class Cas3UrlTemplates(var applicationStart: String = "")
