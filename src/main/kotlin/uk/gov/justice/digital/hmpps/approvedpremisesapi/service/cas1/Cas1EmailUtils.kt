package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity

fun ApprovedPremisesApplicationEntity.interestedPartiesEmailAddresses(): List<String> {
  return listOfNotNull(
    this.createdByUser.email,
    if (this.caseManagerIsNotApplicant == true) { this.caseManagerUserDetails?.email } else { null },
  )
}
