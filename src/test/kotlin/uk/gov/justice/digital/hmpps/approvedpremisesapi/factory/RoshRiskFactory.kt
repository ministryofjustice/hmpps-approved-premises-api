package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RoshRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate

class RoshRiskFactory : Factory<RoshRisks> {
  private var overallRisk: Yielded<String> = { randomStringMultiCaseWithNumbers(20) }
  private var riskToChildren: Yielded<String> = { randomStringMultiCaseWithNumbers(20) }
  private var riskToPublic: Yielded<String> = { randomStringMultiCaseWithNumbers(20) }
  private var riskToKnownAdult: Yielded<String> = { randomStringMultiCaseWithNumbers(20) }
  private var riskToStaff: Yielded<String> = { randomStringMultiCaseWithNumbers(20) }
  private var lastUpdated: Yielded<LocalDate> = { LocalDate.now() }

  fun withOverallRisk(overallRisk: String) = apply { this.overallRisk = { overallRisk } }

  override fun produce(): RoshRisks = RoshRisks(
    overallRisk = this.overallRisk(),
    riskToChildren = this.riskToChildren(),
    riskToPublic = this.riskToPublic(),
    riskToKnownAdult = this.riskToKnownAdult(),
    riskToStaff = this.riskToStaff(),
    lastUpdated = this.lastUpdated(),

  )
}
