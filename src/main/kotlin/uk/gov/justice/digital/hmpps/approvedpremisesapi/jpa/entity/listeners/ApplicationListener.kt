package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.listeners

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus

@Component
class ApplicationListener {

  fun preUpdate(application: ApprovedPremisesApplicationEntity) {
    if (application.isInapplicable == true) {
      application.status = ApprovedPremisesApplicationStatus.INAPPLICABLE
    }

    if (application.isWithdrawn) {
      application.status = ApprovedPremisesApplicationStatus.WITHDRAWN
    }
  }
}
