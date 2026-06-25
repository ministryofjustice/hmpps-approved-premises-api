package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.hmppstier.Tier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.domainevent.listener.InboxEventDispatcher
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.domainevent.listener.TierCalculationChangedHandler.Companion.TIER_CALCULATION_EVENT_TYPE
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.factory.HmppsDomainEventFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.hmppsTierMock404TierCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.hmppsTierMock500TierCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.hmppsTierMockSuccessfulTierCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.PersonReference
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingTopicException
import java.time.LocalDateTime
import java.util.UUID

class TierCalculationChangedTest : IntegrationTestBase() {

  @Autowired
  lateinit var hmppsQueueService: HmppsQueueService

  @Autowired
  lateinit var inboxEventDispatcher: InboxEventDispatcher

  @Autowired
  lateinit var inboxAsserter: InboxAsserter

  private val domainTopic by lazy {
    hmppsQueueService.findByTopicId("domainevents") ?: throw MissingTopicException("domainevents topic not found")
  }

  companion object {
    const val OLD_TIER = "OLD"
    const val NEW_TIER = "NEW"
    const val CRN = "CRN123"
  }

  @Test
  fun `case exists, update tier, inbox event is PROCESSED`() {
    caseEntityFactory.produceAndPersist {
      withCrn(CRN)
      withTier(OLD_TIER)
    }

    hmppsTierMockSuccessfulTierCall(
      CRN,
      Tier(
        tierScore = NEW_TIER,
        calculationId = UUID.randomUUID(),
        calculationDate = LocalDateTime.now(),
      ),
    )

    publishTierEvent(CRN)

    inboxAsserter.waitForPendingCount(1)

    inboxEventDispatcher.process()

    inboxAsserter.assertProcessedCount(1)

    assertThat(caseService.getCase(CRN)!!.tier).isEqualTo(NEW_TIER)
  }

  @Test
  fun `case doesnt exist, do nothing, inbox event is SKIPPED`() {
    publishTierEvent("CRN123")

    inboxAsserter.waitForPendingCount(1)

    inboxEventDispatcher.process()

    inboxAsserter.assertIgnoredCount(1)
  }

  @Test
  fun `case exists, tier not found, alert, inbox event is FAILED`() {
    caseEntityFactory.produceAndPersist {
      withCrn(CRN)
      withTier(OLD_TIER)
    }

    hmppsTierMock404TierCall(CRN)

    publishTierEvent(CRN)

    inboxAsserter.waitForPendingCount(1)

    inboxEventDispatcher.process()

    inboxAsserter.assertFailedCount(1)
  }

  @Test
  fun `case exists, upstream error, FAILED`() {
    caseEntityFactory.produceAndPersist {
      withCrn(CRN)
      withTier(OLD_TIER)
    }

    hmppsTierMock500TierCall(CRN)

    publishTierEvent(CRN)

    inboxAsserter.waitForPendingCount(1)

    inboxEventDispatcher.process()

    inboxAsserter.assertFailedCount(1)
  }

  private fun publishTierEvent(crn: String) {
    val domainEvent = HmppsDomainEventFactory()
      .withEventType(TIER_CALCULATION_EVENT_TYPE)
      .withPersonReference(
        PersonReference(
          listOf(
            PersonIdentifier("CRN", crn),
          ),
        ),
      )
      .produce()

    domainTopic.snsClient.publish(
      PublishRequest.builder()
        .topicArn(domainTopic.arn)
        .message(jsonMapper.writeValueAsString(domainEvent))
        .messageAttributes(
          mapOf(
            "eventType" to MessageAttributeValue.builder().dataType("String").stringValue(TIER_CALCULATION_EVENT_TYPE).build(),
          ),
        ).build(),
    )
  }
}
