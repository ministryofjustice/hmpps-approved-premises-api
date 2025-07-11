package uk.gov.justice.digital.hmpps.approvedpremisesapi.api

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import java.util.Optional

@Controller
@RequestMapping("\${openapi.approvedPremises.base-path:}")
class CacheApiController(
  delegate: CacheApiDelegate?,
) : CacheApi {
  private lateinit var delegate: CacheApiDelegate

  init {
    this.delegate = Optional.ofNullable(delegate).orElse(object : CacheApiDelegate {})
  }

  override fun getDelegate(): CacheApiDelegate = delegate
}
