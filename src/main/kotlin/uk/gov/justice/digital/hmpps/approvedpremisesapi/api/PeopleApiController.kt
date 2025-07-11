package uk.gov.justice.digital.hmpps.approvedpremisesapi.api

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import java.util.Optional

@Controller
@RequestMapping("\${openapi.approvedPremises.base-path:}")
class PeopleApiController(
  delegate: PeopleApiDelegate?,
) : PeopleApi {
  private lateinit var delegate: PeopleApiDelegate

  init {
    this.delegate = Optional.ofNullable(delegate).orElse(object : PeopleApiDelegate {})
  }

  override fun getDelegate(): PeopleApiDelegate = delegate
}
