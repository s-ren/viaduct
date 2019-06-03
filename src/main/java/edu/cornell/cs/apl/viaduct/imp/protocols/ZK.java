package edu.cornell.cs.apl.viaduct.imp.protocols;

import edu.cornell.cs.apl.viaduct.Binding;
import edu.cornell.cs.apl.viaduct.PdgNode;
import edu.cornell.cs.apl.viaduct.Protocol;
import edu.cornell.cs.apl.viaduct.ProtocolInstantiationInfo;
import edu.cornell.cs.apl.viaduct.imp.ast.Host;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** non-interactive zero-knowledge proof. */
public class ZK implements Protocol<ImpAstNode> {
  private Host prover;
  private Host verifier;

  public ZK(Host p, Host v) {
    this.prover = p;
    this.verifier = v;
  }

  @Override
  public Set<Host> getHosts() {
    Set<Host> hosts = new HashSet<>();
    hosts.add(this.prover);
    hosts.add(this.verifier);
    return hosts;
  }

  @Override
  public Set<Host> readFrom(
      PdgNode<ImpAstNode> node, Host h, ProtocolInstantiationInfo<ImpAstNode> info) {

    // TODO: finish
    return new HashSet<>();
  }

  @Override
  public Binding<ImpAstNode> readPostprocess(
      Map<Host, Binding<ImpAstNode>> hostBindings,
      Host host,
      ProtocolInstantiationInfo<ImpAstNode> info) {

    // TODO: finish
    return null;
  }

  @Override
  public void writeTo(
      PdgNode<ImpAstNode> node,
      Host h,
      ImpAstNode val,
      ProtocolInstantiationInfo<ImpAstNode> info) {

    // TODO: finish
  }

  @Override
  public void instantiate(PdgNode<ImpAstNode> node, ProtocolInstantiationInfo<ImpAstNode> info) {

    // TODO: finish
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }

    if (o instanceof ZK) {
      ZK ozk = (ZK) o;
      boolean peq = this.prover.equals(ozk.prover);
      boolean veq = this.prover.equals(ozk.prover);
      return peq && veq;
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.prover, this.verifier);
  }

  @Override
  public String toString() {
    String pname = this.prover.toString();
    String vname = this.verifier.toString();
    return String.format("ZK(%s,%s)", pname, vname);
  }
}
