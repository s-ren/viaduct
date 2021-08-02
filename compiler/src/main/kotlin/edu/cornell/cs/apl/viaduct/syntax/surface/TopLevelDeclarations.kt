package edu.cornell.cs.apl.viaduct.syntax.surface

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.braced
import edu.cornell.cs.apl.prettyprinting.bracketed
import edu.cornell.cs.apl.prettyprinting.nested
import edu.cornell.cs.apl.prettyprinting.plus
import edu.cornell.cs.apl.prettyprinting.times
import edu.cornell.cs.apl.prettyprinting.tupled
import edu.cornell.cs.apl.viaduct.syntax.Arguments
import edu.cornell.cs.apl.viaduct.syntax.ClassNameNode
import edu.cornell.cs.apl.viaduct.syntax.FunctionNameNode
import edu.cornell.cs.apl.viaduct.syntax.HostNode
import edu.cornell.cs.apl.viaduct.syntax.LabelNode
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariableNode
import edu.cornell.cs.apl.viaduct.syntax.ParameterDirection
import edu.cornell.cs.apl.viaduct.syntax.PrincipalNode
import edu.cornell.cs.apl.viaduct.syntax.ProtocolNode
import edu.cornell.cs.apl.viaduct.syntax.SourceLocation
import edu.cornell.cs.apl.viaduct.syntax.ValueTypeNode
import edu.cornell.cs.apl.viaduct.syntax.datatypes.ImmutableCell

/** A declaration at the top level of a file. */
sealed class TopLevelDeclarationNode : Node()

/**
 * Declaration of a host participant.
 *
 * @param name Host name.
 */
class HostDeclarationNode(
    val name: HostNode,
    override val sourceLocation: SourceLocation
) : TopLevelDeclarationNode() {
    override val asDocument: Document
        get() = keyword("host") * name
}

/**
 * Declaration of a principal participant.
 *
 * @param name Principal name.
 */
class PrincipalDeclarationNode(
    val name: PrincipalNode,
    override val sourceLocation: SourceLocation
) : TopLevelDeclarationNode() {
    override val asDocument: Document
        get() = keyword("principal") * name
}

/**
 * Declaration of a delegations.
 * @param node1 The label that acts for the other label.
 * @param node2 The other label.
 * @param is_mutual True iff the delegation is on both directions.
 */
class DelegationDeclarationNode(
    val node1: LabelNode,
    val node2: LabelNode,
    val is_mutual: Boolean,
    override val sourceLocation: SourceLocation
) : TopLevelDeclarationNode() {
    override val asDocument: Document
        get() {
            val flag = if (is_mutual) "<" else ""
            return keyword("delegation:") * listOf(node1).braced() *
                flag * "=>" * listOf(node2).braced()
        }
}

/**
 * A set of principals as a top level declaration.
 */
class PrincipalDeclarationSetNode(
    val principals: List<PrincipalDeclarationNode>,
    override val sourceLocation: SourceLocation
) : TopLevelDeclarationNode() {
    override val asDocument: Document
        get() = keyword("principals:") *
            principals.fold(
                Document(),
                { acc: Document, principal: PrincipalDeclarationNode ->
                    acc * principal
                });
}

/**
 * A set of hosts as a top level declaration.
 */
class HostDeclarationSetNode(
    val hosts: List<HostDeclarationNode>,
    override val sourceLocation: SourceLocation
) : TopLevelDeclarationNode() {
    override val asDocument: Document
        get() = keyword("hosts:") *
            hosts.fold(
                Document(),
                { acc: Document, host: HostDeclarationNode ->
                    acc * host
                });
}

/**
 * A set of delegations as top level declaration.
 */
class DelegationDeclarationSetNode(
    val delegations: List<DelegationDeclarationNode>,
    override val sourceLocation: SourceLocation
) : TopLevelDeclarationNode() {
    override val asDocument: Document
        get() = keyword("delegations:") *
            delegations.fold(
                Document(),
                { acc: Document, delegation: DelegationDeclarationNode ->
                    acc * delegation
                });
}


/**
 * A process declaration associating a protocol with the code that process should run.
 *
 * @param protocol Name of the process.
 * @param body Code that will be executed by this process.
 */
class ProcessDeclarationNode(
    val protocol: ProtocolNode,
    val body: BlockNode,
    override val sourceLocation: SourceLocation
) : TopLevelDeclarationNode() {
    override val asDocument: Document
        get() = keyword("process") * protocol * body
}

/**
 * A parameter to a function declaration.
 */
class ParameterNode(
    val name: ObjectVariableNode,
    val parameterDirection: ParameterDirection,
    val className: ClassNameNode,
    val typeArguments: Arguments<ValueTypeNode>,
    // TODO: allow leaving out some of the labels (right now it's all or nothing)
    val labelArguments: Arguments<LabelNode>?,
    val protocol: ProtocolNode?,
    override val sourceLocation: SourceLocation
) : Node() {
    override val asDocument: Document
        get() {
            val protocolDoc = protocol?.let {
                Document("@") + it.value.asDocument
            } ?: Document("")

            return when (className.value) {
                ImmutableCell -> {
                    val label = labelArguments?.braced() ?: Document()
                    name + Document(":") + parameterDirection * typeArguments[0] + label + protocolDoc
                }

                else -> {
                    val types = typeArguments.bracketed().nested()
                    // TODO: labels should have braces
                    //   val labels = labelArguments?.braced()?.nested() ?: Document()
                    val labels = labelArguments?.braced() ?: Document()
                    name * ":" + parameterDirection * className + types + labels + protocolDoc
                }
            }
        }
}

/**
 * A declaration of a function that can be called by a process.
 *
 * @param parameters A list of formal parameters.
 * @param body The function body.
 */
class FunctionDeclarationNode(
    val name: FunctionNameNode,
    val pcLabel: LabelNode?,
    val parameters: Arguments<ParameterNode>,
    val body: BlockNode,
    override val sourceLocation: SourceLocation
) : TopLevelDeclarationNode() {
    override val asDocument: Document
        get() =
            keyword("fun") * name +
                (pcLabel?.let { listOf(it).braced() } ?: Document("")) +
                parameters.tupled() * body
}
