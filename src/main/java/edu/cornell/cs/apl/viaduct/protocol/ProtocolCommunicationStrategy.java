package edu.cornell.cs.apl.viaduct.protocol;

import edu.cornell.cs.apl.viaduct.AstNode;
import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.Host;

import java.util.Map;
import java.util.Set;

/** determines how protocols will communicate with each other. */
public interface ProtocolCommunicationStrategy<T extends AstNode> {
  Map<Host, Set<Host>> getCommunication(
      HostTrustConfiguration hostConfig, Protocol<T> fromProtocol, Protocol<T> toProtocol);
}