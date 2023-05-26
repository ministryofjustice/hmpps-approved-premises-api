package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysQuestion

abstract class OASysTransformer {
  protected fun oASysQuestionWithSingleAnswer(label: String, questionNumber: String, answer: String?) = OASysQuestion(
    label = label,
    questionNumber = questionNumber,
    answer = answer,
  )
}
