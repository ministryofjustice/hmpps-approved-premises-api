package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReferralHistoryNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReferralHistoryNoteMessageDetails
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 */
class ReferralHistoryDomainEventNote(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("id", required = true) override val id: java.util.UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("createdAt", required = true) override val createdAt: java.time.Instant,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("createdByUserName", required = true) override val createdByUserName: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("type", required = true) override val type: kotlin.String,

    @Schema(example = "null", description = "")
    @get:JsonProperty("message") override val message: kotlin.String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("messageDetails") override val messageDetails: ReferralHistoryNoteMessageDetails? = null
) : ReferralHistoryNote{

}

