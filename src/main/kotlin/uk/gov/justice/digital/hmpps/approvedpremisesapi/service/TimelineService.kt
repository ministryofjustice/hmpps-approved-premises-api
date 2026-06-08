package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.javers.core.Javers
import org.javers.repository.jql.QueryBuilder
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineRecordV2
import java.util.UUID

@Service
class TimelineService(
  private val javers: Javers,
  private val timelineMapperV2: TimelineMapperV2,
) {

  fun getTimelineRecordsForSpaceBooking(
    spaceBookingId: UUID,
  ): List<TimelineRecordV2> {
    val query = QueryBuilder.anyDomainObject()
      .withCommitProperty(
        COMMIT_PROPERTY_SPACE_BOOKING_ID,
        spaceBookingId.toString(),
      )
      .limit(200)
      .build()

    return javers.findChanges(query)
      .groupByCommit()
      .map { timelineMapperV2.toTimelineRecord(it) }
  }

  companion object {
    private const val COMMIT_PROPERTY_SPACE_BOOKING_ID = "spaceBookingId"
  }
}
