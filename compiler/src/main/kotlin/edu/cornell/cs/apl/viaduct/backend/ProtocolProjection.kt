package edu.cornell.cs.apl.viaduct.backend

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.Name
import edu.cornell.cs.apl.viaduct.syntax.Protocol

data class ProtocolProjection(
    val protocol: Protocol,
    val host: Host
) : Name, Comparable<ProtocolProjection> {
    override val name = toString()

    override val nameCategory = "ProtocolProjection"

    override val asDocument = Document(name)

    override fun toString(): String {
        return "${protocol.name}@$host"
    }

    override fun compareTo(other: ProtocolProjection): Int {
        val protocolCmp: Int = protocol.compareTo(other.protocol)

        return if (protocolCmp == 0) {
            host.compareTo(other.host)
        } else {
            return protocolCmp
        }
    }
}