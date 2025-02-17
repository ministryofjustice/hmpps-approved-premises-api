package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

fun <K, V> mapOfNonNullValues(vararg pairs: Pair<K, V>): Map<K, V> = mapOf(*pairs).filter { it.value != null }
