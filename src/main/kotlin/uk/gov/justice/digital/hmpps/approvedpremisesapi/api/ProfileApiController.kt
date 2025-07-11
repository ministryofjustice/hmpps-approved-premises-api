package uk.gov.justice.digital.hmpps.approvedpremisesapi.api

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import java.util.Optional

@Controller
@RequestMapping("\${openapi.approvedPremises.base-path:}")
class ProfileApiController(
  delegate: ProfileApiDelegate?,
) : ProfileApi {
  private lateinit var delegate: ProfileApiDelegate

  init {
    this.delegate = Optional.ofNullable(delegate).orElse(object : ProfileApiDelegate {})
  }

  override fun getDelegate(): ProfileApiDelegate = delegate
}
