package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PrisonCaseNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.CaseNote
import java.time.ZoneOffset

@Component
class PrisonCaseNoteTransformer {
  fun transformModelToApi(domain: CaseNote) = PrisonCaseNote(
    id = domain.caseNoteId,
    sensitive = domain.sensitive,
    createdAt = domain.creationDateTime.toInstant(ZoneOffset.UTC),
    occurredAt = domain.occurrenceDateTime.toInstant(ZoneOffset.UTC),
    authorName = domain.authorName,
    type = domain.typeDescription ?: domain.type,
    subType = domain.subTypeDescription ?: domain.subType,
    note = domain.text,
  )
}
