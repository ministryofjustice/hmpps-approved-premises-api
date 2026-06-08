package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.javers.core.Javers
import org.javers.repository.jql.QueryBuilder
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineRecord
import java.util.UUID

@Service
class JaversTimelineService(
  private val javers: Javers,
  private val timelineMapper: TimeLineMapper,
) {

  fun getTimelineRecordsForSpaceBooking(spaceBookingId: UUID): List<TimelineRecord> {
    val query = QueryBuilder.anyDomainObject()
      .withCommitProperty(COMMIT_PROPERTY_SPACE_BOOKING_ID, spaceBookingId.toString())
      .limit(200)
      .build()

    return javers.findChanges(query)
      .groupByCommit()
      .map { changesByCommit ->
        TimelineRecord(
          author = changesByCommit.commit.author,
          commitDate = changesByCommit.commit.commitDateInstant,
          sections = timelineMapper.buildSections(changesByCommit.get()),
        )
      }
  }

  companion object {
    private const val COMMIT_PROPERTY_SPACE_BOOKING_ID = "spaceBookingId"
  }
}
