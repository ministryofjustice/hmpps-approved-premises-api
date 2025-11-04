package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProbationDeliveryUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.User

/**
 *
 * @param roles
 */
data class TemporaryAccommodationUser(

  val roles: kotlin.collections.List<TemporaryAccommodationUserRole>,

  override val service: kotlin.String,

  override val id: java.util.UUID,

  override val name: kotlin.String,

  override val deliusUsername: kotlin.String,

  override val region: ProbationRegion,

  override val email: kotlin.String? = null,

  override val telephoneNumber: kotlin.String? = null,

  override val isActive: kotlin.Boolean? = null,

  override val probationDeliveryUnit: ProbationDeliveryUnit? = null,
) : User
