package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.subjectaccessrequests.SubjectAccessRequestService

@RestController
@RequestMapping(
  "\${api.base-path:}/subject-access-request",
  produces = [MediaType.TEXT_PLAIN_VALUE],
)
class SubjectAccessRequestController(
  private val subjectAccessRequestService: SubjectAccessRequestService,
) {
  @PreAuthorize("hasRole('SAR_DATA_ACCESS')")
  @GetMapping("/template")
  fun getTemplate(): String = subjectAccessRequestService.getTemplate()
}
