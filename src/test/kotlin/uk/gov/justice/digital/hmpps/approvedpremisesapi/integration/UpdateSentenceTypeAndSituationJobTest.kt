package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.fasterxml.jackson.core.type.TypeReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.MigrationJobService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringLowerCase

class UpdateSentenceTypeAndSituationJobTest : IntegrationTestBase() {
  @Autowired
  lateinit var migrationJobService: MigrationJobService

  @Autowired
  lateinit var applicationRepository: ApplicationRepository

  @Test
  fun `all applications have the sentence type and situation updated`() {
    val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
    }

    val probationRegion = givenAProbationRegion()

    val user = userEntityFactory.produceAndPersist {
      withProbationRegion(probationRegion)
    }

    val applications = generateSequence {
      approvedPremisesApplicationEntityFactory.produceAndPersist {
        withApplicationSchema(applicationSchema)
        withCreatedByUser(user)
        withData(
          objectMapper.writeValueAsString(
            mapOf(
              "basic-information" to mapOf(
                "sentence-type" to mapOf(
                  "sentenceType" to randomStringLowerCase(10),
                ),
                "situation" to mapOf(
                  "situation" to randomStringLowerCase(10),
                ),
              ),
            ),
          ),
        )
      }
    }.take(50).toList()

    migrationJobService.runMigrationJob(MigrationJobType.sentenceTypeAndSituation, 1)

    applications.forEach {
      val application = applicationRepository.findByIdOrNull(it.id)!! as ApprovedPremisesApplicationEntity
      val data = objectMapper.readValue(application.data, object : TypeReference<Map<String, Map<String, Map<String, String>>>>() {})
      assertThat(application.situation).isEqualTo(data["basic-information"]?.get("situation")?.get("situation")!!)
      assertThat(application.sentenceType).isEqualTo(data["basic-information"]?.get("sentence-type")?.get("sentenceType")!!)
    }
  }
}
