package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DatePeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Withdrawable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawableType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity

@Component
class PlacementApplicationTransformer(
  private val objectMapper: ObjectMapper,
) {
  fun transformJpaToApi(jpa: PlacementApplicationEntity): PlacementApplication {
    val latestAssessment = jpa.application.getLatestAssessment()!!

    return PlacementApplication(
      id = jpa.id,
      applicationId = jpa.application.id,
      applicationCompletedAt = jpa.application.submittedAt!!.toInstant(),
      assessmentId = latestAssessment.id,
      assessmentCompletedAt = latestAssessment.submittedAt!!.toInstant(),
      createdByUserId = jpa.createdByUser.id,
      schemaVersion = jpa.schemaVersion.id,
      createdAt = jpa.createdAt.toInstant(),
      data = if (jpa.data != null) objectMapper.readTree(jpa.data) else null,
      document = if (jpa.document != null) objectMapper.readTree(jpa.document) else null,
      outdatedSchema = !jpa.schemaUpToDate,
      submittedAt = jpa.submittedAt?.toInstant(),
      canBeWithdrawn = jpa.canBeWithdrawn(),
      isWithdrawn = jpa.isWithdrawn(),
    )
  }

  fun transformToWithdrawable(placementApplication: PlacementApplicationEntity): Withdrawable = Withdrawable(
    placementApplication.id,
    WithdrawableType.placementApplication,
    placementApplication.placementDates.map {
      DatePeriod(it.expectedArrival, it.expectedDeparture())
    },
  )
}
