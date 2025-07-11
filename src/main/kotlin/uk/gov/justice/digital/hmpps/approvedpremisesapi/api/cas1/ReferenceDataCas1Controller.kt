package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import java.util.Optional

@Controller
@RequestMapping("\${openapi.communityAccommodationServicesApprovedPremisesCAS1.base-path:/cas1}")
class ReferenceDataCas1Controller(
  delegate: ReferenceDataCas1Delegate?,
) : ReferenceDataCas1 {
  private lateinit var delegate: ReferenceDataCas1Delegate

  init {
    this.delegate = Optional.ofNullable(delegate).orElse(object : ReferenceDataCas1Delegate {})
  }

  override fun getDelegate(): ReferenceDataCas1Delegate = delegate
}
