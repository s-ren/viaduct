package edu.cornell.cs.apl.viaduct.imp.protocols;

import edu.cornell.cs.apl.viaduct.Binding;
import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Host;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.imp.builders.ExpressionBuilder;
import edu.cornell.cs.apl.viaduct.imp.builders.StmtBuilder;
import edu.cornell.cs.apl.viaduct.pdg.PdgComputeNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgStorageNode;
import edu.cornell.cs.apl.viaduct.protocol.Protocol;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolInstantiationException;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolInstantiationInfo;
import edu.cornell.cs.apl.viaduct.security.Label;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** replication protocol. */
public class Replication extends Cleartext implements Protocol<ImpAstNode> {
  private final Set<Host> replicas;
  private final Label trust;
  private final Map<Host, Variable> outVarMap;

  /** constructor. */
  public Replication(HostTrustConfiguration hostConfig, Set<Host> replicas) {
    this.replicas = replicas;
    this.outVarMap = new HashMap<>();

    Label label = Label.top();
    for (Host replica : replicas) {
      label = label.meet(hostConfig.getTrust(replica));
    }
    this.trust = label;
  }

  public Set<Host> getReplicas() {
    return this.replicas;
  }

  public int getNumReplicas() {
    return this.replicas.size();
  }

  @Override
  public Set<Host> getHosts() {
    return this.replicas;
  }

  @Override
  public Label getTrust() {
    return this.trust;
  }

  @Override
  public String getId() {
    return "Replication";
  }

  @Override
  public void initialize(PdgNode<ImpAstNode> node, ProtocolInstantiationInfo<ImpAstNode> info) {
    return;
  }

  @Override
  public void instantiate(PdgNode<ImpAstNode> node, ProtocolInstantiationInfo<ImpAstNode> info) {
    if (node.isStorageNode()) {
      for (Host realHost : this.replicas) {
        Variable hostStorageVar =
            instantiateStorageNode(realHost, (PdgStorageNode<ImpAstNode>) node, info);
        this.outVarMap.put(realHost, hostStorageVar);
      }

    } else if (node.isComputeNode()) {
      for (Host realHost : this.replicas) {
        Variable hostOutVar =
            instantiateComputeNode(realHost, (PdgComputeNode<ImpAstNode>) node, info);
        this.outVarMap.put(realHost, hostOutVar);
      }

    } else {
      throw new ProtocolInstantiationException("control nodes must have Control protocol");
    }
  }

  @Override
  public Binding<ImpAstNode> readFrom(
      PdgNode<ImpAstNode> node,
      PdgNode<ImpAstNode> readNode,
      Host readHost,
      Binding<ImpAstNode> readLabel,
      List<ImpAstNode> args,
      ProtocolInstantiationInfo<ImpAstNode> info) {

    // should not be read from until it has been instantiated
    assert this.outVarMap.size() == getNumReplicas();

    Map<Host, Binding<ImpAstNode>> hostBindings = new HashMap<>();
    StmtBuilder readBuilder = info.getBuilder(readHost);
    Set<Host> outHosts = info.getReadSet(node, readNode, readHost);

    for (Host outHost : outHosts) {
      Variable outVar = this.outVarMap.get(outHost);
      Binding<ImpAstNode> readVar =
          performRead(node, readHost, readLabel,
            outHost, outVar, args, info);
      hostBindings.put(outHost, readVar);
    }

    if (hostBindings.size() > 1) {
      Binding<ImpAstNode> curBinding = null;
      ExpressionNode curExpr = null;
      ExpressionBuilder e = new ExpressionBuilder();
      for (Binding<ImpAstNode> binding : hostBindings.values()) {
        if (curBinding == null) {
          curBinding = binding;

        } else {
          if (curExpr == null) {
            curExpr = e.equals(e.var(curBinding), e.var(binding));
            curBinding = binding;

          } else {
            curExpr = e.and(curExpr, e.equals(e.var(curBinding), e.var(binding)));
            curBinding = binding;
          }
        }
      }

      readBuilder.assertion(curExpr);
    }

    Host h = (Host) hostBindings.keySet().toArray()[0];
    return hostBindings.get(h);
  }

  @Override
  public void writeTo(
      PdgNode<ImpAstNode> node,
      PdgNode<ImpAstNode> writeNode,
      Host writeHost,
      List<ImpAstNode> args,
      ProtocolInstantiationInfo<ImpAstNode> info) {

    if (node.isStorageNode()) {
      // node must have been instantiated before being written to
      assert this.outVarMap.size() == getNumReplicas();

      Set<Host> inHosts = info.getWriteSet(writeNode, node, writeHost);
      for (Host inHost : inHosts) {
        Variable storageVar = this.outVarMap.get(inHost);
        performWrite(node, writeHost, inHost, storageVar, args, info);
      }

    } else {
      throw new ProtocolInstantiationException(
          "attempted to write to a non storage node");
    }
  }


  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }

    if (o instanceof Replication) {
      Replication other = (Replication) o;
      boolean realEq = this.replicas.equals(other.replicas);
      return realEq;

    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.replicas);
  }

  @Override
  public String toString() {
    HashSet<String> realStrs = new HashSet<>();
    for (Host real : this.replicas) {
      realStrs.add(real.toString());
    }

    String realList = String.join(",", realStrs);
    return String.format("Replication({%s})", realList);
  }
}
