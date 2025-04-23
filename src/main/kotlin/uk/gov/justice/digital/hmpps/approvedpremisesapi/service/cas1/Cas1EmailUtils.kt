package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity

fun ApprovedPremisesApplicationEntity.interestedPartiesEmailAddresses(): Set<String> = setOfNotNull(
  createdByUser.email,
  if (caseManagerIsNotApplicant == true) {
    caseManagerUserDetails?.email
  } else {
    null
  },
)

fun PlacementApplicationEntity.interestedPartiesEmailAddresses(): Set<String> = application.interestedPartiesEmailAddresses() +
  setOfNotNull(createdByUser.email)
