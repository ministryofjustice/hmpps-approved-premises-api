package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity

import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin
import java.time.OffsetDateTime

@Component
object Cas2v2ApplicationSummarySpecifications {

  fun hasUserId(userId: String): Specification<Cas2v2ApplicationSummaryEntity> = Specification { root, _, criteriaBuilder ->
    criteriaBuilder.equal(root.get<String>("userId"), userId)
  }

  fun hasApplicationOrigin(applicationOrigin: ApplicationOrigin?): Specification<Cas2v2ApplicationSummaryEntity> = Specification { root, _, criteriaBuilder ->
    applicationOrigin?.let {
      criteriaBuilder.equal(root.get<String>("applicationOrigin"), it.toString())
    } ?: criteriaBuilder.conjunction()
  }

  fun isSubmitted(submitted: Boolean?): Specification<Cas2v2ApplicationSummaryEntity> = Specification { root, _, criteriaBuilder ->
    submitted?.let {
      if (it) {
        criteriaBuilder.isNotNull(root.get<OffsetDateTime>("submittedAt"))
      } else {
        criteriaBuilder.isNull(root.get<OffsetDateTime>("submittedAt"))
      }
    } ?: criteriaBuilder.conjunction()
  }

  fun hasPrisonCode(prisonCode: String?): Specification<Cas2v2ApplicationSummaryEntity> = Specification { root, _, criteriaBuilder ->
    prisonCode?.let { criteriaBuilder.equal(root.get<String>("prisonCode"), it) }
      ?: criteriaBuilder.conjunction()
  }
}
