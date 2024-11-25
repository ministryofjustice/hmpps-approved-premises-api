package uk.gov.justice.digital.hmpps.approvedpremisesapi.api

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import java.util.Optional

@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.7.0")
@Controller
@RequestMapping("\${openapi.aPDomainEvents.base-path:}")
class EventsApiController(
  @org.springframework.beans.factory.annotation.Autowired(required = false) delegate: EventsApiDelegate?,
) : EventsApi {
  private lateinit var delegate: EventsApiDelegate

  init {
    this.delegate = Optional.ofNullable(delegate).orElse(object : EventsApiDelegate {})
  }

  override fun getDelegate(): EventsApiDelegate = delegate
}
