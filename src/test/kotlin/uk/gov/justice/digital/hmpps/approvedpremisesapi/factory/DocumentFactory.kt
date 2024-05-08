package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.Document
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.DocumentType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDateTime
import java.util.UUID

class DocumentFactory : Factory<Document> {
  private var id: Yielded<String?> = { UUID.randomUUID().toString() }
  private var documentName: Yielded<String?> = { "${randomStringMultiCaseWithNumbers(5)}.pdf" }
  private var author: Yielded<String?> = { randomStringMultiCaseWithNumbers(4) }
  private var typeCode: Yielded<String> = { randomStringMultiCaseWithNumbers(4) }
  private var typeDescription: Yielded<String> = { randomStringMultiCaseWithNumbers(4) }
  private var extendedDescription: Yielded<String?> = { randomStringMultiCaseWithNumbers(10) }
  private var lastModifiedAt: Yielded<LocalDateTime?> = { null }
  private var createdAt: Yielded<LocalDateTime> = { LocalDateTime.now().randomDateTimeBefore(5) }
  private var parentPrimaryKeyId: Yielded<Long?> = { null }

  fun withId(id: String?) = apply {
    this.id = { id }
  }

  fun withDocumentName(documentName: String?) = apply {
    this.documentName = { documentName }
  }

  fun withAuthor(author: String) = apply {
    this.author = { author }
  }

  fun withoutAuthor() = apply {
    this.author = { null }
  }
  fun withTypeCode(typeCode: String) = apply {
    this.typeCode = { typeCode }
  }

  fun withTypeDescription(typeDescription: String) = apply {
    this.typeDescription = { typeDescription }
  }

  fun withExtendedDescription(extendedDescription: String?) = apply {
    this.extendedDescription = { extendedDescription }
  }

  fun withLastModifiedAt(lastModifiedAt: LocalDateTime?) = apply {
    this.lastModifiedAt = { lastModifiedAt }
  }

  fun withCreatedAt(createdAt: LocalDateTime) = apply {
    this.createdAt = { createdAt }
  }

  fun withParentPrimaryKeyId(parentPrimaryKeyId: Long?) = apply {
    this.parentPrimaryKeyId = { parentPrimaryKeyId }
  }

  override fun produce(): Document = Document(
    id = this.id(),
    documentName = this.documentName(),
    author = this.author(),
    type = DocumentType(
      code = this.typeCode(),
      description = this.typeDescription(),
    ),
    extendedDescription = this.extendedDescription(),
    lastModifiedAt = this.lastModifiedAt(),
    createdAt = this.createdAt(),
    parentPrimaryKeyId = this.parentPrimaryKeyId(),
  )
}
