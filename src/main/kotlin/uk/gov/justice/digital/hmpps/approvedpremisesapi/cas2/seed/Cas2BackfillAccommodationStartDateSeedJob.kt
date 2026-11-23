package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.seed

import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.seed.SeedException
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.seed.SeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.seed.SeedLogger
import java.time.LocalDate
import java.time.format.DateTimeParseException

@Component
class Cas2BackfillAccommodationStartDateSeedJob(
  private val applicationRepository: Cas2ApplicationRepository,
  private val jsonMapper: JsonMapper,
  private val seedLogger: SeedLogger,
) : SeedJob<Unit>(requiredHeaders = setOf("run")) {
  override fun deserializeRow(columns: Map<String, String>) {
    require(columns.getValue("run").trim() == "true") { "run must be true" }
  }

  override fun processRow(row: Unit) {
    val applications = applicationRepository.findAllByApplicationOriginAndSubmittedAtIsNotNullAndAccommodationStartDateIsNull(ApplicationOrigin.other)
    var updated = 0
    applications.forEach { application ->
      val date = accommodationStartDate(application) ?: return@forEach
      application.accommodationStartDate = date
      applicationRepository.save(application)
      updated++
    }
    seedLogger.info("Backfilled accommodation start dates for $updated applications")
  }

  private fun accommodationStartDate(application: Cas2ApplicationEntity): LocalDate? {
    val data = application.data ?: return null
    val answer = jsonMapper.readTree(data)
      .path("cohorts")
      .path("accommodation-start-date")
      .path("accommodationStartDate")
    if (answer.isMissingNode || answer.isNull) return null

    if (!answer.isString) throw SeedException("Accommodation start date for application ${application.id} is not a string")
    val value = answer.stringValue()
    return try {
      LocalDate.parse(value)
    } catch (exception: DateTimeParseException) {
      throw SeedException("Invalid accommodation start date for application ${application.id}: $value", exception)
    }
  }
}
