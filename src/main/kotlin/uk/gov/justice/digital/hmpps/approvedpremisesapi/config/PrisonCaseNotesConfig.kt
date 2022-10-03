package uk.gov.justice.digital.hmpps.approvedpremisesapi.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "prison-case-notes")
class PrisonCaseNotesConfigBindingModel {
  var lookbackDays: Int? = null
  var prisonApiPageSize: Int? = null
  var excludedCategories: List<ExcludedCategoryBindingModel>? = null
}

data class PrisonCaseNotesConfig(
  val lookbackDays: Int,
  val prisonApiPageSize: Int,
  val excludedCategories: List<ExcludedCategory>
)

class ExcludedCategoryBindingModel {
  var category: String? = null
  var subcategory: String? = null
    set(value) {
      field = if (value == "") null else value
    }
}

data class ExcludedCategory(
  val category: String,
  val subcategory: String?
) {
  fun excluded(otherCategory: String, otherSubcategory: String) = (otherCategory == category && subcategory == null) || (otherCategory == category && otherSubcategory == subcategory)
}
