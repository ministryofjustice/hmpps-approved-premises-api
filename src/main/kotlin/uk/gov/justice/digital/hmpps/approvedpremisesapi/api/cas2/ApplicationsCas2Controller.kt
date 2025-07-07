package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas2

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import java.util.Optional

@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.13.0")@Controller
@RequestMapping("\${openapi.communityAccommodationServicesTier2CAS2.base-path:/cas2}")
class ApplicationsCas2Controller(
        delegate: ApplicationsCas2Delegate?
) : ApplicationsCas2 {
    private lateinit var delegate: ApplicationsCas2Delegate

    init {
        this.delegate = Optional.ofNullable(delegate).orElse(object : ApplicationsCas2Delegate {})
    }

    override fun getDelegate(): ApplicationsCas2Delegate = delegate
}
