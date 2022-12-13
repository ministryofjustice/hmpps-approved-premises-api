package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.ConvictionDocuments
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.Document
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.GroupedDocuments

class GroupedDocumentsFactory : Factory<GroupedDocuments> {
  private val offenderLevelDocuments = mutableListOf<Document>()
  private val convictionLevelDocuments = mutableMapOf<String, MutableList<Document>>()

  fun withOffenderLevelDocument(offenderLevelDocument: Document) = apply {
    this.offenderLevelDocuments += offenderLevelDocument
  }

  fun withConvictionLevelDocument(convictionId: String, convictionLevelDocument: Document) = apply {
    if (! this.convictionLevelDocuments.containsKey(convictionId)) {
      this.convictionLevelDocuments[convictionId] = mutableListOf()
    }

    this.convictionLevelDocuments[convictionId]!! += convictionLevelDocument
  }

  override fun produce(): GroupedDocuments = GroupedDocuments(
    documents = offenderLevelDocuments,
    convictions = convictionLevelDocuments.map {
      ConvictionDocuments(
        convictionId = it.key,
        documents = it.value
      )
    }
  )
}
