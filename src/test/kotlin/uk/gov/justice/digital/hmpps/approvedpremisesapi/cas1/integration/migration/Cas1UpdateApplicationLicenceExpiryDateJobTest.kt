package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.integration.migration

import com.fasterxml.jackson.core.type.TypeReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.migration.UpdateLicenceExpiryDateRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJobService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateAfter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.collections.get

class Cas1UpdateApplicationLicenceExpiryDateJobTest : IntegrationTestBase() {
  @Autowired
  lateinit var migrationJobService: MigrationJobService

  @Autowired
  lateinit var updateLicenceExpiryDateRepository: UpdateLicenceExpiryDateRepository

  @Test
  fun `all applications have the licence expiry date updated`() {
    val probationRegion = givenAProbationRegion()

    val user = userEntityFactory.produceAndPersist {
      withProbationRegion(probationRegion)
    }

    val applications = generateSequence {
      approvedPremisesApplicationEntityFactory.produceAndPersist {
        withCreatedByUser(user)
        withData(
          objectMapper.writeValueAsString(
            mapOf(
              "basic-information" to mapOf(
                "relevant-dates" to mapOf(
                  "selectedDates" to mapOf(
                    "licenceExpiryDate" to LocalDate.now().randomDateAfter(10).toString(),
                  ),
                  "licenceExpiryDate" to LocalDate.now().randomDateAfter(10).toString(),
                ),
              ),
            ),
          ),
        )
      }
    }.take(10).toList()

    migrationJobService.runMigrationJob(MigrationJobType.updateCas1ApplicationsLicenceExpiryDate, 1)

    applications.forEach {
      val application = updateLicenceExpiryDateRepository.findByIdOrNull(it.id)!! as ApprovedPremisesApplicationEntity
      val data: Map<String, Any> = objectMapper.readValue(application.data, object : TypeReference<Map<String, Any>>() {})
      assertThat(application.licenceExpiryDate).isEqualTo(extractLicenceExpiryDate(data))
    }
  }

  fun extractLicenceExpiryDate(data: Map<String, Any>): LocalDate? {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    return (data["basic-information"] as? Map<*, *>)?.let { it["relevant-dates"] as? Map<*, *> }
      ?.let {
        val dateStr = it["licenceExpiryDate"] as? String
        dateStr?.let {
          try {
            LocalDate.parse(it, formatter)
          } catch (e: DateTimeParseException) {
            null
          }
        }
      }
  }
}
