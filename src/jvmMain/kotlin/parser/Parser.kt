package parser

import ast.*
import lexer.*

class Parser(private val tokenList: List<Token>) {
    private var position = 0

    fun parse(): DocumentNode =
        mutableListOf<DefinitionNode>().let { definitionList ->
            while (this.hasMoreDefinition()) {
                definitionList += this.nextDefinition()
            }

            DocumentNode(definitionList)
        }

    private fun hasMoreDefinition(): Boolean =
        this.position < this.tokenList.size

    private fun nextDefinition(): DefinitionNode =
        this.tryToExecutableDefinition()
            ?: this.tryToTypeSystemDefinition()
            ?: this.tryToTypeSystemExtension()
            ?: this.unexpectedToken()

    private fun tryToExecutableDefinition(): ExecutableDefinitionNode? =
        this.tryToOperationDefinition()
            ?: this.tryToFragmentDefinition()

    private fun tryToTypeSystemDefinition(): DefinitionNode? =
        this.tryToDescription().let {
            this.tryToSchemaDefinition(it)
                ?: this.tryToTypeDefinition(it)
                ?: this.tryToDirectiveDefinition(it)
        }

    private fun tryToDescription(): StringValueNode? =
        this.tryToStringValue()

    private fun tryToSchemaDefinition(description: StringValueNode?): SchemaDefinitionNode? =
        when {
            this.expectKeyword(KeywordKind.Schema) -> {
                this.consume()
                val directiveList = this.tryToDirectiveList()
                val operationTypeDefinitionList = this.tryToOperationTypeDefinitionList()

                SchemaDefinitionNode(description, directiveList, operationTypeDefinitionList)
            }
            else -> null
        }

    private fun tryToOperationTypeDefinitionList(): List<OperationTypeDefinitionNode> =
        when {
            this.expectSymbol(SymbolKind.LeftBrace) -> {
                this.consume()

                generateSequence { this.tryToOperationTypeDefinition() }.toList().also {
                    when {
                        it.isEmpty() -> this.unexpectedToken()
                        this.expectSymbol(SymbolKind.RightBrace) -> this.consume()
                        else -> this.unexpectedToken()
                    }
                }
            }
            else -> this.unexpectedToken()
        }

    private fun tryToOperationTypeDefinition(): OperationTypeDefinitionNode? =
        when {
            this.expectKeyword(KeywordKind.Query) -> OperationTypeNode.Query
            this.expectKeyword(KeywordKind.Mutation) -> OperationTypeNode.Mutation
            this.expectKeyword(KeywordKind.Subscription) -> OperationTypeNode.Subscription
            else -> null
        }?.let { operation ->
            this.consume()
            when {
                this.expectSymbol(SymbolKind.Colon) -> this.consume()
                else -> this.unexpectedToken()
            }
            val type = this.tryToTypeCondition()

            OperationTypeDefinitionNode(operation, type)
        }

    private fun tryToTypeDefinition(description: StringValueNode?): TypeDefinitionNode? =
        this.tryToScalarTypeDefinition(description)
            ?: this.tryToObjectTypeDefinition(description)
            ?: this.tryToInterfaceTypeDefinition(description)
            ?: this.tryToUnionTypeDefinition(description)
            ?: this.tryToEnumTypeDefinition(description)
            ?: this.tryToInputObjectTypeDefinition(description)

    private fun tryToScalarTypeDefinition(description: StringValueNode?): ScalarTypeDefinitionNode? =
        when {
            this.expectKeyword(KeywordKind.Scalar) -> {
                this.consume()
                val name = this.tryToName() ?: this.unexpectedToken()
                val directiveList = this.tryToDirectiveList()

                ScalarTypeDefinitionNode(description, name, directiveList)
            }
            else -> null
        }

    private fun tryToObjectTypeDefinition(description: StringValueNode?): ObjectTypeDefinitionNode? =
        when {
            this.expectKeyword(KeywordKind.Type) -> {
                this.consume()
                val name = this.tryToName() ?: this.unexpectedToken()
                val interfaceList = this.tryToInterfaceList()
                val directiveList = this.tryToDirectiveList()
                val fieldList = this.tryToFieldDefinitionList()

                ObjectTypeDefinitionNode(description, name, interfaceList, directiveList, fieldList)
            }
            else -> null
        }

    private fun tryToInterfaceTypeDefinition(description: StringValueNode?): InterfaceTypeDefinitionNode? =
        when {
            this.expectKeyword(KeywordKind.Interface) -> {
                this.consume()
                val name = this.tryToName() ?: this.unexpectedToken()
                val interfaceList = this.tryToInterfaceList()
                val directiveList = this.tryToDirectiveList()
                val fieldList = this.tryToFieldDefinitionList()

                InterfaceTypeDefinitionNode(description, name, interfaceList, directiveList, fieldList)
            }
            else -> null
        }

    private fun tryToUnionTypeDefinition(description: StringValueNode?): UnionTypeDefinitionNode? =
        when {
            this.expectKeyword(KeywordKind.Union) -> {
                this.consume()
                val name = this.tryToName() ?: this.unexpectedToken()
                val directiveList = this.tryToDirectiveList()
                when {
                    this.expectSymbol(SymbolKind.Equal) -> this.consume()
                    else -> this.unexpectedToken()
                }
                if (this.expectSymbol(SymbolKind.Pipeline)) {
                    this.consume()
                }
                val typeList = mutableListOf<NamedTypeNode>()
                while (true) {
                    typeList += this.tryToTypeCondition()
                    when {
                        this.expectSymbol(SymbolKind.Pipeline) -> this.consume()
                        else -> break
                    }
                }

                UnionTypeDefinitionNode(description, name, directiveList, typeList)
            }
            else -> null
        }

    private fun tryToEnumTypeDefinition(description: StringValueNode?): EnumTypeDefinitionNode? =
        when {
            this.expectKeyword(KeywordKind.Enum) -> {
                this.consume()
                val name = this.tryToName() ?: this.unexpectedToken()
                val directiveList = this.tryToDirectiveList()
                val valueList = this.tryToEnumValueDefinitionList()

                EnumTypeDefinitionNode(description, name, directiveList, valueList)
            }
            else -> null
        }

    private fun tryToEnumValueDefinitionList(): List<EnumValueDefinitionNode> =
        when {
            this.expectSymbol(SymbolKind.LeftBrace) -> {
                this.consume()

                mutableListOf<EnumValueDefinitionNode>().also { valueList ->
                    while (true) {
                        valueList += this.tryToEnumValueDefinition() ?: break
                    }
                    when {
                        valueList.isEmpty() -> this.unexpectedToken()
                        this.expectSymbol(SymbolKind.RightBrace) -> this.consume()
                        else -> this.unexpectedToken()
                    }
                }
            }
            else -> this.unexpectedToken()
        }

    private fun tryToEnumValueDefinition(): EnumValueDefinitionNode? =
        this.tryToStringValue().let { description ->
            val name = this.tryToName() ?: when (description) {
                null -> return null
                else -> this.unexpectedToken()
            }
            val directiveList = this.tryToDirectiveList()
            EnumValueDefinitionNode(description, name, directiveList)
        }

    private fun tryToInputObjectTypeDefinition(description: StringValueNode?): InputObjectTypeDefinitionNode? =
        when {
            this.expectKeyword(KeywordKind.Input) -> {
                this.consume()
                val name = this.tryToName() ?: this.unexpectedToken()
                val directiveList = this.tryToDirectiveList()
                val fieldList = this.tryToInputValueDefinitionList()

                InputObjectTypeDefinitionNode(description, name, directiveList, fieldList)
            }
            else -> null
        }

    private fun tryToDirectiveDefinition(description: StringValueNode?): DirectiveDefinitionNode? =
        when {
            this.expectKeyword(KeywordKind.Directive) -> {
                this.consume()
                when {
                    this.expectSymbol(SymbolKind.At) -> this.consume()
                    else -> this.unexpectedToken()
                }
                val name = this.tryToName() ?: this.unexpectedToken()
                val argumentList = this.tryToInputValueDefinitionList()
                val repeatable = when {
                    this.expectKeyword(KeywordKind.Repeatable) -> {
                        this.consume()
                        true
                    }
                    else -> false
                }
                when {
                    this.expectKeyword(KeywordKind.On) -> this.consume()
                    else -> this.unexpectedToken()
                }
                if (this.expectSymbol(SymbolKind.Pipeline)) {
                    this.consume()
                }
                val locationList = mutableListOf<NameNode>()
                while (true) {
                    locationList += this.tryToName() ?: break
                    if (this.expectSymbol(SymbolKind.Pipeline)) {
                        this.consume()
                    }
                }
                if (locationList.isEmpty()) {
                    this.unexpectedToken()
                }

                DirectiveDefinitionNode(description, name, argumentList, repeatable, locationList)
            }
            else -> null
        }

    private fun tryToInputValueDefinitionList(): List<InputValueDefinitionNode> =
        when {
            this.expectSymbol(SymbolKind.LeftBrace) -> {
                this.consume()

                generateSequence { this.tryToInputValueDefinition() }.toList().also {
                    when {
                        it.isEmpty() -> this.unexpectedToken()
                        this.expectSymbol(SymbolKind.RightBrace) -> this.consume()
                        else -> this.unexpectedToken()
                    }
                }
            }
            else -> listOf()
        }

    private fun tryToInputValueDefinition(): InputValueDefinitionNode? {
        val description = when {
            this.expectSymbol(SymbolKind.DotDotDot) -> this.tryToStringValue()
            else -> null
        }
        val name = this.tryToName() ?: when (description) {
            null -> return null
            else -> this.unexpectedToken()
        }
        when {
            this.expectSymbol(SymbolKind.Colon) -> this.consume()
            else -> this.unexpectedToken()
        }
        val type = this.tryToType() ?: this.unexpectedToken()
        val defaultValue = when {
            this.expectSymbol(SymbolKind.Equal) -> {
                this.consume()

                this.tryToValue()
            }
            else -> null
        }
        val directiveList = this.tryToDirectiveList()

        return InputValueDefinitionNode(description, name, type, defaultValue, directiveList)
    }

    private fun tryToInterfaceList(): List<NamedTypeNode> =
        when {
            this.expectKeyword(KeywordKind.Implements) -> {
                this.consume()
                if (this.expectSymbol(SymbolKind.Ampersand)) {
                    this.consume()
                }

                mutableListOf<NamedTypeNode>().also { interfaceList ->
                    while (true) {
                        interfaceList += this.tryToTypeCondition()
                        when {
                            this.expectSymbol(SymbolKind.Ampersand) -> this.consume()
                            else -> break
                        }
                    }
                }
            }
            else -> listOf()
        }

    private fun tryToFieldDefinitionList(): List<FieldDefinitionNode> =
        when {
            this.expectSymbol(SymbolKind.LeftBrace) -> {
                this.consume()

                generateSequence { this.tryToFieldDefinition() }.toList().also {
                    when {
                        it.isEmpty() -> this.unexpectedToken()
                        this.expectSymbol(SymbolKind.RightBrace) -> this.consume()
                        else -> this.unexpectedToken()
                    }
                }
            }
            else -> this.unexpectedToken()
        }

    private fun tryToFieldDefinition(): FieldDefinitionNode? =
        this.tryToStringValue().let { description ->
            val name = this.tryToName() ?: when (description) {
                null -> return null
                else -> this.unexpectedToken()
            }
            when {
                this.expectSymbol(SymbolKind.Colon) -> this.consume()
                else -> this.unexpectedToken()
            }
            val argumentList = this.tryToInputValueDefinitionList()
            val type = this.tryToType() ?: this.unexpectedToken()
            val directiveList = this.tryToDirectiveList()

            FieldDefinitionNode(description, name, argumentList, type, directiveList)
        }

    private fun tryToTypeSystemExtension(): DefinitionNode? =
        null

    private fun unexpectedToken(): Nothing =
        throw UnexpectedToken(this.currentToken)

    private fun tryToOperationDefinition(): ExecutableDefinitionNode? =
        when {
            this.expectSymbol(SymbolKind.LeftBrace) -> OperationTypeNode.Query to { }
            this.expectKeyword(KeywordKind.Query) -> OperationTypeNode.Query to { this.consume() }
            this.expectKeyword(KeywordKind.Mutation) -> OperationTypeNode.Mutation to { this.consume() }
            this.expectKeyword(KeywordKind.Subscription) -> OperationTypeNode.Subscription to { this.consume() }
            else -> null
        }?.let { (kind, consumeKeyword) ->
            consumeKeyword()
            val name = this.tryToName()
            val variableList = this.tryToVariableDefinitionList()
            val directiveList = this.tryToDirectiveList()
            val selectionSet = this.tryToSelectionSet() ?: this.unexpectedToken()

            OperationDefinitionNode(kind, name, variableList, directiveList, selectionSet)
        }

    private fun tryToFragmentDefinition(): ExecutableDefinitionNode? =
        when {
            this.expectKeyword(KeywordKind.Fragment) -> {
                this.consume()
                val name = this.tryToName() ?: this.unexpectedToken()
                val variableList = this.tryToVariableDefinitionList()
                when {
                    this.expectKeyword(KeywordKind.On) -> this.consume()
                    else -> this.unexpectedToken()
                }
                val typeCondition = this.tryToTypeCondition()
                val directiveList = this.tryToDirectiveList()
                val selectSet = this.tryToSelectionSet() ?: this.unexpectedToken()

                FragmentDefinitionNode(name, variableList, typeCondition, directiveList, selectSet)
            }
            else -> null
        }

    private fun tryToName(): NameNode? =
        when (val currentToken = this.currentToken) {
            is IdentifierToken -> {
                this.consume()

                NameNode(currentToken.value)
            }
            else -> null
        }

    private fun tryToVariableDefinitionList(): List<VariableDefinitionNode> =
        when {
            this.expectSymbol(SymbolKind.LeftParens) -> {
                this.consume()

                generateSequence { this.tryToVariableDefinition() }.toList().also {
                    when {
                        it.isEmpty() -> this.unexpectedToken()
                        this.expectSymbol(SymbolKind.RightParens) -> this.consume()
                        else -> this.unexpectedToken()
                    }
                }
            }
            else -> listOf()
        }

    private fun tryToVariableDefinition(): VariableDefinitionNode? =
        this.tryToVariable()?.let { variable ->
            val type = when {
                this.expectSymbol(SymbolKind.Colon) -> {
                    this.consume()

                    this.tryToType() ?: this.unexpectedToken()
                }
                else -> this.unexpectedToken()
            }
            val defaultValue = when {
                this.expectSymbol(SymbolKind.Equal) -> {
                    this.consume()

                    this.tryToValue() ?: this.unexpectedToken()
                }
                else -> null
            }
            val directiveList = this.tryToDirectiveList()

            VariableDefinitionNode(variable, type, defaultValue, directiveList)
        }

    private fun tryToType(): TypeNode? =
        this.tryToNamedType()
            ?: this.tryToListType()

    private fun tryToNamedType(): TypeNode? =
        this.tryToName()?.let { name ->
            when {
                this.expectSymbol(SymbolKind.Exclamation) -> {
                    this.consume()

                    NonNullNamedTypeNode(name)
                }
                else -> NamedTypeNode(name)
            }
        }

    private fun tryToListType(): TypeNode? =
        when {
            this.expectSymbol(SymbolKind.LeftBracket) -> {
                this.consume()
                val type = this.tryToType() ?: this.unexpectedToken()
                when {
                    this.expectSymbol(SymbolKind.RightBracket) -> {
                        this.consume()

                        when {
                            this.expectSymbol(SymbolKind.Exclamation) -> {
                                this.consume()

                                NonNullListTypeNode(type)
                            }
                            else -> ListTypeNode(type)
                        }
                    }
                    else -> this.unexpectedToken()
                }
            }
            else -> null
        }

    private fun tryToDirectiveList(): List<DirectiveNode> =
        generateSequence { this.tryToDirective() }.toList()

    private fun tryToDirective(): DirectiveNode? =
        when {
            this.expectSymbol(SymbolKind.At) -> {
                this.consume()
                val name = this.tryToName() ?: this.unexpectedToken()
                val argumentList = this.tryToArgumentList()

                DirectiveNode(name, argumentList)
            }
            else -> null
        }

    private fun tryToArgumentList(): List<ArgumentNode> =
        when {
            this.expectSymbol(SymbolKind.LeftParens) -> {
                this.consume()

                generateSequence { this.tryToArgument() }.toList().also {
                    when {
                        it.isEmpty() -> this.unexpectedToken()
                        this.expectSymbol(SymbolKind.RightParens) -> this.consume()
                        else -> this.unexpectedToken()
                    }
                }
            }
            else -> mutableListOf()
        }

    private fun tryToArgument(): ArgumentNode? =
        this.tryToName()?.let { name ->
            when {
                this.expectSymbol(SymbolKind.Colon) -> this.consume()
                else -> this.unexpectedToken()
            }
            val value = this.tryToValue() ?: this.unexpectedToken()

            ArgumentNode(name, value)
        }

    private fun tryToVariable(): VariableNode? =
        when {
            this.expectSymbol(SymbolKind.Dollar) -> {
                this.consume()
                this.tryToName()?.let(::VariableNode)
            }
            else -> null
        }

    private fun tryToValue(): ValueNode? =
        this.tryToIntValue()
            ?: this.tryToFloatValue()
            ?: this.tryToStringValue()
            ?: this.tryToBooleanValue()
            ?: this.tryToNullValue()
            ?: this.tryToEnumValue()
            ?: this.tryToListValue()
            ?: this.tryToObjectValue()

    private fun tryToIntValue(): IntValueNode? =
        when (val currentToken = this.currentToken) {
            is IntegerLiteralToken -> {
                this.consume()
                IntValueNode(currentToken.value)
            }
            else -> null
        }

    private fun tryToFloatValue(): FloatValueNode? =
        when (val currentToken = this.currentToken) {
            is FloatLiteralToken -> {
                this.consume()

                FloatValueNode(currentToken.value)
            }
            else -> null
        }

    private fun tryToStringValue(): StringValueNode? =
        when (val currentToken = this.currentToken) {
            is StringLiteralToken -> {
                this.consume()

                StringValueNode(currentToken.value)
            }
            else -> null
        }

    private fun tryToBooleanValue(): BooleanValueNode? =
        when (val currentToken = this.currentToken) {
            is BooleanLiteralToken -> {
                this.consume()

                BooleanValueNode(currentToken.value)
            }
            else -> null
        }

    private fun tryToNullValue(): NullValueNode? =
        when (this.currentToken) {
            is NullLiteralToken -> {
                this.consume()

                NullValueNode
            }
            else -> null
        }

    private fun tryToEnumValue(): EnumValueNode? =
        when (val currentToken = this.currentToken) {
            is IdentifierToken -> {
                this.consume()

                EnumValueNode(currentToken.value)
            }
            else -> null
        }

    private fun tryToListValue(): ListValueNode? =
        when {
            this.expectSymbol(SymbolKind.LeftBracket) -> {
                this.consume()
                val valueList = generateSequence { this.tryToValue() }.toList().also {
                    when {
                        this.expectSymbol(SymbolKind.RightBracket) -> this.consume()
                        else -> this.unexpectedToken()
                    }
                }

                ListValueNode(valueList)
            }
            else -> null
        }

    private fun tryToObjectValue(): ObjectValueNode? =
        when {
            this.expectSymbol(SymbolKind.LeftBrace) -> {
                this.consume()
                val fieldList = generateSequence { this.tryToObjectField() }.toList().also {
                    when {
                        this.expectSymbol(SymbolKind.RightBrace) -> this.consume()
                        else -> this.unexpectedToken()
                    }
                }
                when {
                    this.expectSymbol(SymbolKind.RightBrace) -> this.consume()
                    else -> this.unexpectedToken()
                }

                ObjectValueNode(fieldList)
            }
            else -> null
        }

    private fun tryToObjectField(): ObjectFieldNode? =
        this.tryToName()?.let { name ->
            val value = when {
                this.expectSymbol(SymbolKind.Colon) -> this.tryToValue() ?: unexpectedToken()
                else -> this.unexpectedToken()
            }

            ObjectFieldNode(name, value)
        }

    private fun tryToSelectionSet(): SelectionSetNode? =
        when {
            this.expectSymbol(SymbolKind.LeftBrace) -> {
                this.consume()
                val selectionList = generateSequence { this.tryToSelection() }.toList()
                when {
                    selectionList.isEmpty() -> this.unexpectedToken()
                    this.expectSymbol(SymbolKind.RightBrace) -> this.consume()
                    else -> this.unexpectedToken()
                }

                SelectionSetNode(selectionList)
            }
            else -> null
        }

    private fun tryToSelection(): SelectionNode? =
        this.tryToField()
            ?: this.tryToFragment()

    private fun tryToField(): FieldNode? =
        this.tryToName()?.let { nameOrAlias ->
            val (alias, name) = when {
                this.expectSymbol(SymbolKind.Colon) -> {
                    this.consume()
                    val name = this.tryToName() ?: this.unexpectedToken()

                    nameOrAlias to name
                }
                else -> null to nameOrAlias
            }
            val argumentList = this.tryToArgumentList()
            val directiveList = this.tryToDirectiveList()
            val selectionSet = this.tryToSelectionSet()

            FieldNode(alias, name, argumentList, directiveList, selectionSet)
        }

    private fun tryToFragment(): SelectionNode? =
        when {
            this.expectSymbol(SymbolKind.DotDotDot) -> {
                this.consume()
                when (val currentToken = this.currentToken) {
                    is IdentifierToken -> {
                        this.consume()
                        val directiveList = this.tryToDirectiveList()

                        FragmentSpreadNode(NameNode(currentToken.value), directiveList)
                    }
                    is KeywordToken -> {
                        this.consume()
                        when {
                            this.expectKeyword(KeywordKind.On) -> this.consume()
                            else -> this.unexpectedToken()
                        }
                        val typeCondition = this.tryToTypeCondition()
                        val directiveList = this.tryToDirectiveList()
                        val selectionSet = this.tryToSelectionSet() ?: this.unexpectedToken()

                        InlineFragmentNode(typeCondition, directiveList, selectionSet)
                    }
                    else -> this.unexpectedToken()
                }
            }
            else -> null
        }

    private fun tryToTypeCondition(): NamedTypeNode =
        this.tryToNamedType()?.let { name ->
            when (name) {
                is NamedTypeNode -> name
                else -> this.unexpectedToken()
            }
        } ?: this.unexpectedToken()

    private val currentToken: Token?
        get () =
            when {
                this.hasMoreDefinition() -> this.tokenList[this.position]
                else -> null
            }

    private fun expectSymbol(kind: SymbolKind): Boolean =
        this.expect(SymbolToken(kind))

    private fun expectKeyword(kind: KeywordKind): Boolean =
        this.expect(KeywordToken(kind))

    private fun expect(token: Token): Boolean =
        this.currentToken == token

    private fun consume(): Unit {
        this.position += 1
    }
}
