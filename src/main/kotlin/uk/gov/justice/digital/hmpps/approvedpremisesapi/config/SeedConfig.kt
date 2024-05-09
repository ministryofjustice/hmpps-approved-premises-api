package uk.gov.justice.digital.hmpps.approvedpremisesapi.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "seed")
class SeedConfig {
  lateinit var filePrefix: String
  var auto: AutoSeedConfig = AutoSeedConfig()
  var autoScript: AutoScriptConfig = AutoScriptConfig()
}

class AutoSeedConfig {
  var enabled: Boolean = false
  var filePrefixes: List<String> = listOf()
}

class AutoScriptConfig {
  var cas1Enabled: Boolean = false
  var cas2Enabled: Boolean = false
  var noms: String = "A1234AI"
}
