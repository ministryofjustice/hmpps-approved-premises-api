package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import java.util.Optional

@Controller
@RequestMapping("\${openapi.communityAccommodationServicesApprovedPremisesCAS1.base-path:/cas1}")
class AssessmentsCas1Controller(
  delegate: AssessmentsCas1Delegate?,
) : AssessmentsCas1 {
  private lateinit var delegate: AssessmentsCas1Delegate

  init {
    this.delegate = Optional.ofNullable(delegate).orElse(object : AssessmentsCas1Delegate {})
  }

  override fun getDelegate(): AssessmentsCas1Delegate = delegate
}
