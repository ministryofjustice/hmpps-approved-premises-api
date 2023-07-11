package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Document
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DocumentLevel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.GroupedDocuments
import java.time.ZoneOffset
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.Document as CommunityApiDocument

@Component
class DocumentTransformer {
  fun transformToApi(groupedDocuments: GroupedDocuments, convictionId: Long): List<Document> {
    val offenderDocuments = documentsWithIds(groupedDocuments.documents).map {
      Document(
        id = it.id!!,
        level = DocumentLevel.offender,
        fileName = it.documentName,
        createdAt = it.createdAt.toInstant(ZoneOffset.UTC),
        typeCode = it.type.code,
        typeDescription = it.type.description,
        description = it.extendedDescription,
      )
    }

    val documentsForConvictionId = groupedDocuments.convictions
      .firstOrNull { it.convictionId == convictionId.toString() }

    val convictionDocuments = if (documentsForConvictionId != null) {
      documentsWithIds(documentsForConvictionId.documents).map {
        Document(
          id = it.id!!,
          level = DocumentLevel.conviction,
          fileName = it.documentName,
          createdAt = it.createdAt.toInstant(ZoneOffset.UTC),
          typeCode = it.type.code,
          typeDescription = it.type.description,
          description = it.extendedDescription,
        )
      }
    } else {
      emptyList()
    }

    return offenderDocuments + convictionDocuments
  }

  // Filter out any documents without IDs - at the moment, we don't have a way of fetching documents without IDs
  // This fixes the nullability problem we have previously had with documents with no ID
  fun documentsWithIds(documents: List<CommunityApiDocument>) = documents.filter { it.id != null }
}
