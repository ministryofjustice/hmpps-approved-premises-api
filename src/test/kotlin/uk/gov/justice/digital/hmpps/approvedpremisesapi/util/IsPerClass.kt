package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtensionContext

fun isPerClass(context: ExtensionContext?) = context?.testInstanceLifecycle?.get() == TestInstance.Lifecycle.PER_CLASS
