package uk.gov.justice.digital.hmpps.approvedpremisesapi.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType

enum class ApprovedPremisesType {
  NORMAL,
  PIPE,
  ESAP,
  RFAP,
  MHAP_ST_JOSEPHS,
  MHAP_ELLIOTT_HOUSE,
}

fun ApType.asApprovedPremisesType() = when (this) {
  ApType.NORMAL -> ApprovedPremisesType.NORMAL
  ApType.PIPE -> ApprovedPremisesType.PIPE
  ApType.ESAP -> ApprovedPremisesType.ESAP
  ApType.RFAP -> ApprovedPremisesType.RFAP
  ApType.MHAP_ST_JOSEPHS -> ApprovedPremisesType.MHAP_ST_JOSEPHS
  ApType.MHAP_ELLIOTT_HOUSE -> ApprovedPremisesType.MHAP_ELLIOTT_HOUSE
}

fun ApprovedPremisesType.asApiType() = when (this) {
  ApprovedPremisesType.NORMAL -> ApType.NORMAL
  ApprovedPremisesType.PIPE -> ApType.PIPE
  ApprovedPremisesType.ESAP -> ApType.ESAP
  ApprovedPremisesType.RFAP -> ApType.RFAP
  ApprovedPremisesType.MHAP_ST_JOSEPHS -> ApType.MHAP_ST_JOSEPHS
  ApprovedPremisesType.MHAP_ELLIOTT_HOUSE -> ApType.MHAP_ELLIOTT_HOUSE
}
