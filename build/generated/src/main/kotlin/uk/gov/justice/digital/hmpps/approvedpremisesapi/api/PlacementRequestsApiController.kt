package uk.gov.justice.digital.hmpps.approvedpremisesapi.api

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import java.util.Optional

@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.7.0")
@Controller
@RequestMapping("\${openapi.approvedPremises.base-path:}")
class PlacementRequestsApiController(
        @org.springframework.beans.factory.annotation.Autowired(required = false) delegate: PlacementRequestsApiDelegate?
) : PlacementRequestsApi {
    private lateinit var delegate: PlacementRequestsApiDelegate

    init {
        this.delegate = Optional.ofNullable(delegate).orElse(object : PlacementRequestsApiDelegate {})
    }

    override fun getDelegate(): PlacementRequestsApiDelegate = delegate
}
