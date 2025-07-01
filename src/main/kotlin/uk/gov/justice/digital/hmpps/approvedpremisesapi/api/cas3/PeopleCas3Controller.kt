package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas3

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import java.util.Optional

@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.13.0")@Controller
@RequestMapping("\${openapi.transitionalAccommodationServicesCAS3.base-path:/cas3}")
class PeopleCas3Controller(
        delegate: PeopleCas3Delegate?
) : PeopleCas3 {
    private lateinit var delegate: PeopleCas3Delegate

    init {
        this.delegate = Optional.ofNullable(delegate).orElse(object : PeopleCas3Delegate {})
    }

    override fun getDelegate(): PeopleCas3Delegate = delegate
}
