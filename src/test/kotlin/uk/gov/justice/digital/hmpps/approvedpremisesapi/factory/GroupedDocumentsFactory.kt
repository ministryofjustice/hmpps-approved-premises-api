package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.ConvictionDocuments
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.ConvictionLevelDocument
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.GroupedDocuments
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderLevelDocument

class GroupedDocumentsFactory : Factory<GroupedDocuments> {
  private val offenderLevelDocuments = mutableListOf<OffenderLevelDocument>()
  private val convictionLevelDocuments = mutableMapOf<String, MutableList<ConvictionLevelDocument>>()

  fun withOffenderLevelDocument(offenderLevelDocument: OffenderLevelDocument) = apply {
    this.offenderLevelDocuments += offenderLevelDocument
  }

  fun withConvictionLevelDocument(convictionId: String, convictionLevelDocument: ConvictionLevelDocument) = apply {
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
