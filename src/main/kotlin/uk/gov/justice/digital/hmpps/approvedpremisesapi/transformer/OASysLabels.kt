package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

@SuppressWarnings("MagicNumber")
object OASysLabels {
  val sectionToLabel = mapOf(
    3 to "Accommodation",
    4 to "Education, Training and Employment",
    5 to "Finance",
    6 to "Relationships",
    7 to "Lifestyle",
    10 to "Emotional",
    11 to "Thinking and Behavioural",
    12 to "Attitude",
    13 to "Health",
  )

  val questionToLabel = mapOf(
    "3.9" to "Accommodation issues contributing to risks of offending and harm",
    "4.9" to "Education, training and employability issues contributing to risks of offending and harm",
    "5.9" to "Financial management issues contributing to risks of offending and harm",
    "6.9" to "Relationship issues contributing to risks of offending and harm",
    "7.9" to "Lifestyle issues contributing to risks of offending and harm",
    "8.9" to "Drug misuse issues contributing to risks of offending and harm",
    "9.9" to "Alcohol misuse issues contributing to risks of offending and harm",
    "10.9" to "Issues of emotional well-being contributing to risks of offending and harm",
    "11.9" to "Thinking / behavioural issues contributing to risks of offending and harm",
    "12.9" to "Issues about attitudes contributing to risks of offending and harm",
  )
}
