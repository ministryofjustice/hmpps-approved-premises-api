package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.transformer

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.model.Cas2V2OASysRiskLevel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.model.Cas2v2OASysAssessmentMetadataDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.model.Cas2v2OAsysRiskToSelfDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.model.Cas2v2OAsysRoshRatingsDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.model.Cas2v2OAsysRoshSummaryDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.transformer.Cas2v2OASysAssessmentInfoTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.apandoasys.OASysAssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.apandoasys.RisksToTheIndividual
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.apandoasys.RoshRatings
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.apandoasys.RoshSummary

@Service
class Cas2v2OASysTransformer {

  fun toOASysAssessmentMetadataDto(summary: OASysAssessmentSummary?) = Cas2v2OASysAssessmentMetadataDto(
    hasApplicableAssessment = summary != null,
    dateStarted = summary?.initiationDate?.toInstant(),
    dateCompleted = summary?.completedDate?.toInstant(),
  )

  fun toOASysRiskToSelfDto(risksToTheIndividual: RisksToTheIndividual?) = Cas2v2OAsysRiskToSelfDto(
    metadata = Cas2v2OASysAssessmentInfoTransformer().toAssessmentMetadata(risksToTheIndividual),
    analysisSuicideSelfharm = risksToTheIndividual?.riskToTheIndividual?.analysisSuicideSelfharm,
    analysisVulnerabilities = risksToTheIndividual?.riskToTheIndividual?.analysisVulnerabilities,
  )

  fun toOASysRoshSummaryDto(roshSummary: RoshSummary?) = Cas2v2OAsysRoshSummaryDto(
    metadata = Cas2v2OASysAssessmentInfoTransformer().toAssessmentMetadata(roshSummary),
    whoIsAtRisk = roshSummary?.roshSummary?.whoIsAtRisk,
    natureOfRisk = roshSummary?.roshSummary?.natureOfRisk,
  )

  fun toOASysRoshRatingsDto(roshRatings: RoshRatings?) = Cas2v2OAsysRoshRatingsDto(
    metadata = Cas2v2OASysAssessmentInfoTransformer().toAssessmentMetadata(roshRatings),
    overallRisk = Cas2V2OASysRiskLevel.forValue(roshRatings?.rosh?.determineOverallRiskLevel()?.name),
    riskToChildren = Cas2V2OASysRiskLevel.forValue(roshRatings?.rosh?.riskChildrenCommunity?.name),
    riskToPublic = Cas2V2OASysRiskLevel.forValue(roshRatings?.rosh?.riskPublicCommunity?.name),
    riskToKnownAdult = Cas2V2OASysRiskLevel.forValue(roshRatings?.rosh?.riskKnownAdultCommunity?.name),
    riskToStaff = Cas2V2OASysRiskLevel.forValue(roshRatings?.rosh?.riskStaffCommunity?.name),
  )
}
