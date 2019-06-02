package edu.cornell.cs.apl.viaduct;

import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.AstNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Host;
import java.util.Map;
import java.util.Set;

/** A cryptographic protocol for instantiating a PDG node. */
public interface Protocol<T extends AstNode> {
  Set<Protocol<T>> createInstances(
      HostTrustConfiguration hostConfig,
      Map<PdgNode<T>, Protocol<T>> currProtoMap,
      PdgNode<T> node);

  Set<Host> readFrom(PdgNode<T> node, Host h, PdgNode<T> reader, ProtocolInstantiationInfo<T> info);

  void writeTo(
      PdgNode<T> node, Host h, PdgNode<T> writer, T val, ProtocolInstantiationInfo<T> info);

  void instantiate(PdgNode<T> node, ProtocolInstantiationInfo<T> info);
}
