package uk.gov.justice.digital.hmpps.approvedpremisesapi.api

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import java.util.Optional

@Controller
@RequestMapping("\${openapi.approvedPremises.base-path:}")
class SeedApiController(
  delegate: SeedApiDelegate?,
) : SeedApi {
  private lateinit var delegate: SeedApiDelegate

  init {
    this.delegate = Optional.ofNullable(delegate).orElse(object : SeedApiDelegate {})
  }

  override fun getDelegate(): SeedApiDelegate = delegate
}
