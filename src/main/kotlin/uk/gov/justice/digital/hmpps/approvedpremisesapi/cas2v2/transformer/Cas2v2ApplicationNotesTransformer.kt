package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2ApplicationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2ApplicationNoteEntity

@Component
class Cas2v2ApplicationNotesTransformer {

  fun transformJpaToApi(
    jpa: Cas2v2ApplicationNoteEntity,
  ): Cas2v2ApplicationNote {
    val name = jpa.getUser().name
    val email = jpa.getUser().email ?: "Not found"
    return Cas2v2ApplicationNote(
      id = jpa.id,
      name = name,
      email = email,
      body = jpa.body,
      createdAt = jpa.createdAt.toInstant(),
    )
  }
}
