package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationTimeline
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonalTimeline
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult

@Component
class PersonalTimelineTransformer(
  private val userTransformer: UserTransformer,
  private val personTransformer: PersonTransformer,
) {
  fun transformApplicationTimelineModels(
    personInfoResult: PersonInfoResult,
    applicationTimelineModels: List<ApplicationTimelineModel>,
  ) = PersonalTimeline(
    person = personTransformer.transformModelToPersonApi(personInfoResult),
    applications = applicationTimelineModels.map { transformApplication(it.application, it.timelineEvents) },
  )

  private fun transformApplication(
    application: BoxedApplication,
    timelineEvents: List<TimelineEvent>,
  ) = application.map(
    { transformApplication(it, timelineEvents) },
    { transformApplication(it, timelineEvents) },
  )

  private fun transformApplication(
    application: ApprovedPremisesApplicationEntity,
    timelineEvents: List<TimelineEvent>,
  ) = ApplicationTimeline(
    id = application.id,
    createdAt = application.createdAt.toInstant(),
    status = application.status.apiValue,
    isOfflineApplication = false,
    createdBy = userTransformer.transformJpaToApi(application.createdByUser, ServiceName.approvedPremises),
    timelineEvents = timelineEvents,
  )

  private fun transformApplication(
    application: OfflineApplicationEntity,
    timelineEvents: List<TimelineEvent>,
  ) = ApplicationTimeline(
    id = application.id,
    createdAt = application.createdAt.toInstant(),
    status = null,
    isOfflineApplication = true,
    createdBy = null,
    timelineEvents = timelineEvents,
  )
}

data class ApplicationTimelineModel(
  val application: BoxedApplication,
  val timelineEvents: List<TimelineEvent>,
)

sealed interface BoxedApplication {
  val value: Any

  fun <T> map(
    regularApplicationFunc: (ApprovedPremisesApplicationEntity) -> T,
    offlineApplicationFunc: (OfflineApplicationEntity) -> T,
  ) = when (this) {
    is Regular -> regularApplicationFunc(this.value)
    is Offline -> offlineApplicationFunc(this.value)
  }

  data class Regular(override val value: ApprovedPremisesApplicationEntity) : BoxedApplication
  data class Offline(override val value: OfflineApplicationEntity) : BoxedApplication

  companion object {
    fun of(value: ApprovedPremisesApplicationEntity) = Regular(value)
    fun of(value: OfflineApplicationEntity) = Offline(value)
  }
}
