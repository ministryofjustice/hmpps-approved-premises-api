package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.LinksToHarm
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.LinksToReOffending
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.NeedsDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.NeedsDetailsInner
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers

class NeedsDetailsFactory : AssessmentInfoFactory<NeedsDetails>() {
  private var offenceAnalysisDetails: Yielded<String?> = { randomStringMultiCaseWithNumbers(20) }
  private var emotionalIssuesDetails: Yielded<String?> = { randomStringMultiCaseWithNumbers(20) }
  private var emotionalIssuesLinkedToHarm: Yielded<Boolean?> = { false }
  private var emotionalIssuesLinkedToReOffending: Yielded<Boolean?> = { false }
  private var drugIssuesDetails: Yielded<String?> = { randomStringMultiCaseWithNumbers(20) }
  private var drugIssuesLinkedToHarm: Yielded<Boolean?> = { false }
  private var drugIssuesLinkedToReOffending: Yielded<Boolean?> = { false }
  private var alcoholIssuesDetails: Yielded<String?> = { randomStringMultiCaseWithNumbers(20) }
  private var alcoholIssuesLinkedToHarm: Yielded<Boolean?> = { false }
  private var alcoholIssuesLinkedToReOffending: Yielded<Boolean?> = { false }
  private var lifestyleIssuesDetails: Yielded<String?> = { randomStringMultiCaseWithNumbers(20) }
  private var lifestyleLinkedToHarm: Yielded<Boolean?> = { false }
  private var lifestyleLinkedToReOffending: Yielded<Boolean?> = { false }
  private var relationshipIssuesDetails: Yielded<String?> = { randomStringMultiCaseWithNumbers(20) }
  private var relationshipIssuesLinkedToHarm: Yielded<Boolean?> = { false }
  private var relationshipIssuesLinkedToReOffending: Yielded<Boolean?> = { false }
  private var financeIssuesDetails: Yielded<String?> = { randomStringMultiCaseWithNumbers(20) }
  private var financeLinkedToHarm: Yielded<Boolean?> = { false }
  private var financeLinkedToReOffending: Yielded<Boolean?> = { false }
  private var educationTrainingEmploymentIssuesDetails: Yielded<String?> = { randomStringMultiCaseWithNumbers(20) }
  private var educationTrainingEmploymentIssuesLinkedToHarm: Yielded<Boolean?> = { false }
  private var educationTrainingEmploymentIssuesLinkedToReOffending: Yielded<Boolean?> = { false }
  private var accommodationIssuesDetails: Yielded<String?> = { randomStringMultiCaseWithNumbers(20) }
  private var accommodationLinkedToHarm: Yielded<Boolean?> = { false }
  private var accommodationLinkedToReOffending: Yielded<Boolean?> = { false }
  private var attitudeIssuesDetails: Yielded<String?> = { randomStringMultiCaseWithNumbers(20) }
  private var attitudeIssuesLinkedToHarm: Yielded<Boolean?> = { false }
  private var attitudeIssuesLinkedToReOffending: Yielded<Boolean?> = { false }
  private var thinkingBehaviouralIssuesDetails: Yielded<String?> = { randomStringMultiCaseWithNumbers(20) }
  private var thinkingBehaviouralIssuesLinkedToHarm: Yielded<Boolean?> = { false }
  private var thinkingBehaviouralIssuesLinkedToReOffending: Yielded<Boolean?> = { false }

  fun withEmotionalIssuesDetails(emotionalIssuesDetails: String? = null, linkedToHarm: Boolean?, linkedToReoffending: Boolean? = false) = apply {
    this.emotionalIssuesDetails = { emotionalIssuesDetails }
    this.emotionalIssuesLinkedToHarm = { linkedToHarm }
    this.emotionalIssuesLinkedToReOffending = { linkedToReoffending }
  }

  fun withDrugIssuesDetails(drugIssuesDetails: String? = null, linkedToHarm: Boolean?, linkedToReoffending: Boolean? = false) = apply {
    this.drugIssuesDetails = { drugIssuesDetails }
    this.drugIssuesLinkedToHarm = { linkedToHarm }
    this.drugIssuesLinkedToReOffending = { linkedToReoffending }
  }

  fun withAlcoholIssuesDetails(alcoholIssuesDetails: String? = null, linkedToHarm: Boolean?, linkedToReoffending: Boolean? = false) = apply {
    this.alcoholIssuesDetails = { alcoholIssuesDetails }
    this.alcoholIssuesLinkedToHarm = { linkedToHarm }
    this.alcoholIssuesLinkedToReOffending = { linkedToReoffending }
  }

  fun withLifestyleIssuesDetails(lifestyleIssuesDetails: String? = null, linkedToHarm: Boolean?, linkedToReoffending: Boolean? = false) = apply {
    this.lifestyleIssuesDetails = { lifestyleIssuesDetails }
    this.lifestyleLinkedToHarm = { linkedToHarm }
    this.lifestyleLinkedToReOffending = { linkedToReoffending }
  }

  fun withRelationshipIssuesDetails(relationshipIssuesDetails: String? = null, linkedToHarm: Boolean?, linkedToReoffending: Boolean? = false) = apply {
    this.relationshipIssuesDetails = { relationshipIssuesDetails }
    this.relationshipIssuesLinkedToHarm = { linkedToHarm }
    this.relationshipIssuesLinkedToReOffending = { linkedToReoffending }
  }

  fun withFinanceIssuesDetails(financeIssuesDetails: String? = null, linkedToHarm: Boolean?, linkedToReoffending: Boolean? = false) = apply {
    this.financeIssuesDetails = { financeIssuesDetails }
    this.financeLinkedToHarm = { linkedToHarm }
    this.financeLinkedToReOffending = { linkedToReoffending }
  }

  fun withEducationTrainingEmploymentIssuesDetails(educationTrainingEmploymentIssuesDetails: String? = null, linkedToHarm: Boolean?, linkedToReoffending: Boolean? = false) = apply {
    this.educationTrainingEmploymentIssuesDetails = { educationTrainingEmploymentIssuesDetails }
    this.educationTrainingEmploymentIssuesLinkedToHarm = { linkedToHarm }
    this.educationTrainingEmploymentIssuesLinkedToReOffending = { linkedToReoffending }
  }

  fun withAccommodationIssuesDetails(accommodationIssuesDetails: String? = null, linkedToHarm: Boolean?, linkedToReoffending: Boolean? = false) = apply {
    this.accommodationIssuesDetails = { accommodationIssuesDetails }
    this.accommodationLinkedToHarm = { linkedToHarm }
    this.accommodationLinkedToReOffending = { linkedToReoffending }
  }

  fun withAttitudeIssuesDetails(attitudeIssuesDetails: String? = null, linkedToHarm: Boolean?, linkedToReoffending: Boolean? = false) = apply {
    this.attitudeIssuesDetails = { attitudeIssuesDetails }
    this.attitudeIssuesLinkedToHarm = { linkedToHarm }
    this.attitudeIssuesLinkedToReOffending = { linkedToReoffending }
  }

  fun withThinkingBehaviouralIssuesDetails(thinkingBehaviouralIssuesDetails: String? = null, linkedToHarm: Boolean?, linkedToReoffending: Boolean? = false) = apply {
    this.thinkingBehaviouralIssuesDetails = { thinkingBehaviouralIssuesDetails }
    this.thinkingBehaviouralIssuesLinkedToHarm = { linkedToHarm }
    this.thinkingBehaviouralIssuesLinkedToReOffending = { linkedToReoffending }
  }

  override fun produce() = NeedsDetails(
    assessmentId = this.assessmentId(),
    assessmentType = this.assessmentType(),
    dateCompleted = this.dateCompleted(),
    assessorSignedDate = this.assessorSignedDate(),
    initiationDate = this.initiationDate(),
    assessmentStatus = this.assessmentStatus(),
    superStatus = this.superStatus(),
    limitedAccessOffender = this.limitedAccessOffender(),
    needs = NeedsDetailsInner(
      offenceAnalysisDetails = this.offenceAnalysisDetails(),
      emotionalIssuesDetails = this.emotionalIssuesDetails(),
      drugIssuesDetails = this.drugIssuesDetails(),
      alcoholIssuesDetails = this.alcoholIssuesDetails(),
      lifestyleIssuesDetails = this.lifestyleIssuesDetails(),
      relationshipIssuesDetails = this.relationshipIssuesDetails(),
      financeIssuesDetails = this.financeIssuesDetails(),
      educationTrainingEmploymentIssuesDetails = this.educationTrainingEmploymentIssuesDetails(),
      accommodationIssuesDetails = this.accommodationIssuesDetails(),
      attitudeIssuesDetails = this.attitudeIssuesDetails(),
      thinkingBehaviouralIssuesDetails = this.thinkingBehaviouralIssuesDetails(),
    ),
    linksToHarm = LinksToHarm(
      accommodationLinkedToHarm = this.accommodationLinkedToHarm(),
      educationTrainingEmploymentLinkedToHarm = this.educationTrainingEmploymentIssuesLinkedToHarm(),
      financeLinkedToHarm = this.financeLinkedToHarm(),
      relationshipLinkedToHarm = this.relationshipIssuesLinkedToHarm(),
      lifestyleLinkedToHarm = this.lifestyleLinkedToHarm(),
      drugLinkedToHarm = this.drugIssuesLinkedToHarm(),
      alcoholLinkedToHarm = this.alcoholIssuesLinkedToHarm(),
      emotionalLinkedToHarm = this.emotionalIssuesLinkedToHarm(),
      thinkingBehaviouralLinkedToHarm = this.thinkingBehaviouralIssuesLinkedToHarm(),
      attitudeLinkedToHarm = this.attitudeIssuesLinkedToHarm(),
    ),
    linksToReOffending = LinksToReOffending(
      accommodationLinkedToReOffending = this.accommodationLinkedToReOffending(),
      educationTrainingEmploymentLinkedToReOffending = this.educationTrainingEmploymentIssuesLinkedToReOffending(),
      financeLinkedToReOffending = this.financeLinkedToReOffending(),
      relationshipLinkedToReOffending = this.relationshipIssuesLinkedToReOffending(),
      lifestyleLinkedToReOffending = this.lifestyleLinkedToReOffending(),
      drugLinkedToReOffending = this.drugIssuesLinkedToReOffending(),
      alcoholLinkedToReOffending = this.alcoholIssuesLinkedToReOffending(),
      emotionalLinkedToReOffending = this.emotionalIssuesLinkedToReOffending(),
      thinkingBehaviouralLinkedToReOffending = this.thinkingBehaviouralIssuesLinkedToReOffending(),
      attitudeLinkedToReOffending = this.attitudeIssuesLinkedToReOffending(),
    ),
  )
}
