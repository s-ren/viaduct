package edu.cornell.cs.apl.viaduct.analysis

import edu.cornell.cs.apl.viaduct.errorskotlin.NameClashError
import edu.cornell.cs.apl.viaduct.errorskotlin.UndefinedNameError
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.JumpLabel
import edu.cornell.cs.apl.viaduct.syntax.Located
import edu.cornell.cs.apl.viaduct.syntax.Name
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariable
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.SourceLocation
import edu.cornell.cs.apl.viaduct.syntax.Temporary
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BreakNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExternalCommunicationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.HostDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InfiniteLoopNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InternalCommunicationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.Node
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProcessDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.QueryNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.TemporaryDefinition
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.attributes.Tree
import edu.cornell.cs.apl.viaduct.syntax.intermediate.attributes.attribute
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf

/**
 * Associates each use of a [Name] with its declaration.
 *
 * For example, [Temporary] variables are associated with [LetNode]s, [ObjectVariable]s with
 * [DeclarationNode]s, and [JumpLabel]s with [InfiniteLoopNode]s.
 * */
class NameAnalysis(val tree: Tree<Node, ProgramNode>) {
    /** Host declarations in scope for this node. */
    private val Node.hostDeclarations: NameMap<Host, HostDeclarationNode> by attribute {
        when (val parent = tree.parent(this)) {
            null -> {
                require(this is ProgramNode)
                declarations.filterIsInstance<HostDeclarationNode>()
                    .fold(NameMap()) { map, declaration -> map.put(declaration.name, declaration) }
            }
            else ->
                parent.hostDeclarations
        }
    }

    /** Protocol declarations in scope for this node. */
    private val Node.protocolDeclarations: NameMap<Protocol, ProcessDeclarationNode> by attribute {
        when (val parent = tree.parent(this)) {
            null -> {
                require(this is ProgramNode)
                declarations.filterIsInstance<ProcessDeclarationNode>()
                    .fold(NameMap()) { map, declaration ->
                        map.put(declaration.protocol, declaration)
                    }
            }
            else ->
                parent.protocolDeclarations
        }
    }

    /** Temporary definitions in scope for this node. */
    // TODO: change to [LetNode] once we merge temporary definitions into one.
    private val Node.temporaryDefinitions by Context<Temporary, TemporaryDefinition> {
        if (it is TemporaryDefinition) Pair(it.temporary, it) else null
    }

    /** Object declarations in scope for this node. */
    private val Node.objectDeclarations: NameMap<ObjectVariable, DeclarationNode> by Context {
        if (it is DeclarationNode) Pair(it.variable, it) else null
    }

    /** Jump labels in scope for this node. */
    private val Node.loops: NameMap<JumpLabel, InfiniteLoopNode> by attribute {
        when (val parent = tree.parent(this)) {
            null ->
                NameMap()
            is InfiniteLoopNode ->
                parent.loops.put(parent.jumpLabel, parent)
            else ->
                parent.loops
        }
    }

    /**
     * Threads a context through the program according to the scoping rules.
     *
     * @param defines Returns the name and the context information defined by this node, or `null`
     *   if this node does not define a new name.
     */
    private inner class Context<N : Name, Data>(defines: (Node) -> Pair<Located<N>, Data>?) :
        ReadOnlyProperty<Node, NameMap<N, Data>> {
        /** Context just before this node. */
        private val Node.contextIn: NameMap<N, Data> by attribute {
            val parent = tree.parent(this)
            val previousSibling = tree.previousSibling(this)
            when {
                parent == null ->
                    NameMap()
                parent is BlockNode && previousSibling != null ->
                    previousSibling.contextOut
                else ->
                    parent.contextIn
            }
        }

        /** Context just after this node. */
        private val Node.contextOut: NameMap<N, Data> by attribute {
            defines(this).let {
                if (it == null) contextIn else contextIn.put(it.first, it.second)
            }
        }

        override fun getValue(thisRef: Node, property: KProperty<*>): NameMap<N, Data> =
            thisRef.contextIn
    }

    /** Returns the statement that defines the [Temporary] in [node]. */
    fun declaration(node: ReadNode): TemporaryDefinition =
        node.temporaryDefinitions[node.temporary]

    /** Returns the statement that declares the [ObjectVariable] in [node]. */
    fun declaration(node: QueryNode): DeclarationNode =
        node.objectDeclarations[node.variable]

    /** Returns the statement that declares the [ObjectVariable] in [node]. */
    fun declaration(node: UpdateNode): DeclarationNode =
        node.objectDeclarations[node.variable]

    /** Returns the loop that [node] is breaking out of. */
    fun loop(node: BreakNode): InfiniteLoopNode =
        node.loops[node.jumpLabel]

    /** Returns the declaration of the [Host] in [node]. */
    fun declaration(node: ExternalCommunicationNode): HostDeclarationNode =
        node.hostDeclarations[node.host]

    /** Returns the declaration of the [Protocol] in [node]. */
    fun declaration(node: InternalCommunicationNode): ProcessDeclarationNode =
        node.protocolDeclarations[node.protocol]

    /**
     * Asserts that every referenced [Name] has a declaration, and that no [Name] is declared
     * multiple times in the same scope.
     *
     * @throws UndefinedNameError if a referenced [Name] is not in scope.
     * @throws NameClashError if a [Name] is declared multiple times in the same scope.
     */
    fun check() {
        fun check(node: Node) {
            // Check that name references are valid
            when (node) {
                is ReadNode ->
                    declaration(node)
                is QueryNode ->
                    declaration(node)
                is UpdateNode ->
                    declaration(node)
                is BreakNode ->
                    loop(node)
                is ExternalCommunicationNode ->
                    declaration(node)
                is InternalCommunicationNode ->
                    declaration(node)
            }
            // Check that there are no name clashes
            when (node) {
                is TemporaryDefinition ->
                    node.temporaryDefinitions.put(node.temporary, node)
                is DeclarationNode ->
                    node.objectDeclarations.put(node.variable, node)
                is InfiniteLoopNode ->
                    node.loops.put(node.jumpLabel, node)
                is ProgramNode -> {
                    // Forcing these thunks
                    node.hostDeclarations
                    node.protocolDeclarations
                }
            }
            // Check the children
            node.children.forEach(::check)
        }
        check(tree.root)
    }
}

/** A persistent map from [Name]s to [Data]. */
private class NameMap<N : Name, Data>
private constructor(private val map: PersistentMap<N, Pair<Data, SourceLocation>>) {
    /** The empty map. */
    constructor() : this(persistentMapOf())

    /**
     * Returns the data associated with [name].
     *
     * @throws UndefinedNameError if [name] is not in the map.
     */
    operator fun get(name: Located<N>): Data {
        return map[name.value]?.first ?: throw UndefinedNameError(name)
    }

    /**
     * Returns a new map where [name] is associated with [data].
     *
     * @throws NameClashError if [name] is already in the map.
     */
    fun put(name: Located<N>, data: Data): NameMap<N, Data> {
        val previousDeclaration = map[name.value]?.second
        if (previousDeclaration != null) {
            throw NameClashError(name.value, previousDeclaration, name.sourceLocation)
        }
        return NameMap(map.put(name.value, Pair(data, name.sourceLocation)))
    }
}
