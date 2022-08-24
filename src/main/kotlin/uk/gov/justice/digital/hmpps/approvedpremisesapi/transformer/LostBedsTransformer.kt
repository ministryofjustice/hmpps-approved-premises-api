package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LostBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LostBedReasons
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewLostBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import java.util.UUID

@Component
class LostBedsTransformer {
  fun transformJpaToApi(jpa: LostBedsEntity) = LostBed(
    id = jpa.id,
    startDate = jpa.startDate,
    endDate = jpa.endDate,
    numberOfBeds = jpa.numberOfBeds,
    reason = transformReasonFromJpaToApi(jpa.reason),
    referenceNumber = jpa.referenceNumber,
    notes = jpa.notes
  )

  fun transformApiToJpa(api: NewLostBed, premisesEntity: PremisesEntity) = LostBedsEntity(
    id = UUID.randomUUID(),
    startDate = api.startDate,
    endDate = api.endDate,
    numberOfBeds = api.numberOfBeds,
    reason = transformReasonFromApiToJpa(api.reason),
    referenceNumber = api.referenceNumber,
    notes = api.notes,
    premises = premisesEntity
  )

  private fun transformReasonFromJpaToApi(reason: LostBedReason): LostBedReasons = when (reason) {
    LostBedReason.Damaged -> LostBedReasons.damaged
    LostBedReason.Fire -> LostBedReasons.fire
    LostBedReason.Refurbishment -> LostBedReasons.refurbishment
    LostBedReason.StaffShortage -> LostBedReasons.staffShortage
  }

  private fun transformReasonFromApiToJpa(reason: LostBedReasons): LostBedReason = when (reason) {
    LostBedReasons.damaged -> LostBedReason.Damaged
    LostBedReasons.fire -> LostBedReason.Fire
    LostBedReasons.refurbishment -> LostBedReason.Refurbishment
    LostBedReasons.staffShortage -> LostBedReason.StaffShortage
  }
}
