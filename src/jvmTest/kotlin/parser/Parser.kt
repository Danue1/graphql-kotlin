package parser

import ast.*
import lexer.Lexer
import org.junit.Test
import kotlin.test.assertEquals

class ParserTest {
    @Test
    fun implicitQuery() {
        val schema = """
            enum Platform {
                Web
                iOS
                Android
            }
        """.trimIndent()

        assertEquals(
            DocumentNode(
                arrayListOf(
                    EnumTypeDefinitionNode(
                        null,
                        NameNode("Platform"),
                        listOf(),
                        listOf(
                            EnumValueDefinitionNode(
                                null,
                                NameNode("Web"),
                                listOf(),
                            ),
                            EnumValueDefinitionNode(
                                null,
                                NameNode("iOS"),
                                listOf(),
                            ),
                            EnumValueDefinitionNode(
                                null,
                                NameNode("Android"),
                                listOf(),
                            ),
                        ),
                    ),
                ),
            ),
            Parser(Lexer(schema).toList()).parse(),
        )
    }
}
