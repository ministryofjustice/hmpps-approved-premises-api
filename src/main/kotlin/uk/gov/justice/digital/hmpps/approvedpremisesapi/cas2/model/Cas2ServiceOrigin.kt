package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model

/**
 * The Entity Model is multi-tenanted, with the Service Origin value used to determine if a given entry corresponds to
 * CAS2 HDC (the /cas2 API) or CAS2 Bail (the /cas2v2 API).
 *
 * This value is typically hardcoded into business logic that is specific to CAS2 HDC or CAS2 Bail.
 *
 * Whilst this information could be arguably derived from the [uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin],
 * we deliberately maintain these as separate fields to simplify how to identify which API/UI this
 * record originated from
 */
enum class Cas2ServiceOrigin {
  BAIL,
  HDC,
}
