package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.givens.givenACas3Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspacesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import java.time.LocalDate

fun IntegrationTestBase.givenCas3PremisesAndBedspace(
  user: UserEntity,
  startDate: LocalDate = LocalDate.now(),
  premises: Cas3PremisesEntity = givenACas3Premises(
    probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
      withProbationRegion(user.probationRegion)
    },
  ),
  bedspace: Cas3BedspacesEntity = cas3BedspaceEntityFactory.produceAndPersist {
    withReference("test-bed")
    withPremises(premises)
    withStartDate(startDate)
  },
) = Pair(premises, bedspace)
