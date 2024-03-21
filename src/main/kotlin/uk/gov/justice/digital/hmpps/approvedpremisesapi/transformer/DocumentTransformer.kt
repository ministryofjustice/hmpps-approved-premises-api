package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DocumentLevel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.Document
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.GroupedDocuments
import java.time.ZoneOffset
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Document as ApiDocument

@Component
class DocumentTransformer {
  fun transformToApi(document: Document, level: DocumentLevel) = ApiDocument(
    id = document.id,
    level = level,
    fileName = document.documentName,
    createdAt = document.createdAt.toInstant(ZoneOffset.UTC),
    typeCode = document.type.code,
    typeDescription = document.type.description,
    description = document.extendedDescription,
  )

  fun transformToApi(groupedDocuments: GroupedDocuments, convictionId: Long): List<ApiDocument> {
    val offenderDocuments = groupedDocuments.documents.map { transformToApi(it, DocumentLevel.offender) }

    val documentsForConvictionId = groupedDocuments.convictions
      .firstOrNull { it.convictionId == convictionId.toString() }

    val convictionDocuments = documentsForConvictionId?.documents?.map { transformToApi(it, DocumentLevel.conviction) }
      ?: emptyList()

    return offenderDocuments + convictionDocuments
  }
}
