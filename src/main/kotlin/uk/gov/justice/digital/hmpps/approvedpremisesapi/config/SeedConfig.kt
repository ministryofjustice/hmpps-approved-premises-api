package uk.gov.justice.digital.hmpps.approvedpremisesapi.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "seed")
class SeedConfig {
  lateinit var filePrefix: String
  var onStartup: SeedOnStartupConfig = SeedOnStartupConfig()
}

class SeedOnStartupConfig {
  var enabled: Boolean = false
  var filePrefixes: List<String> = listOf()
  var script: StartupScriptConfig = StartupScriptConfig()
}

class StartupScriptConfig {
  var cas1Enabled: Boolean = false
  var cas2Enabled: Boolean = false
  var cas2v2Enabled: Boolean = false
  var noms: String = "A1234AI"
  var prisonCode: String = "LEI"
}
