package lexer

import org.junit.Test
import kotlin.test.assertEquals

class LexerTest {
    @Test
    fun keyword() {
        val result = Lexer("""
            extend
            schema
            query
            mutation
            subscription
            type
            input
            enum
            union
            interface
            scalar
            directive
            fragment
            on
            implements
        """.trimIndent()).toList()

        assertEquals(
            arrayListOf(
                KeywordToken(kind = KeywordKind.Extend),
                KeywordToken(kind = KeywordKind.Schema),
                KeywordToken(kind = KeywordKind.Query),
                KeywordToken(kind = KeywordKind.Mutation),
                KeywordToken(kind = KeywordKind.Subscription),
                KeywordToken(kind = KeywordKind.Type),
                KeywordToken(kind = KeywordKind.Input),
                KeywordToken(kind = KeywordKind.Enum),
                KeywordToken(kind = KeywordKind.Union),
                KeywordToken(kind = KeywordKind.Interface),
                KeywordToken(kind = KeywordKind.Scalar),
                KeywordToken(kind = KeywordKind.Directive),
                KeywordToken(kind = KeywordKind.Fragment),
                KeywordToken(kind = KeywordKind.On),
                KeywordToken(kind = KeywordKind.Implements),
            ),
            result
        )
    }

    @Test
    fun primitive() {
        val result = Lexer("""
            true
            false
            123
            123.456
            "StringValue"
        """.trimIndent()).toList()

        assertEquals(
            arrayListOf(
                BooleanLiteralToken(true),
                BooleanLiteralToken(false),
                IntegerLiteralToken(123),
                FloatLiteralToken(123.456),
                StringLiteralToken("StringValue"),
            ),
            result
        )
    }

    @Test
    fun symbol() {
        val result = Lexer("... ( ) { } [ ] | = ${'$'} @ . : !".trimIndent()).toList()

        assertEquals(
            arrayListOf(
                SymbolToken(SymbolKind.DotDotDot),
                SymbolToken(SymbolKind.LeftParens),
                SymbolToken(SymbolKind.RightParens),
                SymbolToken(SymbolKind.LeftBrace),
                SymbolToken(SymbolKind.RightBrace),
                SymbolToken(SymbolKind.LeftBracket),
                SymbolToken(SymbolKind.RightBracket),
                SymbolToken(SymbolKind.Pipeline),
                SymbolToken(SymbolKind.Equal),
                SymbolToken(SymbolKind.Dollar),
                SymbolToken(SymbolKind.At),
                SymbolToken(SymbolKind.Dot),
                SymbolToken(SymbolKind.Colon),
                SymbolToken(SymbolKind.Exclamation),
            ),
            result
        )
    }
}
