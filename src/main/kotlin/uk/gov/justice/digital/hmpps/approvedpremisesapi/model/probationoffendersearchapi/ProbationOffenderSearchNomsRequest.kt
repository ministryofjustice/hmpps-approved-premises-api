package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.probationoffendersearchapi

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude
data class ProbationOffenderSearchNomsRequest(val nomsNumber: String)
