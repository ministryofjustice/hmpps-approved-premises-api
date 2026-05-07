package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.seed

import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesRepository
import java.util.UUID

@Component
class Cas3UpdatePremisesPostcodeSeedJob(
  private val cas3PremisesRepository: Cas3PremisesRepository,
) : uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedJob<Cas3UpdatePostcodeRow>(
  requiredHeaders = setOf("cas3PremisesId", "postcode"),
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>) = Cas3UpdatePostcodeRow(
    cas3PremisesId = UUID.fromString(columns["cas3PremisesId"]),
    postcode = columns["postcode"]!!.trim(),
  )

  override fun processRow(row: Cas3UpdatePostcodeRow) {
    val cas3PremisesId = row.cas3PremisesId
    val postcode = row.postcode

    cas3PremisesRepository.findByIdOrNull(cas3PremisesId)?.let {
      log.info("Updating postcode from {} to {} for CAS3 Premises with id: {}", it.postcode, postcode, cas3PremisesId)
      it.postcode = postcode
      cas3PremisesRepository.save(it)
    } ?: log.warn("Cas3Premises with id: {} not found", cas3PremisesId)
  }
}

data class Cas3UpdatePostcodeRow(
  val cas3PremisesId: UUID,
  val postcode: String,
)
