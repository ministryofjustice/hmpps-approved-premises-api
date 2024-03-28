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
  ApType.normal -> ApprovedPremisesType.NORMAL
  ApType.pipe -> ApprovedPremisesType.PIPE
  ApType.esap -> ApprovedPremisesType.ESAP
  ApType.rfap -> ApprovedPremisesType.RFAP
  ApType.mhapStJosephs -> ApprovedPremisesType.MHAP_ST_JOSEPHS
  ApType.mhapElliottHouse -> ApprovedPremisesType.MHAP_ELLIOTT_HOUSE
}

fun ApprovedPremisesType.asApiType() = when (this) {
  ApprovedPremisesType.NORMAL -> ApType.normal
  ApprovedPremisesType.PIPE -> ApType.pipe
  ApprovedPremisesType.ESAP -> ApType.esap
  ApprovedPremisesType.RFAP -> ApType.rfap
  ApprovedPremisesType.MHAP_ST_JOSEPHS -> ApType.mhapStJosephs
  ApprovedPremisesType.MHAP_ELLIOTT_HOUSE -> ApType.mhapElliottHouse
}
