package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateBefore
import java.time.LocalDate

@SuppressWarnings("LongParameterList")
fun IntegrationTestBase.`Given a Booking`(
  crn: String,
  application: ApplicationEntity,
  premises: PremisesEntity? = null,
  arrivalDate: LocalDate = LocalDate.now().randomDateBefore(14),
  departureDate: LocalDate = LocalDate.now().randomDateBefore(14),
  adhoc: Boolean = false,
) = bookingEntityFactory.produceAndPersist {
  withCrn(crn)
  withPremises(premises ?: approvedPremisesEntityFactory.produceAndPersist())
  withApplication(application)
  withArrivalDate(arrivalDate)
  withDepartureDate(departureDate)
  withAdhoc(adhoc)
}

@SuppressWarnings("LongParameterList")
fun IntegrationTestBase.`Given a Booking for an Offline Application`(
  crn: String,
  offlineApplication: OfflineApplicationEntity,
  premises: PremisesEntity? = null,
  arrivalDate: LocalDate = LocalDate.now().randomDateBefore(14),
  departureDate: LocalDate = LocalDate.now().randomDateBefore(14),
) = bookingEntityFactory.produceAndPersist {
  withCrn(crn)
  withPremises(premises ?: approvedPremisesEntityFactory.produceAndPersist())
  withApplication(null)
  withOfflineApplication(offlineApplication)
  withArrivalDate(arrivalDate)
  withDepartureDate(departureDate)
  withAdhoc(true)
}
