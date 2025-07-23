package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationPlaceholderEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationPlaceholderEntity

fun IntegrationTestBase.givenAPlacementApplicationPlaceholder(
  application: ApprovedPremisesApplicationEntity,
): PlacementApplicationPlaceholderEntity = placementApplicationPlaceholderRepository.save(
  PlacementApplicationPlaceholderEntityFactory()
    .withApplication(application)
    .produce(),
)
