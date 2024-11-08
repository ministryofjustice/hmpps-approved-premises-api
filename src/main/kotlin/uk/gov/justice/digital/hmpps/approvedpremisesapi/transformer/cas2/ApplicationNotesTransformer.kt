package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2ApplicationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationNoteEntity

@Component("Cas2ApplicationNoteTransformer")
class ApplicationNotesTransformer {

  fun transformJpaToApi(
    jpa: Cas2ApplicationNoteEntity,
  ): Cas2ApplicationNote {
    val name = jpa.getUser().name
    val email = jpa.getUser().email ?: "Not found"
    return Cas2ApplicationNote(
      id = jpa.id,
      name = name,
      email = email,
      body = jpa.body,
      createdAt = jpa.createdAt.toInstant(),
    )
  }
}
