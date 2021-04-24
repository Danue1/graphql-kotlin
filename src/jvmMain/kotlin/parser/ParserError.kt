package parser

import lexer.Token

sealed class ParserError: Throwable()

data class UnexpectedToken(val token: Token?): ParserError()
