package edu.cornell.cs.apl.viaduct.passes

import edu.cornell.cs.apl.viaduct.errors.JumpOutsideLoopScopeError
import edu.cornell.cs.apl.viaduct.syntax.Arguments
import edu.cornell.cs.apl.viaduct.syntax.JumpLabel
import edu.cornell.cs.apl.viaduct.syntax.JumpLabelNode
import edu.cornell.cs.apl.viaduct.syntax.NameMap
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariable
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariableNode
import edu.cornell.cs.apl.viaduct.syntax.Temporary
import edu.cornell.cs.apl.viaduct.syntax.TemporaryNode
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Get
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AssertionNode as IAssertionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AtomicExpressionNode as IAtomicExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode as IBlockNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BreakNode as IBreakNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode as IDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclassificationNode as IDeclassificationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.EndorsementNode as IEndorsementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionNode as IExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.HostDeclarationNode as IHostDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.IfNode as IIfNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InfiniteLoopNode as IInfiniteLoopNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InputNode as IInputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode as ILetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LiteralNode as ILiteralNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.Node
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OperatorApplicationNode as IOperatorApplicationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutputNode as IOutputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProcessDeclarationNode as IProcessDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode as IProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.QueryNode as IQueryNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode as IReadNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReceiveNode as IReceiveNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SendNode as ISendNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.StatementNode as IStatementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.TopLevelDeclarationNode as ITopLevelDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode as IUpdateNode
import edu.cornell.cs.apl.viaduct.syntax.surface.AssertionNode as SAssertionNode
import edu.cornell.cs.apl.viaduct.syntax.surface.BlockNode as SBlockNode
import edu.cornell.cs.apl.viaduct.syntax.surface.BreakNode as SBreakNode
import edu.cornell.cs.apl.viaduct.syntax.surface.DeclarationNode as SDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.surface.DeclassificationNode as SDeclassificationNode
import edu.cornell.cs.apl.viaduct.syntax.surface.EndorsementNode as SEndorsementNode
import edu.cornell.cs.apl.viaduct.syntax.surface.ExpressionNode as SExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.surface.ForLoopNode as SForLoopNode
import edu.cornell.cs.apl.viaduct.syntax.surface.HostDeclarationNode as SHostDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.surface.IfNode as SIfNode
import edu.cornell.cs.apl.viaduct.syntax.surface.InfiniteLoopNode as SInfiniteLoopNode
import edu.cornell.cs.apl.viaduct.syntax.surface.InputNode as SInputNode
import edu.cornell.cs.apl.viaduct.syntax.surface.LetNode as SLetNode
import edu.cornell.cs.apl.viaduct.syntax.surface.LiteralNode as SLiteralNode
import edu.cornell.cs.apl.viaduct.syntax.surface.OperatorApplicationNode as SOperatorApplicationNode
import edu.cornell.cs.apl.viaduct.syntax.surface.OutputNode as SOutputNode
import edu.cornell.cs.apl.viaduct.syntax.surface.ProcessDeclarationNode as SProcessDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.surface.ProgramNode as SProgramNode
import edu.cornell.cs.apl.viaduct.syntax.surface.QueryNode as SQueryNode
import edu.cornell.cs.apl.viaduct.syntax.surface.ReadNode as SReadNode
import edu.cornell.cs.apl.viaduct.syntax.surface.ReceiveNode as SReceiveNode
import edu.cornell.cs.apl.viaduct.syntax.surface.SendNode as SSendNode
import edu.cornell.cs.apl.viaduct.syntax.surface.SkipNode as SSkipNode
import edu.cornell.cs.apl.viaduct.syntax.surface.StatementNode as SStatementNode
import edu.cornell.cs.apl.viaduct.syntax.surface.UpdateNode as SUpdateNode
import edu.cornell.cs.apl.viaduct.syntax.surface.WhileLoopNode as SWhileLoopNode
import edu.cornell.cs.apl.viaduct.util.FreshNameGenerator

/**
 * Elaborates this surface program into a program in the intermediate representation.
 *
 * See [Node] for the list of transformations performed.
 */
fun SProgramNode.elaborated(): IProgramNode {
    val declarations = mutableListOf<ITopLevelDeclarationNode>()
    this.declarations.forEach {
        val declaration = when (it) {
            is SHostDeclarationNode -> {
                IHostDeclarationNode(
                    it.name,
                    it.authority,
                    it.sourceLocation
                )
            }

            is SProcessDeclarationNode -> {
                IProcessDeclarationNode(
                    it.protocol,
                    StatementElaborator().elaborate(it.body),
                    it.sourceLocation
                )
            }
        }
        declarations.add(declaration)
    }
    return IProgramNode(declarations, this.sourceLocation)
}

private class StatementElaborator(
    private val nameGenerator: FreshNameGenerator,

    // Maps old [Name]s to their new [Name]s.
    private var temporaryRenames: NameMap<Temporary, Temporary>,
    private var objectRenames: NameMap<ObjectVariable, ObjectVariable>,
    private val jumpLabelRenames: NameMap<JumpLabel, JumpLabel>,

    /** The label of the innermost loop surrounding the current context. */
    private val surroundingLoop: JumpLabel?
) {
    private companion object {
        const val TMP_NAME = "tmp"
        const val LOOP_NAME = "loop"
    }

    constructor() :
        this(FreshNameGenerator(), NameMap(), NameMap(), NameMap(), null)

    private fun copy(
        jumpLabelRenames: NameMap<JumpLabel, JumpLabel> = this.jumpLabelRenames,
        surroundingLoop: JumpLabel? = this.surroundingLoop
    ): StatementElaborator =
        StatementElaborator(
            nameGenerator,
            NameMap(), // Temporaries are local and reset at each block.
            objectRenames,
            jumpLabelRenames,
            surroundingLoop
        )

    /** Generates a new temporary whose name is based on [baseName]. */
    private fun freshTemporary(baseName: String? = null): Temporary =
        Temporary(nameGenerator.getFreshName(baseName ?: TMP_NAME))

    /**
     * Runs [producer] to get a statement, and prepends bindings generated by [producer]
     * before that statement.
     */
    private fun withBindings(producer: (MutableList<in IStatementNode>) -> IStatementNode): List<IStatementNode> {
        val bindings = mutableListOf<IStatementNode>()
        bindings.add(producer(bindings))
        return bindings
    }

    /**
     * Converts this surface expression into an intermediate expression in A-normal form.
     * Intermediate result are bound using let statements, which are appended to [bindings].
     */
    private fun SExpressionNode.toAnf(bindings: MutableList<in IStatementNode>): IExpressionNode {
        return when (this) {
            is SLiteralNode ->
                ILiteralNode(value, sourceLocation)

            is SReadNode ->
                IReadNode(TemporaryNode(temporaryRenames[temporary], sourceLocation))

            is SOperatorApplicationNode ->
                IOperatorApplicationNode(
                    operator,
                    Arguments(
                        arguments.map { it.toAnf(bindings).toAtomic(bindings) },
                        arguments.sourceLocation
                    ),
                    sourceLocation
                )

            // this is extremely hacky---since the parser can't recognize temporary reads,
            // we must interpret some query nodes as temporary reads
            is SQueryNode -> {
                val oldTemporary = TemporaryNode(Temporary(variable.value.name), variable.sourceLocation)

                return when {
                    objectRenames.contains(variable) -> {
                        IQueryNode(
                            ObjectVariableNode(objectRenames[variable], variable.sourceLocation),
                            query,
                            Arguments(
                                arguments.map { it.toAnf(bindings).toAtomic(bindings) },
                                arguments.sourceLocation
                            ),
                            sourceLocation
                        )
                    }

                    temporaryRenames.contains(oldTemporary) &&
                        arguments.size == 0
                        && query.value == Get ->
                    {
                        IReadNode(TemporaryNode(temporaryRenames[oldTemporary], sourceLocation))
                    }

                    else -> throw Exception("unknown query")
                }
            }

            is SDeclassificationNode ->
                IDeclassificationNode(
                    expression.toAnf(bindings).toAtomic(bindings),
                    fromLabel,
                    toLabel,
                    sourceLocation
                )

            is SEndorsementNode ->
                IEndorsementNode(
                    expression.toAnf(bindings).toAtomic(bindings),
                    fromLabel,
                    toLabel,
                    sourceLocation
                )

            is SInputNode ->
                IInputNode(type, host, sourceLocation)

            is SReceiveNode -> {
                IReceiveNode(type, protocol, sourceLocation)
            }
        }
    }

    /**
     * Convert this expression to an atomic expression by introducing a new let binding
     * if necessary. This binding is appended to [bindings].
     */
    private fun IExpressionNode.toAtomic(bindings: MutableList<in ILetNode>): IAtomicExpressionNode {
        return when (this) {
            is IAtomicExpressionNode ->
                this

            else -> {
                val tmp = TemporaryNode(freshTemporary(), this.sourceLocation)
                bindings.add(ILetNode(tmp, this, this.sourceLocation))
                IReadNode(tmp)
            }
        }
    }

    /** Converts surface block statement into an intermediate block statement. */
    fun elaborate(block: SBlockNode): IBlockNode {
        val newScope = this.copy()

        // Flatten children into one big list
        val statements = mutableListOf<IStatementNode>()
        block.forEach { statements.addAll(newScope.run(it)) }

        return IBlockNode(statements, block.sourceLocation)
    }

    private fun run(stmt: SStatementNode): List<IStatementNode> {
        return when (stmt) {
            is SLetNode ->
                withBindings { bindings ->
                    // The value must be processed before the temporary is freshened
                    val newValue = stmt.value.toAnf(bindings)

                    val newName = freshTemporary(stmt.temporary.value.name)
                    temporaryRenames = temporaryRenames.put(stmt.temporary, newName)

                    ILetNode(
                        TemporaryNode(newName, stmt.temporary.sourceLocation),
                        newValue,
                        stmt.sourceLocation
                    )
                }

            is SDeclarationNode ->
                withBindings { bindings ->
                    // The arguments must be processed before the variable is freshened
                    val newArguments =
                        Arguments(
                            stmt.arguments.map { it.toAnf(bindings).toAtomic(bindings) },
                            stmt.arguments.sourceLocation
                        )

                    val newName =
                        ObjectVariable(nameGenerator.getFreshName(stmt.variable.value.name))
                    objectRenames = objectRenames.put(stmt.variable, newName)

                    IDeclarationNode(
                        ObjectVariableNode(newName, stmt.variable.sourceLocation),
                        stmt.className,
                        stmt.typeArguments,
                        stmt.labelArguments,
                        newArguments,
                        stmt.sourceLocation
                    )
                }

            is SUpdateNode ->
                withBindings { bindings ->
                    IUpdateNode(
                        ObjectVariableNode(
                            objectRenames[stmt.variable],
                            stmt.variable.sourceLocation
                        ),
                        stmt.update,
                        Arguments(
                            stmt.arguments.map { it.toAnf(bindings).toAtomic(bindings) },
                            stmt.arguments.sourceLocation
                        ),
                        stmt.sourceLocation
                    )
                }

            is SSkipNode ->
                listOf()

            is SOutputNode ->
                withBindings { bindings ->
                    IOutputNode(
                        stmt.message.toAnf(bindings).toAtomic(bindings),
                        stmt.host,
                        stmt.sourceLocation
                    )
                }

            is SSendNode ->
                withBindings { bindings ->
                    ISendNode(
                        stmt.message.toAnf(bindings).toAtomic(bindings),
                        stmt.protocol,
                        stmt.sourceLocation
                    )
                }

            is SAssertionNode ->
                withBindings { bindings ->
                    IAssertionNode(
                        stmt.condition.toAnf(bindings).toAtomic(bindings),
                        stmt.sourceLocation
                    )
                }

            is SIfNode -> {
                withBindings { bindings ->
                    IIfNode(
                        stmt.guard.toAnf(bindings).toAtomic(bindings),
                        elaborate(stmt.thenBranch),
                        elaborate(stmt.elseBranch),
                        stmt.sourceLocation
                    )
                }
            }

            is SInfiniteLoopNode -> {
                // Create a jump label if there isn't one
                val jumpLabel = stmt.jumpLabel?.value?.name ?: LOOP_NAME
                val renamedJumpLabel = JumpLabel(nameGenerator.getFreshName(jumpLabel))
                val jumpLabelLocation = stmt.jumpLabel?.sourceLocation ?: stmt.sourceLocation

                val newScope = this.copy(
                    jumpLabelRenames =
                    if (stmt.jumpLabel == null)
                        jumpLabelRenames
                    else jumpLabelRenames.put(stmt.jumpLabel, renamedJumpLabel),
                    surroundingLoop = renamedJumpLabel
                )

                listOf(
                    IInfiniteLoopNode(
                        newScope.elaborate(stmt.body),
                        JumpLabelNode(renamedJumpLabel, jumpLabelLocation),
                        stmt.sourceLocation
                    )
                )
            }

            is SBreakNode -> {
                if (surroundingLoop == null)
                    throw JumpOutsideLoopScopeError(stmt)

                val jumpLabelNode: JumpLabelNode =
                    if (stmt.jumpLabel == null)
                        JumpLabelNode(surroundingLoop, stmt.sourceLocation)
                    else
                        JumpLabelNode(
                            jumpLabelRenames[stmt.jumpLabel],
                            stmt.jumpLabel.sourceLocation
                        )

                listOf(
                    IBreakNode(jumpLabelNode, stmt.sourceLocation)
                )
            }

            is SWhileLoopNode ->
                run(elaborate(stmt))

            is SForLoopNode ->
                run(elaborate(stmt))

            is SBlockNode ->
                listOf(elaborate(stmt))
        }
    }
}

/**
 * Rewrites a while loop into a loop-until-break statement.
 *
 * More specifically,
 *
 * ```
 * while (guard) { body... }
 * ```
 *
 * gets translated to
 *
 * ```
 * loop {
 *     if (guard) {
 *         body...
 *     } else {
 *         break;
 *     }
 * }
 * ```
 */
private fun elaborate(node: SWhileLoopNode): SInfiniteLoopNode =
    SInfiniteLoopNode(
        SBlockNode(
            SIfNode(
                guard = node.guard,
                thenBranch = node.body,
                elseBranch = SBlockNode(
                    SBreakNode(jumpLabel = node.jumpLabel, sourceLocation = node.sourceLocation),
                    sourceLocation = node.sourceLocation
                ),
                sourceLocation = node.sourceLocation
            ),
            sourceLocation = node.sourceLocation
        ),
        jumpLabel = node.jumpLabel,
        sourceLocation = node.sourceLocation
    )

/**
 * Rewrites a for loop into a while loop.
 *
 * More specifically,
 *
 * ```
 * for (init; guard; update) { body... }
 * ```
 *
 * gets translated to
 *
 * ```
 * {
 *     init;
 *     while (guard) { body... update; }
 * }
 * ```
 */
private fun elaborate(node: SForLoopNode): SBlockNode =
    SBlockNode(
        node.initialize,
        SWhileLoopNode(
            guard = node.guard,
            body = SBlockNode(node.body + node.update, sourceLocation = node.sourceLocation),
            jumpLabel = node.jumpLabel,
            sourceLocation = node.sourceLocation
        ),
        sourceLocation = node.sourceLocation
    )