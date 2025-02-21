package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

@Component
class ApplicationContextProvider(applicationContext: ApplicationContext) {
  companion object {
    private lateinit var instance: ApplicationContext

    fun get(): ApplicationContext = instance
  }

  init {
    instance = applicationContext
  }
}
