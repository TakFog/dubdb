package takutility.dubdb.util

import java.util.regex.Pattern

fun bow(name: String?): Sequence<String> {
    if (name == null) return sequenceOf()
    return name.splitToSequence(" ")
        .filter(String::isNotEmpty)
        .filter { it.any(Char::isLetter) }
        .map(String::lowercase)
        .distinct()
}

private val charnameSplit = Pattern.compile("[/;,]+")

fun splitCharacter(name: String): Array<String> = charnameSplit.split(name)