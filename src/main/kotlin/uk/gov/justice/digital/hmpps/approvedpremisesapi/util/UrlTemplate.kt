package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

class UrlTemplate(val template: String) {
  fun resolve(args: Map<String, String>) =
    args.entries.fold(template) { acc, (key, value) -> acc.replace("#$key", value) }
}
