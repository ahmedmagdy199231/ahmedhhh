package app.lovable.giant.data.models

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val answerIndex: Int
)

data class PatternItem(
    val sequence: List<Int>,
    val options: List<Int>,
    val answerIndex: Int
)

data class CharacterItem(
    val hints: List<String>,
    val name: String,
    val options: List<String>
)

data class CountryItem(
    val flag: String,
    val name: String,
    val options: List<String>
)

data class FastQuestion(
    val question: String,
    val options: List<String>,
    val answerIndex: Int
)
