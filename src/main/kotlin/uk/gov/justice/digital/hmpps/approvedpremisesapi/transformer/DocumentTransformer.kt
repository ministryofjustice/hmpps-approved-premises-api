package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Document
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DocumentLevel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.GroupedDocuments
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.APDeliusDocument
import java.time.ZoneOffset
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.Document as CommunityApiDocument

@Component
class DocumentTransformer {

  private val log = LoggerFactory.getLogger(this::class.java)

  fun transformToApi(groupedDocuments: GroupedDocuments): List<Document> {
    val offenderDocuments = documentsWithIdsAndNames(groupedDocuments.documents).map {
      Document(
        id = it.id!!,
        level = DocumentLevel.OFFENDER,
        fileName = it.documentName!!,
        createdAt = it.createdAt.toInstant(ZoneOffset.UTC),
        typeCode = it.type.code,
        typeDescription = it.type.description,
        description = it.extendedDescription,
      )
    }

    val convictionDocuments = groupedDocuments
      .convictions
      .flatMap { it.documents }

    val filteredConvictionDocuments = documentsWithIdsAndNames(convictionDocuments).map {
      Document(
        id = it.id!!,
        level = DocumentLevel.CONVICTION,
        fileName = it.documentName!!,
        createdAt = it.createdAt.toInstant(ZoneOffset.UTC),
        typeCode = it.type.code,
        typeDescription = it.type.description,
        description = it.extendedDescription,
      )
    }

    return offenderDocuments + filteredConvictionDocuments
  }

  fun transformToApi(documents: List<APDeliusDocument>): List<Document> =
    documents
      .filter { it.id != null }
      .map {
        Document(
          id = it.id!!,
          level = when (it.level) {
            "Conviction" -> DocumentLevel.CONVICTION
            else -> DocumentLevel.OFFENDER
          },
          fileName = it.filename,
          createdAt = it.dateCreated.toInstant(),
          typeCode = it.typeCode,
          typeDescription = it.typeDescription,
          description = it.description,
        )
      }

  /**
   * If a document doesn't have an ID we have no way to retrieve it for download, so it's filtered out
   *
   * If a document doesn't have a name, we have no way to determine what the file should be called on download,
   * or what file extension (e.g. pdf) should be used. This rarely occurs.
   */
  private fun documentsWithIdsAndNames(documents: List<CommunityApiDocument>): List<CommunityApiDocument> {
    documents
      .filter { it.documentName == null }
      .forEach {
        log.warn("Filtering out document with id ${it.id} because it doesn't have a name")
      }

    return documents.filter { it.id != null && it.documentName != null }
  }
}
