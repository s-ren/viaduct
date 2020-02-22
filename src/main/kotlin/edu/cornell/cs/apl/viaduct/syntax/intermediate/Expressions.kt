package edu.cornell.cs.apl.viaduct.syntax.intermediate

import edu.cornell.cs.apl.viaduct.syntax.Arguments
import edu.cornell.cs.apl.viaduct.syntax.LabelNode
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariableNode
import edu.cornell.cs.apl.viaduct.syntax.Operator
import edu.cornell.cs.apl.viaduct.syntax.SourceLocation
import edu.cornell.cs.apl.viaduct.syntax.Temporary
import edu.cornell.cs.apl.viaduct.syntax.datatypes.QueryName
import edu.cornell.cs.apl.viaduct.syntax.values.Value

/** A computation that produces a result. */
sealed class ExpressionNode : Node() {
    abstract override fun toSurfaceNode(): edu.cornell.cs.apl.viaduct.syntax.surface.ExpressionNode
}

/** An expression that requires no computation to reduce to a value. */
sealed class AtomicExpressionNode : ExpressionNode() {
    abstract override fun toSurfaceNode(): edu.cornell.cs.apl.viaduct.syntax.surface.AtomicExpressionNode
}

/** A literal constant. */
class LiteralNode(val value: Value, override val sourceLocation: SourceLocation) :
    AtomicExpressionNode() {
    override fun toSurfaceNode(): edu.cornell.cs.apl.viaduct.syntax.surface.LiteralNode =
        edu.cornell.cs.apl.viaduct.syntax.surface.LiteralNode(value, sourceLocation)
}

/** Reading the value stored in a temporary. */
class ReadNode(val temporary: Temporary, override val sourceLocation: SourceLocation) :
    AtomicExpressionNode() {
    override fun toSurfaceNode(): edu.cornell.cs.apl.viaduct.syntax.surface.ReadNode =
        edu.cornell.cs.apl.viaduct.syntax.surface.ReadNode(temporary, sourceLocation)
}

/** An n-ary operator applied to n arguments. */
class OperatorApplicationNode(
    val operator: Operator,
    val arguments: Arguments<ExpressionNode>,
    override val sourceLocation: SourceLocation
) : ExpressionNode() {
    override fun toSurfaceNode(): edu.cornell.cs.apl.viaduct.syntax.surface.OperatorApplicationNode =
        edu.cornell.cs.apl.viaduct.syntax.surface.OperatorApplicationNode(
            operator,
            Arguments(arguments.map { it.toSurfaceNode() }, arguments.sourceLocation),
            sourceLocation
        )
}

/** A query method applied to an object. */
class QueryNode(
    val variable: ObjectVariableNode,
    val query: QueryName,
    val arguments: Arguments<ExpressionNode>,
    override val sourceLocation: SourceLocation
) : ExpressionNode() {
    override fun toSurfaceNode(): edu.cornell.cs.apl.viaduct.syntax.surface.QueryNode =
        edu.cornell.cs.apl.viaduct.syntax.surface.QueryNode(
            variable,
            query,
            Arguments(arguments.map { it.toSurfaceNode() }, arguments.sourceLocation),
            sourceLocation
        )
}

/** Reducing the confidentiality or increasing the integrity of the result of an expression. */
sealed class DowngradeNode : ExpressionNode() {
    /** Expression whose label is being downgraded. */
    abstract val expression: AtomicExpressionNode

    /** The label [expression] must have before the downgrade. */
    abstract val fromLabel: LabelNode?

    /** The label after the downgrade. */
    abstract val toLabel: LabelNode
}

/** Revealing the the result of an expression (reducing confidentiality). */
class DeclassificationNode(
    override val expression: AtomicExpressionNode,
    override val fromLabel: LabelNode?,
    override val toLabel: LabelNode,
    override val sourceLocation: SourceLocation
) : DowngradeNode() {
    override fun toSurfaceNode(): edu.cornell.cs.apl.viaduct.syntax.surface.DeclassificationNode =
        edu.cornell.cs.apl.viaduct.syntax.surface.DeclassificationNode(
            expression.toSurfaceNode(),
            fromLabel,
            toLabel,
            sourceLocation
        )
}

/** Trusting the result of an expression (increasing integrity). */
class EndorsementNode(
    override val expression: AtomicExpressionNode,
    override val fromLabel: LabelNode?,
    override val toLabel: LabelNode,
    override val sourceLocation: SourceLocation
) : DowngradeNode() {
    override fun toSurfaceNode(): edu.cornell.cs.apl.viaduct.syntax.surface.EndorsementNode =
        edu.cornell.cs.apl.viaduct.syntax.surface.EndorsementNode(
            expression.toSurfaceNode(),
            fromLabel,
            toLabel,
            sourceLocation
        )
}
