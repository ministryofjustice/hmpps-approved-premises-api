package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas2

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import java.util.Optional

@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.13.0")@Controller
@RequestMapping("\${openapi.communityAccommodationServicesTier2CAS2.base-path:/cas2}")
class ReferenceDataCas2Controller(
        delegate: ReferenceDataCas2Delegate?
) : ReferenceDataCas2 {
    private lateinit var delegate: ReferenceDataCas2Delegate

    init {
        this.delegate = Optional.ofNullable(delegate).orElse(object : ReferenceDataCas2Delegate {})
    }

    override fun getDelegate(): ReferenceDataCas2Delegate = delegate
}
