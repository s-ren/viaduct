package edu.cornell.cs.apl.viaduct.protocol;

import edu.cornell.cs.apl.viaduct.AstNode;
import edu.cornell.cs.apl.viaduct.Binding;
import edu.cornell.cs.apl.viaduct.imp.ast.Host;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;

import java.util.Map;
import java.util.Set;

/** A cryptographic protocol for instantiating a PDG node. */
public interface Protocol<T extends AstNode> {
  Set<Host> getHosts();

  Set<Host> readFrom(PdgNode<T> node, Host h, ProtocolInstantiationInfo<T> info);

  Binding<T> readPostprocess(Map<Host,Binding<T>> hostBindings, Host readHost,
      ProtocolInstantiationInfo<T> info);

  void writeTo(PdgNode<T> node, Host writeHost, T val, ProtocolInstantiationInfo<T> info);

  void instantiate(PdgNode<T> node, ProtocolInstantiationInfo<T> info);
}