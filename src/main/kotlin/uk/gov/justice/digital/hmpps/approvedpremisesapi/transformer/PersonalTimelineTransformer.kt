package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationTimeline
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonalTimeline
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity

@Component
class PersonalTimelineTransformer(
  private val userTransformer: UserTransformer,
) {
  fun transformApplicationsAndTimelineEvents(
    applicationsAndTimelineEvents: Map<ApprovedPremisesApplicationEntity, List<TimelineEvent>>,
  ) = PersonalTimeline(
    applications = applicationsAndTimelineEvents.map { transformApplication(it.key, it.value) },
  )

  private fun transformApplication(
    application: ApprovedPremisesApplicationEntity,
    timelineEvents: List<TimelineEvent>,
  ) = ApplicationTimeline(
    id = application.id,
    createdAt = application.createdAt.toInstant(),
    status = application.status.apiValue,
    createdBy = userTransformer.transformJpaToApi(application.createdByUser, ServiceName.approvedPremises),
    timelineEvents = timelineEvents,
  )
}
