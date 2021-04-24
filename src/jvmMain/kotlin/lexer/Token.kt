package lexer

sealed class Token

data class SymbolToken(val kind: SymbolKind): Token()

data class DescriptionToken(val value: String): Token()

sealed class LiteralToken: Token()

data class StringLiteralToken(val value: String): LiteralToken()

data class FloatLiteralToken(val value: Double): LiteralToken()

data class IntegerLiteralToken(val value: Int): LiteralToken()

data class BooleanLiteralToken(val value: Boolean): LiteralToken()

object NullLiteralToken: LiteralToken()

data class KeywordToken(val kind: KeywordKind): Token()

data class IdentifierToken(val value: String): Token()

data class IllegalToken(val value: Char): Token()

enum class KeywordKind {
    Extend,
    Schema,
    Query,
    Mutation,
    Subscription,
    Type,
    Input,
    Enum,
    Union,
    Interface,
    Scalar,
    Directive,
    Fragment,
    On,
    Implements,
    Repeatable,
}

enum class SymbolKind {
    LeftParens, // )
    RightParens, // )
    LeftBrace, // {
    RightBrace, // }
    LeftBracket, // [
    RightBracket, // ]
    Pipeline, // |
    Equal, // =
    Ampersand, // &
    Dollar, // $
    At, // @
    Dot, // .
    DotDotDot, // ...
    Colon, // :
    Exclamation, // !
}
