package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Document
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DocumentLevel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.GroupedDocuments
import java.time.ZoneOffset

@Component
class DocumentTransformer {
  fun transformToApi(groupedDocuments: GroupedDocuments, convictionId: Long): List<Document> {
    val offenderDocuments = groupedDocuments.documents.map {
      Document(
        id = it.id,
        level = DocumentLevel.offender,
        fileName = it.documentName,
        createdAt = it.createdAt.toInstant(ZoneOffset.UTC),
        typeCode = it.type.code,
        typeDescription = it.type.description,
        description = it.extendedDescription
      )
    }

    val convictionDocuments = groupedDocuments.convictions
      .firstOrNull { it.convictionId == convictionId.toString() }
      ?.documents
      ?.map {
        Document(
          id = it.id,
          level = DocumentLevel.conviction,
          fileName = it.documentName,
          createdAt = it.createdAt.toInstant(ZoneOffset.UTC),
          typeCode = it.type.code,
          typeDescription = it.type.description,
          description = it.extendedDescription
        )
      } ?: emptyList()

    return offenderDocuments + convictionDocuments
  }
}
