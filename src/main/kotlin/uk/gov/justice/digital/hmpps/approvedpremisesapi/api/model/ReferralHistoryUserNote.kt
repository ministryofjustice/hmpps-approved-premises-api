package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

/**
 *
 */
class ReferralHistoryUserNote(

  override val id: java.util.UUID,

  override val createdAt: java.time.Instant,

  override val createdByUserName: kotlin.String,

  override val type: kotlin.String,

  override val message: kotlin.String? = null,

  override val messageDetails: ReferralHistoryNoteMessageDetails? = null,
) : ReferralHistoryNote
