package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspacesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import java.time.LocalDate

fun IntegrationTestBase.givenCas3PremiseBedspace(
  premises: Cas3PremisesEntity,
  startDate: LocalDate = LocalDate.now(),
  bedspace: Cas3BedspacesEntity = cas3BedspaceEntityFactory.produceAndPersist {
    withReference("test-bed")
    withPremises(premises)
    withStartDate(startDate)
  },
) = bedspace
