package uk.gov.justice.digital.hmpps.approvedpremisesapi.api

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import java.util.Optional

@Controller
@RequestMapping("\${openapi.approvedPremises.base-path:}")
class PremisesApiController(
  delegate: PremisesApiDelegate?,
) : PremisesApi {
  private lateinit var delegate: PremisesApiDelegate

  init {
    this.delegate = Optional.ofNullable(delegate).orElse(object : PremisesApiDelegate {})
  }

  override fun getDelegate(): PremisesApiDelegate = delegate
}
