package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2ApplicationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationNoteEntity

@Component
class Cas2v2ApplicationNotesTransformer {

  fun transformJpaToApi(
    jpa: Cas2ApplicationNoteEntity,
  ): Cas2v2ApplicationNote {
    val name = jpa.createdByCas2User.name
    val email = jpa.createdByCas2User.email ?: "Not found"
    return Cas2v2ApplicationNote(
      id = jpa.id,
      name = name,
      email = email,
      body = jpa.body,
      createdAt = jpa.createdAt.toInstant(),
    )
  }
}
