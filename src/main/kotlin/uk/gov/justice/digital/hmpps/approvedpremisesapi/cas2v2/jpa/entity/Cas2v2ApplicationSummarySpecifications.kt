package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity

import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationSummaryEntity
import java.time.OffsetDateTime

@Component
object Cas2v2ApplicationSummarySpecifications {

  fun hasUserId(userId: String): Specification<Cas2ApplicationSummaryEntity> = Specification { root, _, criteriaBuilder ->
    criteriaBuilder.equal(root.get<String>("userId"), userId)
  }

  fun hasServiceOrigin(serviceOrigin: String): Specification<Cas2ApplicationSummaryEntity> = Specification { root, _, criteriaBuilder ->
    criteriaBuilder.equal(root.get<String>("serviceOrigin"), serviceOrigin)
  }

  fun hasCrnOrNomsNumber(crnOrNomsNumber: String): Specification<Cas2ApplicationSummaryEntity> = Specification<Cas2ApplicationSummaryEntity> { root, _, criteriaBuilder ->
    criteriaBuilder.equal(root.get<String>("crn"), crnOrNomsNumber)
  }.or({ root, _, criteriaBuilder ->
    criteriaBuilder.equal(root.get<String>("nomsNumber"), crnOrNomsNumber)
  })

  fun hasApplicationOrigin(applicationOrigin: ApplicationOrigin?): Specification<Cas2ApplicationSummaryEntity> = Specification { root, _, criteriaBuilder ->
    applicationOrigin?.let {
      criteriaBuilder.equal(root.get<String>("applicationOrigin"), it.toString())
    } ?: criteriaBuilder.conjunction()
  }

  fun isSubmitted(submitted: Boolean?): Specification<Cas2ApplicationSummaryEntity> = Specification { root, _, criteriaBuilder ->
    submitted?.let {
      if (it) {
        criteriaBuilder.isNotNull(root.get<OffsetDateTime>("submittedAt"))
      } else {
        criteriaBuilder.isNull(root.get<OffsetDateTime>("submittedAt"))
      }
    } ?: criteriaBuilder.conjunction()
  }

  fun hasPrisonCode(prisonCode: String?): Specification<Cas2ApplicationSummaryEntity> = Specification { root, _, criteriaBuilder ->
    prisonCode?.let { criteriaBuilder.equal(root.get<String>("prisonCode"), it) }
      ?: criteriaBuilder.conjunction()
  }
}
