package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.Cas2HdcApplicationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationNoteEntity

@Component("Cas2ApplicationNoteTransformer")
class Cas2HdcApplicationNotesTransformer {

  fun transformJpaToApi(
    jpa: Cas2ApplicationNoteEntity,
  ): Cas2HdcApplicationNote {
    val name = jpa.createdByUser.name
    val email = jpa.createdByUser.email ?: "Not found"
    return Cas2HdcApplicationNote(
      id = jpa.id,
      name = name,
      email = email,
      body = jpa.body,
      createdAt = jpa.createdAt.toInstant(),
    )
  }
}
