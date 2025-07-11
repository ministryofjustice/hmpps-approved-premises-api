package uk.gov.justice.digital.hmpps.approvedpremisesapi.api

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import java.util.Optional

@Controller
@RequestMapping("\${openapi.approvedPremises.base-path:}")
class PlacementRequestsApiController(
  delegate: PlacementRequestsApiDelegate?,
) : PlacementRequestsApi {
  private lateinit var delegate: PlacementRequestsApiDelegate

  init {
    this.delegate = Optional.ofNullable(delegate).orElse(object : PlacementRequestsApiDelegate {})
  }

  override fun getDelegate(): PlacementRequestsApiDelegate = delegate
}
