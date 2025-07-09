package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas2v2

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import java.util.Optional

@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.13.0")@Controller
@RequestMapping("\${openapi.communityAccommodationServicesTier2CAS2Version2.base-path:/cas2v2}")
class SubmissionsCas2v2Controller(
        delegate: SubmissionsCas2v2Delegate?
) : SubmissionsCas2v2 {
    private lateinit var delegate: SubmissionsCas2v2Delegate

    init {
        this.delegate = Optional.ofNullable(delegate).orElse(object : SubmissionsCas2v2Delegate {})
    }

    override fun getDelegate(): SubmissionsCas2v2Delegate = delegate
}
