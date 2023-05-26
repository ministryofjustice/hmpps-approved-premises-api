package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.AssignedLivingUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InOutStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase

class InmateDetailFactory : Factory<InmateDetail> {
  private var offenderNo: Yielded<String> = { randomStringUpperCase(8) }
  private var inOutStatus: Yielded<InOutStatus> = { InOutStatus.OUT }
  private var assignedLivingUnit: Yielded<AssignedLivingUnit?> = { null }

  fun withOffenderNo(offenderNo: String) = apply {
    this.offenderNo = { offenderNo }
  }

  fun withInOutStatus(inOutStatus: InOutStatus) = apply {
    this.inOutStatus = { inOutStatus }
  }

  fun withAssignedLivingUnit(assignedLivingUnit: AssignedLivingUnit) = apply {
    this.assignedLivingUnit = { assignedLivingUnit }
  }

  override fun produce(): InmateDetail = InmateDetail(
    offenderNo = this.offenderNo(),
    inOutStatus = this.inOutStatus(),
    assignedLivingUnit = this.assignedLivingUnit(),
  )
}
