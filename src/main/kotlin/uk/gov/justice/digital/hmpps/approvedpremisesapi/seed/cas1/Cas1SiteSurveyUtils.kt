package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1

import org.jetbrains.kotlinx.dataframe.AnyCol
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.getColumn
import org.jetbrains.kotlinx.dataframe.name
import kotlin.math.floor

object Cas1SiteSurveyUtils {

  /**
   Data frame assumes the first row is a header, and assigns it to name
   In our case we don't have a header, so this function produces a list
   which includes the header followed by all other column values
   **/
  private fun AnyCol.toListIncludingHeader() = listOf(this.name) + this.values().toList()

  fun DataFrame<*>.resolveAnswer(question: String, answerCol: Int = 1): String = resolveOptionalAnswer(question, answerCol) ?: error("Answer for question '$question' cannot be blank")

  fun DataFrame<*>.resolveOptionalAnswer(question: String, answerCol: Int = 1): String? {
    val questions = getColumn(0).toListIncludingHeader()
    val answers = getColumn(answerCol).toListIncludingHeader()

    val questionIndex = questions.indexOf(question)

    if (questionIndex == -1) error("Question '$question' not found on sheet Sheet3.")

    fun removeDecimalPlaces() = answers[questionIndex].let {
      if (it is Double) it.toNumberWithNoRedundantDecimalPlaces() else it
    }.toString().trim()

    val answer = removeDecimalPlaces()

    return answer.ifBlank {
      null
    }
  }

  fun DataFrame<*>.resolveAnswerYesNoDropDown(question: String, answerCol: Int = 1): Boolean {
    val answer = resolveAnswer(question, answerCol).uppercase()
    return when (answer) {
      "YES" -> true
      "NO" -> false
      else -> error("Invalid value for Yes/No dropdown: $answer. Question is $question")
    }
  }

  fun DataFrame<*>.resolveAnswerYesNoNaDropDown(question: String, answerCol: Int = 1): Boolean {
    val answer = resolveAnswer(question, answerCol).uppercase()
    return when (answer) {
      "YES" -> true
      "NO" -> false
      "N/A" -> false
      else -> error("Invalid value for Yes/No/N/A dropdown: $answer. Question is $question")
    }
  }

  /**
   * When 'general' types are read by Dataframe they're assumed to be
   * Doubles. If converted to a String, a decimal place will always
   * be present e.g. '1' in Excel will appear as '1.0'. To avoid this
   * issue this code strips out redundant decimal places whilst retaining
   * non 0 decimal places. This is an imperfect solution because if a
   * site survey legitimately included a numeric value of 1.0, this would
   * be truncated to '1'. In this case we could look at using POI directly
   * which doesn't have that issue (see example code on APS-1933)
   */
  fun Double.toNumberWithNoRedundantDecimalPlaces(): Any = if (floor(this) == this) {
    this.toInt()
  } else {
    this
  }
}
