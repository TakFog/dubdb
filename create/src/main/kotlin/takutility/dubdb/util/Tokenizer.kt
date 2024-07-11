package takutility.dubdb.util

fun bow(name: String?): Sequence<String> {
    if (name == null) return sequenceOf()
    return name.splitToSequence(" ")
        .filter(String::isNotEmpty)
        .filter { it.any(Char::isLetter) }
        .map(String::lowercase)
        .distinct()
}