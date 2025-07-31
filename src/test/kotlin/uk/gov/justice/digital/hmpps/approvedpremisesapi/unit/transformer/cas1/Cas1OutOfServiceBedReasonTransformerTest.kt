package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OutOfServiceBedReasonReferenceType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1OutOfServiceBedReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedReasonEntityReferenceType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1OutOfServiceBedReasonTransformer

class Cas1OutOfServiceBedReasonTransformerTest {
  private val transformer = Cas1OutOfServiceBedReasonTransformer()

  @ParameterizedTest
  @CsvSource(
    "CRN, CRN",
    "WORK_ORDER, WORK_ORDER",
  )
  fun `transformJpaToApi transforms correctly`(
    jpaReferenceType: Cas1OutOfServiceBedReasonEntityReferenceType,
    apiReferenceType: Cas1OutOfServiceBedReasonReferenceType,
  ) {
    val reason = Cas1OutOfServiceBedReasonEntityFactory()
      .withName("the reason name")
      .withIsActive(true)
      .withReferenceType(jpaReferenceType)
      .produce()

    val result = transformer.transformJpaToApi(reason)

    assertThat(result.id).isEqualTo(reason.id)
    assertThat(result.name).isEqualTo("the reason name")
    assertThat(result.isActive).isEqualTo(true)
    assertThat(result.referenceType).isEqualTo(apiReferenceType)
  }
}
