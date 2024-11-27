package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseBody

@Controller
internal class FaviconController {
  @GetMapping("favicon.ico")
  @ResponseBody
  fun returnNoFavicon() {
    // deliberately empty
  }
}
