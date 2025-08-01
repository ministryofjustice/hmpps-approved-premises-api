package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1PremiseCapacitySummaryTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@SuppressWarnings("MagicNumber")
@Service
class Cas1CapacityPerformanceTestJob(
  private val cas1PremisesService: Cas1PremisesService,
  private val cas1PremiseCapacitySummaryTransformer: Cas1PremiseCapacitySummaryTransformer,
  private val objectMapper: ObjectMapper,
  private val cas1PremisesRepository: ApprovedPremisesRepository,
) : MigrationJob() {

  private val log = LoggerFactory.getLogger(this::class.java)

  override val shouldRunInTransaction = false

  override fun process(pageSize: Int) {
    log.info("Calculating capacity")
    val start = LocalDateTime.now()

    val capacities = extractEntityFromCasResult(
      cas1PremisesService.getPremisesCapacities(
        premisesIds = cas1PremisesRepository.findAllIds(),
        startDate = LocalDate.of(2025, 1, 1),
        endDate = LocalDate.of(2025, 1, 7),
        excludeSpaceBookingId = null,
      ),
    )

    val description = objectMapper.writeValueAsString(capacities.results.map { cas1PremiseCapacitySummaryTransformer.toCas1PremiseCapacitySummary(it) })

    log.info("Capacities are $description")

    val timeTaken = ChronoUnit.MILLIS.between(start, LocalDateTime.now())

    log.info("job took $timeTaken millis")
  }
}
