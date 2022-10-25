package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonNeed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NeedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.assessrisksandneeds.NeedSeverity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.NeedsTransformer

class NeedsTransformerTest {
  private val needsTransformer = NeedsTransformer()

  @Test
  fun `transformToApi sorts & transforms correctly`() {
    val needs = listOf(
      NeedFactory()
        .withSeverity(NeedSeverity.SEVERE)
        .withSection("SEVERE_HARM")
        .withName("SEVERE_HARM")
        .withOverThreshold(true)
        .withRiskOfHarm(true)
        .withRiskOfReoffending(true)
        .withFlaggedAsNeed(true)
        .withIdentifiedAsNeed(true)
        .withNeedScore(5)
        .produce(),
      NeedFactory()
        .withSeverity(NeedSeverity.STANDARD)
        .withSection("STANDARD_HARM")
        .withName("STANDARD_HARM")
        .withOverThreshold(true)
        .withRiskOfHarm(true)
        .withRiskOfReoffending(false)
        .withFlaggedAsNeed(true)
        .withIdentifiedAsNeed(true)
        .withNeedScore(5)
        .produce(),
      NeedFactory()
        .withSeverity(NeedSeverity.STANDARD)
        .withSection("RISK_OF_REOFFEND")
        .withName("RISK_OF_REOFFEND")
        .withOverThreshold(true)
        .withRiskOfHarm(false)
        .withRiskOfReoffending(true)
        .withFlaggedAsNeed(true)
        .withIdentifiedAsNeed(true)
        .withNeedScore(5)
        .produce()
    )

    val transformed = needsTransformer.transformToApi(needs)

    assertThat(transformed.linkedToRiskOfSeriousHarm).containsExactly(
      PersonNeed(
        section = "SEVERE_HARM",
        name = "SEVERE_HARM",
        overThreshold = true,
        riskOfHarm = true,
        flaggedAsNeed = true,
        severity = "SEVERE",
        identifiedAsNeed = true,
        needScore = 5,
        riskOfReoffending = true
      )
    )

    assertThat(transformed.linkedToReoffending).containsExactly(
      PersonNeed(
        section = "RISK_OF_REOFFEND",
        name = "RISK_OF_REOFFEND",
        overThreshold = true,
        riskOfHarm = false,
        flaggedAsNeed = true,
        severity = "STANDARD",
        identifiedAsNeed = true,
        needScore = 5,
        riskOfReoffending = true
      )
    )

    assertThat(transformed.notLinkedToSeriousHarmOrReoffending).containsExactly(
      PersonNeed(
        section = "STANDARD_HARM",
        name = "STANDARD_HARM",
        overThreshold = true,
        riskOfHarm = true,
        flaggedAsNeed = true,
        severity = "STANDARD",
        identifiedAsNeed = true,
        needScore = 5,
        riskOfReoffending = false
      )
    )
  }
}
