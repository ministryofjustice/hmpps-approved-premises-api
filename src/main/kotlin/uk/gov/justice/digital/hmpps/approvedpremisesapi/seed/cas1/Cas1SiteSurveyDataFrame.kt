package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1

import org.jetbrains.kotlinx.dataframe.AnyCol
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.getColumn
import org.jetbrains.kotlinx.dataframe.name
import kotlin.math.floor

data class Cas1SiteSurveyDataFrame(
  val dataFrame: DataFrame<*>,
  val sheetName: String,
) {

  sealed interface QuestionToMatch {
    data class Exact(val label: String) : QuestionToMatch
    data class StartsWith(val label: String) : QuestionToMatch
  }

  fun resolveAnswer(question: QuestionToMatch, answerCol: Int = 1): String = resolveAnswerOptional(question, answerCol) ?: error("Answer for question '$question' on sheet $sheetName cannot be blank")

  fun resolveAnswerOptional(question: QuestionToMatch, answerCol: Int = 1): String? {
    val questions = dataFrame.getColumn(0).toListIncludingHeader()
    val answers = dataFrame.getColumn(answerCol).toListIncludingHeader()
    val questionsAndAnswers = questions.zip(answers)

    val answer = when (question) {
      is QuestionToMatch.Exact -> {
        questionsAndAnswers.firstOrNull { it.first.toString() == question.label }?.second
          ?: error("Couldn't find a single answer for question '$question' on sheet $sheetName")
      }
      is QuestionToMatch.StartsWith -> {
        val matchingEntries = questionsAndAnswers.filter { it.first.toString().startsWith(question.label) }
        if (matchingEntries.size != 1) {
          error("Couldn't find a single answer for question '$question' on sheet $sheetName")
        }
        matchingEntries[0].second
      }
    }

    fun removeDecimalPlaces() = answer.let {
      if (it is Double) it.toNumberWithNoRedundantDecimalPlaces() else it
    }.toString().trim()

    val normalisedAnswer = removeDecimalPlaces()

    if (normalisedAnswer.isBlank()) {
      return null
    }

    return normalisedAnswer
  }

  fun resolveAnswerYesNoDropDown(question: QuestionToMatch, answerCol: Int = 1): Boolean = when (val answer = resolveAnswer(question, answerCol).uppercase()) {
    "YES" -> true
    "NO" -> false
    else -> error("Invalid value for Yes/No dropdown: $answer on sheet $sheetName. Question is $question")
  }

  fun resolveAnswerYesNoNaDropDown(question: QuestionToMatch, answerCol: Int = 1): Boolean {
    val answer = resolveAnswer(question, answerCol).uppercase()
    return when (answer) {
      "YES" -> true
      "NO" -> false
      "N/A" -> false
      else -> error("Invalid value for Yes/No/N/A dropdown: $answer on sheet $sheetName. Question is $question")
    }
  }

  /**
   Data frame assumes the first row is a header, and assigns it to name
   In our case we don't have a header, so this function produces a list
   which includes the header followed by all other column values
   **/
  private fun AnyCol.toListIncludingHeader() = listOf(this.name) + this.values().toList()

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
  private fun Double.toNumberWithNoRedundantDecimalPlaces(): Any = if (floor(this) == this) {
    this.toInt()
  } else {
    this
  }
}
