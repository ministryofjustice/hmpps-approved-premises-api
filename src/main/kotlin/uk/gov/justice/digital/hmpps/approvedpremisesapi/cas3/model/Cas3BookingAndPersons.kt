package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult

data class Cas3BookingAndPersons(
  val booking: Cas3BookingEntity,
  val personInfo: PersonInfoResult,
)
