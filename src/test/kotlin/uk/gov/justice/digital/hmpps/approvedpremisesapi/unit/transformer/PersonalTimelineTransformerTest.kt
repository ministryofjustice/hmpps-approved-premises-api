package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationTimeline
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.User
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.ApprovedPremisesUserFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.TimelineEventFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonalTimelineTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomOf
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus as DomainApplicationStatus

class PersonalTimelineTransformerTest {
  private val userTransformer = mockk<UserTransformer>()
  private val personTransformer = mockk<PersonTransformer>()

  private val personalTimelineTransformer = PersonalTimelineTransformer(userTransformer, personTransformer)

  companion object {
    private fun assertApplicationMatches(
      actual: ApplicationTimeline,
      expectedApplication: ApprovedPremisesApplicationEntity,
      expectedUser: User,
      expectedTimelineEvents: List<TimelineEvent>,
    ) {
      assertThat(actual.id).isEqualTo(expectedApplication.id)
      assertThat(actual.createdAt).isEqualTo(expectedApplication.createdAt.toInstant())
      assertThat(actual.status).isEqualTo(expectedApplication.status.apiValue)
      assertThat(actual.createdBy).isEqualTo(expectedUser)
      assertThat(actual.timelineEvents).isEqualTo(expectedTimelineEvents)
    }
  }

  @SuppressWarnings("detekt:DestructuringDeclarationWithTooManyEntries")
  @Test
  fun `transformApplicationsAndTimelineEvents creates the expected PersonalTimeline`() {
    val (application0, application1, application2, application3) = ApprovedPremisesApplicationEntityFactory()
      .withYieldedCreatedByUser {
        UserEntityFactory()
          .withDefaultProbationRegion()
          .produce()
      }
      .withStatus(randomOf(DomainApplicationStatus.entries))
      .produceMany()
      .take(4)
      .toList()

    val timelineEvents0 = emptyList<TimelineEvent>()

    val timelineEvents1 = TimelineEventFactory()
      .produceMany()
      .take(1)
      .toList()

    val timelineEvents2 = TimelineEventFactory().produceMany()
      .take(2)
      .toList()

    val timelineEvents3 = TimelineEventFactory()
      .produceMany()
      .take(3)
      .toList()

    val applicationsAndTimelineEvents = mapOf(
      application0 to timelineEvents0,
      application1 to timelineEvents1,
      application2 to timelineEvents2,
      application3 to timelineEvents3,
    )

    val (user0, user1, user2, user3) = ApprovedPremisesUserFactory()
      .produceMany()
      .take(4)
      .toList()

    val offenderDetailSummary = OffenderDetailsSummaryFactory().produce()
    val personInfoResult = PersonInfoResult.Success.Full(
      crn = offenderDetailSummary.otherIds.crn,
      offenderDetailSummary = offenderDetailSummary,
      inmateDetail = null,
    )
    val mockPerson = mockk<Person>()

    every { userTransformer.transformJpaToApi(application0.createdByUser, ServiceName.approvedPremises) } returns user0
    every { userTransformer.transformJpaToApi(application1.createdByUser, ServiceName.approvedPremises) } returns user1
    every { userTransformer.transformJpaToApi(application2.createdByUser, ServiceName.approvedPremises) } returns user2
    every { userTransformer.transformJpaToApi(application3.createdByUser, ServiceName.approvedPremises) } returns user3

    every { personTransformer.transformModelToPersonApi(personInfoResult) } returns mockPerson

    val transformedPersonalTimeline = personalTimelineTransformer
      .transformApplicationsAndTimelineEvents(personInfoResult, applicationsAndTimelineEvents)

    assertThat(transformedPersonalTimeline.applications).hasSize(4)

    assertApplicationMatches(transformedPersonalTimeline.applications[0], application0, user0, timelineEvents0)
    assertApplicationMatches(transformedPersonalTimeline.applications[1], application1, user1, timelineEvents1)
    assertApplicationMatches(transformedPersonalTimeline.applications[2], application2, user2, timelineEvents2)
    assertApplicationMatches(transformedPersonalTimeline.applications[3], application3, user3, timelineEvents3)

    assertThat(transformedPersonalTimeline.person).isEqualTo(mockPerson)

    verify(exactly = 1) { personTransformer.transformModelToPersonApi(personInfoResult) }
  }
}
