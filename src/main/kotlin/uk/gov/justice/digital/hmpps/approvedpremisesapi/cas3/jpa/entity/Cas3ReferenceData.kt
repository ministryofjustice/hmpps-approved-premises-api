package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity

import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3ReferenceData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.ReferenceData

fun ReferenceData.toCas3ReferenceData() = Cas3ReferenceData(
  id = id,
  description = description,
  name = name,
)

interface ReferenceDataRepository<T : ReferenceData> {
  fun findAllByActiveIsTrue(): List<T>
}
