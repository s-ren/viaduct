package edu.cornell.cs.apl.viaduct.protocol;

import edu.cornell.cs.apl.viaduct.AstNode;
import edu.cornell.cs.apl.viaduct.Binding;
import edu.cornell.cs.apl.viaduct.imp.ast.HostName;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import edu.cornell.cs.apl.viaduct.security.Label;
import java.util.List;
import java.util.Set;

/** A cryptographic protocol for instantiating a PDG node. */
public interface Protocol<T extends AstNode> {
  String getId();

  Set<HostName> getHosts();

  Set<ProcessName> getProcesses();

  boolean hasSyntheticProcesses();

  Label getTrust();

  void initialize(PdgNode<T> node, ProtocolInstantiationInfo<T> info);

  void instantiate(PdgNode<T> node, ProtocolInstantiationInfo<T> info);

  Binding<T> readFrom(
      PdgNode<T> node,
      PdgNode<T> readNode,
      ProcessName readProcess,
      Binding<T> readLabel,
      List<T> args,
      ProtocolInstantiationInfo<T> info);

  void writeTo(
      PdgNode<T> node,
      PdgNode<T> writeNode,
      ProcessName writeProcess,
      List<T> args,
      ProtocolInstantiationInfo<T> info);
}
