package uk.gov.justice.digital.hmpps.approvedpremisesapi.api

import jakarta.annotation.Generated
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import java.util.Optional

@Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.13.0")@Controller
@RequestMapping("\${openapi.cAS2DomainEvents.base-path:}")
class CAS2EventsApiController(
        delegate: CAS2EventsApiDelegate?
) : CAS2EventsApi {
    private lateinit var delegate: CAS2EventsApiDelegate

    init {
        this.delegate = Optional.ofNullable(delegate).orElse(object : CAS2EventsApiDelegate {})
    }

    override fun getDelegate(): CAS2EventsApiDelegate = delegate
}
