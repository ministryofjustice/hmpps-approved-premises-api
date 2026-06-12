package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Supporting information questions only apply to the supportingInformation answers group
 *
 * The answers tend to span across many sections in OAsys, so are identified by a unique section number (1 per section)
 *
 * If a question is optional it will not be returned by calls to the answers endpoint unless the user
 * elects to include it e.g. the following will include optional answers for questions 10 and 12:
 *
 * `GET /cas1/people/{crn}/oasys/answers?group=supportingInformation&includeOptionalSections=10,12`
 */
data class Cas1OASysSupportingInformationQuestionMetaData(

  @Schema(example = "10", required = true, description = "The OAsys section that this question relates to")
  val section: Int,

  val sectionLabel: String,

  @Schema(
    example = "null",
    required = true,
    description = "If false this question/answer will always be returned for the supportingInformation answers. Otherwise it has to be explicitly included",
  )
  val inclusionOptional: Boolean,

  @Schema(example = "null", description = "If the response to this question in OASys for the person has been identified as 'linked to harm'")
  val oasysAnswerLinkedToHarm: Boolean? = null,

  @Schema(example = "null", description = "If the response to this question in OAsys for the person hsa been identified as 'linked to re-offending'")
  val oasysAnswerLinkedToReOffending: Boolean? = null,
)
