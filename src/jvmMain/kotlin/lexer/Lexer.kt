package lexer

import java.util.*

private const val DESCRIPTION_START = "\"\"\""

private const val DESCRIPTION_END = "\"\"\""

class Lexer(private val source: String): Enumeration<Token> {
    private var position = 0

    override fun hasMoreElements(): Boolean =
        this.hasMoreToken()

    override fun nextElement(): Token =
        this.lex()

    private fun lex(): Token =
        this.skipIgnoreTokens().let {
            this.tryToDescription()
                ?: this.tryToSymbol()
                ?: this.tryToLiteral()
                ?: this.tryToIdentifier()?.let {
                    this.tryToBooleanLiteral(it)
                        ?: this.tryToNullLiteral(it)
                        ?: this.tryToKeyword(it)
                        ?: IdentifierToken(it)
                }
                ?: this.tryToIllegal()
        }

    private fun tryToDescription(): Token? =
        when {
            this.expect(DESCRIPTION_START) -> {
                val description = this.consumeWhile(::isNotLineTerminator)
                when {
                    this.expect(DESCRIPTION_END)
                        .runIfTrue { this.consume(DESCRIPTION_END) } -> DescriptionToken(description)
                    else -> tryToIllegal()
                }
            }
            else -> null
        }

    private fun tryToSymbol(): SymbolToken? =
        when {
            this.expect("...").runIfTrue { this.consume(2) } -> SymbolToken(SymbolKind.DotDotDot)
            this.expect('(') -> SymbolToken(SymbolKind.LeftParens)
            this.expect(')') -> SymbolToken(SymbolKind.RightParens)
            this.expect('{') -> SymbolToken(SymbolKind.LeftBrace)
            this.expect('}') -> SymbolToken(SymbolKind.RightBrace)
            this.expect('[') -> SymbolToken(SymbolKind.LeftBracket)
            this.expect(']') -> SymbolToken(SymbolKind.RightBracket)
            this.expect('|') -> SymbolToken(SymbolKind.Pipeline)
            this.expect('=') -> SymbolToken(SymbolKind.Equal)
            this.expect('&') -> SymbolToken(SymbolKind.Ampersand)
            this.expect('$') -> SymbolToken(SymbolKind.Dollar)
            this.expect('@') -> SymbolToken(SymbolKind.At)
            this.expect('.') -> SymbolToken(SymbolKind.Dot)
            this.expect(':') -> SymbolToken(SymbolKind.Colon)
            this.expect('!') -> SymbolToken(SymbolKind.Exclamation)
            else -> null
        }?.also { this.consume() }

    private fun tryToLiteral(): Token? =
        this.stringLiteral() ?: this.numericLiteral()

    private fun stringLiteral(): Token? =
        when {
            this.expect(::isDoubleQuote).runIfTrue { this.consume() } -> {
                val value = this.consumeWhile { !isDoubleQuote(it) && isNotLineTerminator(it) }

                when {
                    this.expect(::isDoubleQuote).runIfTrue { this.consume() } -> StringLiteralToken(value)
                    else -> this.tryToIllegal()
                }
            }
            else -> null
        }

    private fun numericLiteral(): Token? =
        when {
            this.expect(::isNumeric) -> {
                val numerator = this.consumeWhile{ isNumeric(it) && isNotLineTerminator(it) }

                when {
                    this.expect { it == '.' }.runIfTrue { this.consume() } -> {
                        val denominator = this.consumeWhile { isNumeric(it) && isNotLineTerminator(it) }

                        when {
                            denominator.isNotEmpty() -> {
                                val value = "$numerator.$denominator".toDoubleOrNull()

                                value?.let(::FloatLiteralToken) ?: this.tryToIllegal()
                            }
                            else -> this.tryToIllegal()
                        }
                    }
                    else -> numerator.toIntOrNull()?.let(::IntegerLiteralToken) ?: this.tryToIllegal()
                }
            }
            else -> null
        }

    private fun tryToIdentifier(): String? =
        when {
            this.expect(::isAlphabet) -> {
                val head = this.consumeWhile(::isAlphabet)
                val tail = this.consumeWhile(::isIdentifier)

                "$head$tail"
            }
            else -> null
        }

    private fun tryToBooleanLiteral(token: String): BooleanLiteralToken? =
        when (token) {
            "true" -> BooleanLiteralToken(true)
            "false" -> BooleanLiteralToken(false)
            else -> null
        }

    private fun tryToNullLiteral(token: String): NullLiteralToken? =
        when (token) {
            "null" -> NullLiteralToken
            else -> null
        }

    private fun tryToKeyword(token: String): KeywordToken? =
        when (token) {
            "extend" -> KeywordToken(KeywordKind.Extend)
            "schema" -> KeywordToken(KeywordKind.Schema)
            "query" -> KeywordToken(KeywordKind.Query)
            "mutation" -> KeywordToken(KeywordKind.Mutation)
            "subscription" -> KeywordToken(KeywordKind.Subscription)
            "type" -> KeywordToken(KeywordKind.Type)
            "input" -> KeywordToken(KeywordKind.Input)
            "enum" -> KeywordToken(KeywordKind.Enum)
            "union" -> KeywordToken(KeywordKind.Union)
            "interface" -> KeywordToken(KeywordKind.Interface)
            "scalar" -> KeywordToken(KeywordKind.Scalar)
            "directive" -> KeywordToken(KeywordKind.Directive)
            "fragment" -> KeywordToken(KeywordKind.Fragment)
            "on" -> KeywordToken(KeywordKind.On)
            "implements" -> KeywordToken(KeywordKind.Implements)
            "repeatable" -> KeywordToken(KeywordKind.Repeatable)
            else -> null
        }

    private fun tryToIllegal(): IllegalToken =
        this.subSequence
            .first()
            .also { this.consume() }
            .let(::IllegalToken)

    private fun hasMoreToken(): Boolean =
        this.position < this.source.length

    private fun skipIgnoreTokens(): Unit {
        @Suppress("ControlFlowWithEmptyBody")
        while (skipIgnoreToken()) { }
    }

    private fun skipIgnoreToken(): Boolean =
        this.skipUnicodeBOM()
            || this.skipWhitespace()
            || this.skipLineDelimiter()
            || this.skipComment()
            || this.skipComma()

    private fun skipUnicodeBOM(): Boolean =
        this.skipCountBy { first ->
            when (first) {
                "uFEFF" -> 5
                else -> 0
            }
        }

    private fun skipWhitespace(): Boolean =
        this.skipWhile { " \t".contains(it) }

    private fun skipLineDelimiter(): Boolean =
        this.skipWhile(::isLineTerminator)

    private fun skipComment(): Boolean =
        when {
            this.expect('#').runIfTrue { this.consume() } -> this.skipWhile(::isNotLineTerminator)
            else -> false
        }

    private fun skipComma(): Boolean =
        this.skipWhile { it == ',' }

    private inline fun skipWhile(by: (Char) -> Boolean): Boolean =
        this.subSequence
            .takeWhile(by)
            .count()
            .also { this.consume(it) }
            .let { it != 0 }

    private inline fun skipCountBy(by: (String) -> Int): Boolean =
        by(this.source)
            .also { this.consume(it) }
            .let { it != 0 }

    private inline fun expect(by: (Char) -> Boolean): Boolean =
        when {
            this.subSequence.isNotEmpty() -> by(this.subSequence.first())
            else -> false
        }

    private fun expect(char: Char): Boolean =
        this.expect { it == char }

    private fun expect(token: String): Boolean =
        this.subSequence.startsWith(token)

    private fun consume(token: String): Unit =
        this.consume(token.length)

    private fun consume(): Unit =
        this.consume(1)

    private fun consume(count: Int): Unit {
        this.position += count
    }

    private inline fun consumeWhile(by: (Char) -> Boolean): String =
        this.subSequence
            .takeWhile(by)
            .toString()
            .also { this.consume(it) }

    private val subSequence
        get (): CharSequence
            = this.source.subSequence(this.position, this.source.length)
}

fun isLineTerminator(char: Char): Boolean
    = "\r\n".contains(char)

fun isNotLineTerminator(char: Char): Boolean
    = !isLineTerminator(char)

fun isDoubleQuote(char: Char): Boolean
    = char == '"'

fun isNumeric(char: Char): Boolean
    = ('0'..'9').contains(char)

fun isAlphabet(char: Char): Boolean
    = ('a'..'z').contains(char) || ('A'..'Z').contains(char)

fun isIdentifier(char: Char): Boolean
    = isNumeric(char) || isAlphabet(char) || char == '_'

inline fun Boolean.runIfTrue(func: () -> Unit): Boolean {
    if (this) {
        func()
    }
    return this
}
