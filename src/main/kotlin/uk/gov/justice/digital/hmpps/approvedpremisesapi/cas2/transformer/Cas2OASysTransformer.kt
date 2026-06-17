package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.transformer

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2OASysRiskLevel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2OASysAssessmentMetadataDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2OAsysRiskToSelfDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2OAsysRoshRatingsDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2OAsysRoshSummaryDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.apandoasys.OASysAssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.apandoasys.RisksToTheIndividual
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.apandoasys.RoshRatings
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.apandoasys.RoshSummary

@Service
class Cas2OASysTransformer {

  fun toOASysAssessmentMetadataDto(summary: OASysAssessmentSummary?) = Cas2OASysAssessmentMetadataDto(
    hasApplicableAssessment = summary != null,
    dateStarted = summary?.initiationDate?.toInstant(),
    dateCompleted = summary?.completedDate?.toInstant(),
  )

  fun toOASysRiskToSelfDto(risksToTheIndividual: RisksToTheIndividual?) = Cas2OAsysRiskToSelfDto(
    metadata = Cas2OASysAssessmentInfoTransformer().toAssessmentMetadata(risksToTheIndividual),
    analysisSuicideSelfharm = risksToTheIndividual?.riskToTheIndividual?.analysisSuicideSelfharm,
    analysisVulnerabilities = risksToTheIndividual?.riskToTheIndividual?.analysisVulnerabilities,
  )

  fun toOASysRoshSummaryDto(roshSummary: RoshSummary?) = Cas2OAsysRoshSummaryDto(
    metadata = Cas2OASysAssessmentInfoTransformer().toAssessmentMetadata(roshSummary),
    whoIsAtRisk = roshSummary?.roshSummary?.whoIsAtRisk,
    natureOfRisk = roshSummary?.roshSummary?.natureOfRisk,
  )

  fun toOASysRoshRatingsDto(roshRatings: RoshRatings?) = Cas2OAsysRoshRatingsDto(
    metadata = Cas2OASysAssessmentInfoTransformer().toAssessmentMetadata(roshRatings),
    overallRisk = Cas2OASysRiskLevel.forValue(roshRatings?.rosh?.determineOverallRiskLevel()?.name),
    riskToChildren = Cas2OASysRiskLevel.forValue(roshRatings?.rosh?.riskChildrenCommunity?.name),
    riskToPublic = Cas2OASysRiskLevel.forValue(roshRatings?.rosh?.riskPublicCommunity?.name),
    riskToKnownAdult = Cas2OASysRiskLevel.forValue(roshRatings?.rosh?.riskKnownAdultCommunity?.name),
    riskToStaff = Cas2OASysRiskLevel.forValue(roshRatings?.rosh?.riskStaffCommunity?.name),
  )
}
