package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2ApplicationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationNoteEntity

@Component("Cas2ApplicationNoteTransformer")
class ApplicationNotesTransformer {

  fun transformJpaToApi(
    jpa: Cas2ApplicationNoteEntity,
  ):
    Cas2ApplicationNote {
    return Cas2ApplicationNote(
      id = jpa.id,
      name = jpa.createdByNomisUser.name,
      email = jpa.createdByNomisUser.email ?: "Not found",
      body = jpa.body,
      createdAt = jpa.createdAt.toInstant(),
    )
  }
}
