package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2ApplicationSubmittedEventDetailsSubmittedBy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.PersonReference
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param applicationId 
 * @param applicationUrl 
 * @param personReference 
 * @param submittedAt 
 * @param submittedBy 
 * @param referringPrisonCode 
 * @param preferredAreas 
 * @param hdcEligibilityDate 
 * @param conditionalReleaseDate 
 */
data class Cas2ApplicationSubmittedEventDetails(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("applicationId", required = true) val applicationId: java.util.UUID,

    @Schema(example = "https://community-accommodation-tier-2-dev.hmpps.service.justice.gov.uk/applications/484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "")
    @get:JsonProperty("applicationUrl", required = true) val applicationUrl: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("personReference", required = true) val personReference: PersonReference,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("submittedAt", required = true) val submittedAt: java.time.Instant,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("submittedBy", required = true) val submittedBy: Cas2ApplicationSubmittedEventDetailsSubmittedBy,

    @Schema(example = "BRI", description = "")
    @get:JsonProperty("referringPrisonCode") val referringPrisonCode: kotlin.String? = null,

    @Schema(example = "Leeds | Bradford", description = "")
    @get:JsonProperty("preferredAreas") val preferredAreas: kotlin.String? = null,

    @Schema(example = "Thu Mar 30 01:00:00 BST 2023", description = "")
    @get:JsonProperty("hdcEligibilityDate") val hdcEligibilityDate: java.time.LocalDate? = null,

    @Schema(example = "Sun Apr 30 01:00:00 BST 2023", description = "")
    @get:JsonProperty("conditionalReleaseDate") val conditionalReleaseDate: java.time.LocalDate? = null
) {

}

