package ast

data class NameNode(val value: String)

data class DocumentNode(val definitionList: List<DefinitionNode>)

sealed class DefinitionNode

sealed class ExecutableDefinitionNode: DefinitionNode()

sealed class TypeSystemDefinitionNode: DefinitionNode()

sealed class TypeSystemExtensionNode: DefinitionNode()

data class OperationDefinitionNode(
    val kind: OperationTypeNode,
    val name: NameNode?,
    val variableList: List<VariableDefinitionNode>,
    val directiveList: List<DirectiveNode>,
    val selectionSet: SelectionSetNode,
): ExecutableDefinitionNode()

enum class OperationTypeNode {
    Query,
    Mutation,
    Subscription,
}

data class FragmentDefinitionNode(
    val name: NameNode,
    val variableList: List<VariableDefinitionNode>,
    val typeCondition: NamedTypeNode,
    val directiveList: List<DirectiveNode>,
    val selectSet: SelectionSetNode,
): ExecutableDefinitionNode()

data class VariableDefinitionNode(
    val variable: VariableNode,
    val type: TypeNode,
    val defaultValue: ValueNode?,
    val directiveList: List<DirectiveNode>,
)

data class VariableNode(val name: NameNode)

data class SelectionSetNode(val selectionList: List<SelectionNode>)

sealed class SelectionNode

data class FieldNode(
    val alias: NameNode?,
    val name: NameNode,
    val argumentList: List<ArgumentNode>,
    val directiveList: List<DirectiveNode>,
    val selectionSet: SelectionSetNode?,
): SelectionNode()

data class ArgumentNode(val name: NameNode, val value: ValueNode)

data class FragmentSpreadNode(val name: NameNode, val directiveList: List<DirectiveNode>): SelectionNode()

data class InlineFragmentNode(
    val typeCondition: NamedTypeNode?,
    val directiveList: List<DirectiveNode>,
    val selectionSet: SelectionSetNode,
): SelectionNode()

sealed class ValueNode

data class IntValueNode(val value: Int): ValueNode()

data class FloatValueNode(val value: Double): ValueNode()

data class StringValueNode(val value: String): ValueNode()

data class BooleanValueNode(val value: Boolean): ValueNode()

object NullValueNode: ValueNode()

data class EnumValueNode(val value: String): ValueNode()

data class ListValueNode(val valueList: List<ValueNode>): ValueNode()

data class ObjectValueNode(val fieldList: List<ObjectFieldNode>): ValueNode()

data class ObjectFieldNode(val name: NameNode, val value: ValueNode)

data class DirectiveNode(val name: NameNode, val argumentList: List<ArgumentNode>)

sealed class TypeNode

data class NamedTypeNode(val name: NameNode): TypeNode()

data class ListTypeNode(val type: TypeNode): TypeNode()

sealed class NonNullTypeNode: TypeNode()

data class NonNullNamedTypeNode(val name: NameNode): NonNullTypeNode()

data class NonNullListTypeNode(val type: TypeNode): NonNullTypeNode()

data class SchemaDefinitionNode(
    val description: StringValueNode?,
    val directiveList: List<DirectiveNode>,
    val operationTypeList: List<OperationTypeDefinitionNode>,
): TypeSystemDefinitionNode()

data class OperationTypeDefinitionNode(val operation: OperationTypeNode, val type: NamedTypeNode)

sealed class TypeDefinitionNode: TypeSystemDefinitionNode()

data class ScalarTypeDefinitionNode(
    val description: StringValueNode?,
    val name: NameNode,
    val directiveList: List<DirectiveNode>,
): TypeDefinitionNode()

data class ObjectTypeDefinitionNode(
    val description: StringValueNode?,
    val name: NameNode,
    val interfaceList: List<NamedTypeNode>,
    val directiveList: List<DirectiveNode>,
    val fieldList: List<FieldDefinitionNode>,
): TypeDefinitionNode()

data class FieldDefinitionNode(
    val description: StringValueNode?,
    val name: NameNode,
    val argumentList: List<InputValueDefinitionNode>,
    val type: TypeNode,
    val directiveList: List<DirectiveNode>,
)

data class InputValueDefinitionNode(
    val description: StringValueNode?,
    val name: NameNode,
    val type: TypeNode,
    val defaultValue: ValueNode?,
    val directiveList: List<DirectiveNode>,
)

data class InterfaceTypeDefinitionNode(
    val description: StringValueNode?,
    val name: NameNode,
    val interfaceList: List<NamedTypeNode>,
    val directiveList: List<DirectiveNode>,
    val fieldList: List<FieldDefinitionNode>,
): TypeDefinitionNode()

data class UnionTypeDefinitionNode(
    val description: StringValueNode?,
    val name: NameNode,
    val directiveList: List<DirectiveNode>,
    val typeList: List<NamedTypeNode>,
): TypeDefinitionNode()

data class EnumTypeDefinitionNode(
    val description: StringValueNode?,
    val name: NameNode,
    val directiveList: List<DirectiveNode>,
    val valueList: List<EnumValueDefinitionNode>,
): TypeDefinitionNode()

data class EnumValueDefinitionNode(
    val description: StringValueNode?,
    val name: NameNode,
    val directiveList: List<DirectiveNode>,
)

data class InputObjectTypeDefinitionNode(
    val description: StringValueNode?,
    val name: NameNode,
    val directiveList: List<DirectiveNode>,
    val fieldList: List<InputValueDefinitionNode>,
): TypeDefinitionNode()

data class DirectiveDefinitionNode(
    val description: StringValueNode?,
    val name: NameNode,
    val argumentList: List<InputValueDefinitionNode>,
    val repeatable: Boolean,
    val locationList: List<NameNode>,
): TypeSystemDefinitionNode()

data class SchemaExtensionNode(
    val description: StringValueNode?,
    val operationTypeList: List<OperationTypeDefinitionNode>,
): TypeSystemExtensionNode()

sealed class TypeExtensionNode: TypeSystemExtensionNode()

data class ScalarTypeExtensionNode(val name: NameNode, val directiveList: List<DirectiveNode>): TypeExtensionNode()

data class ObjectTypeExtensionNode(
    val name: NameNode,
    val interfaceList: List<NamedTypeNode>,
    val directiveList: List<DirectiveNode>,
    val fieldList: List<FieldDefinitionNode>,
): TypeExtensionNode()

data class InterfaceTypeExtensionNode(
    val name: NameNode,
    val interfaceList: List<NamedTypeNode>,
    val directiveList: List<DirectiveNode>,
    val fieldList: List<FieldDefinitionNode>,
): TypeExtensionNode()

data class UnionTypeExtensionNode(
    val name: NameNode,
    val directiveList: List<DirectiveNode>,
    val typeList: List<NamedTypeNode>,
): TypeExtensionNode()

data class EnumTypeExtensionNode(
    val name: NameNode,
    val directiveList: List<DirectiveNode>,
    val valueList: List<EnumValueDefinitionNode>,
): TypeExtensionNode()

data class InputObjectTypeExtensionNode(
    val name: NameNode,
    val directiveList: List<DirectiveNode>,
    val fieldList: List<FieldDefinitionNode>,
): TypeExtensionNode()
