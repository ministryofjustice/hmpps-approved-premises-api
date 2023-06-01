package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PlacementApplicationTransformer

class PlacementApplicationTransformerTest {
  private val objectMapper = ObjectMapper().apply {
    registerModule(Jdk8Module())
    registerModule(JavaTimeModule())
    registerKotlinModule()
  }
  private val placementApplicationTransformer = PlacementApplicationTransformer(objectMapper)

  private var user = UserEntityFactory()
    .withYieldedProbationRegion {
      ProbationRegionEntityFactory()
        .withYieldedApArea { ApAreaEntityFactory().produce() }
        .produce()
    }
    .produce()

  private var application = ApprovedPremisesApplicationEntityFactory()
    .withCreatedByUser(user)
    .produce()

  @Test
  fun `transformJpaToApi converts correctly when there is no data or document`() {
    val placementApplication = PlacementApplicationEntityFactory()
      .withCreatedByUser(user)
      .withApplication(application)
      .withData(null)
      .withDocument(null)
      .produce()

    val result = placementApplicationTransformer.transformJpaToApi(placementApplication)

    assertThat(result.id).isEqualTo(placementApplication.id)
    assertThat(result.applicationId).isEqualTo(placementApplication.application.id)
    assertThat(result.createdByUserId).isEqualTo(placementApplication.createdByUser.id)
    assertThat(result.schemaVersion).isEqualTo(placementApplication.schemaVersion.id)
    assertThat(result.createdAt).isEqualTo(placementApplication.createdAt.toInstant())
    assertThat(result.data).isNull()
    assertThat(result.document).isNull()
    assertThat(result.outdatedSchema).isEqualTo(true)
    assertThat(result.submittedAt).isNull()
  }

  @Test
  fun `transformJpaToApi converts correctly when there is data and a document`() {
    val data = "{\"data\": \"something\"}"
    val document = "{\"document\": \"something\"}"
    val placementApplication = PlacementApplicationEntityFactory()
      .withCreatedByUser(user)
      .withApplication(application)
      .withData(data)
      .withDocument(document)
      .produce()

    val result = placementApplicationTransformer.transformJpaToApi(placementApplication)

    assertThat(result.id).isEqualTo(placementApplication.id)
    assertThat(result.data).isEqualTo(objectMapper.readTree(data))
    assertThat(result.document).isEqualTo(objectMapper.readTree(document))
  }
}
