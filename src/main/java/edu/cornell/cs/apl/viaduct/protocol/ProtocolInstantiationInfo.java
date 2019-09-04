package edu.cornell.cs.apl.viaduct.protocol;

import edu.cornell.cs.apl.viaduct.AstNode;
import edu.cornell.cs.apl.viaduct.Binding;
import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.Host;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.imp.builders.ProcessConfigurationBuilder;
import edu.cornell.cs.apl.viaduct.imp.builders.StmtBuilder;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import edu.cornell.cs.apl.viaduct.pdg.ProgramDependencyGraph.ControlLabel;

import java.util.Map;
import java.util.Set;
import java.util.Stack;

/** helper class for protocol instantiation. */
public class ProtocolInstantiationInfo<T extends AstNode> {
  private final HostTrustConfiguration hostConfig;
  private final ProtocolCommunicationStrategy<T> communicationStrategy;
  private final ProcessConfigurationBuilder pconfig;
  private final Map<PdgNode<T>, Protocol<T>> protocolMap;
  private final Stack<Set<Host>> controlContext;
  private final Stack<Set<Host>> loopControlContext;

  /** store config builder and protocol map. */
  public ProtocolInstantiationInfo(
      HostTrustConfiguration hostConfig,
      ProtocolCommunicationStrategy<T> communicationStrategy,
      ProcessConfigurationBuilder processConfig,
      Map<PdgNode<T>, Protocol<T>> protocolMap) {

    this.hostConfig = hostConfig;
    this.communicationStrategy = communicationStrategy;
    this.pconfig = processConfig;
    this.protocolMap = protocolMap;
    this.controlContext = new Stack<>();
    this.loopControlContext = new Stack<>();
  }

  /** get the set of hosts to read from. */
  public Set<Host> getReadSet(PdgNode<T> writeNode, PdgNode<T> readNode, Host host) {
    final Protocol<T> fromProtocol = this.protocolMap.get(writeNode);
    final Protocol<T> toProtocol = this.protocolMap.get(readNode);
    return this.communicationStrategy.getReadSet(this.hostConfig, fromProtocol, toProtocol, host);
  }

  /** get the set of hosts to write to. */
  public Set<Host> getWriteSet(PdgNode<T> writeNode, PdgNode<T> readNode, Host host) {
    final Protocol<T> fromProtocol = this.protocolMap.get(writeNode);
    final Protocol<T> toProtocol = this.protocolMap.get(readNode);
    return this.communicationStrategy.getWriteSet(this.hostConfig, fromProtocol, toProtocol, host);
  }

  public StmtBuilder createProcess(Host h) {
    this.pconfig.createProcess(h);
    return this.pconfig.getBuilder(h);
  }

  public Protocol<T> getProtocol(PdgNode<T> node) {
    return this.protocolMap.get(node);
  }

  public StmtBuilder getBuilder(Host h) {
    return this.pconfig.getBuilder(h);
  }

  public Variable getFreshVar(String base) {
    return this.pconfig.getFreshVar(base);
  }

  public Variable getFreshVar(Binding<T> base) {
    return getFreshVar(base.getBinding());
  }

  public String getFreshName(String base) {
    return this.pconfig.getFreshName(base);
  }

  public String getFreshName(Binding<T> base) {
    return getFreshName(base.getBinding());
  }

  public boolean isLoopControlContextEmpty() {
    return this.loopControlContext.isEmpty();
  }

  /** get hosts in loop control context. */
  public Set<Host> getCurrentLoopControlContext() {
    if (!this.loopControlContext.isEmpty()) {
      return this.loopControlContext.peek();

    } else {
      throw new ProtocolInstantiationError(
          "attempting to peek empty loop control context stack");
    }
  }

  public void pushLoopControlContext(Set<Host> hosts) {
    this.loopControlContext.push(hosts);
  }

  public void popLoopControlContext() {
    this.loopControlContext.pop();
  }

  public boolean isControlContextEmpty() {
    return this.controlContext.isEmpty();
  }

  /** get the hosts participating in the control context. */
  public Set<Host> getCurrentControlContext() {
    if (!this.controlContext.isEmpty()) {
      return this.controlContext.peek();

    } else {
      throw new ProtocolInstantiationError("attempting to peek empty control context stack");
    }
  }

  public void pushControlContext(Set<Host> hosts) {
    this.controlContext.push(hosts);
  }

  /** set the current path for all hosts participating in the control structure. */
  public void setCurrentPath(ControlLabel label) {
    assert !this.controlContext.isEmpty();

    Set<Host> hosts = this.controlContext.peek();
    for (Host host : hosts) {
      StmtBuilder hostBuilder = this.pconfig.getBuilder(host);
      hostBuilder.setCurrentPath(label);
    }
  }

  /** finish the current execution path for all hosts participating in control structure. */
  public void finishCurrentPath() {
    assert !this.controlContext.isEmpty();

    Set<Host> hosts = this.controlContext.peek();
    for (Host host : hosts) {
      StmtBuilder hostBuilder = this.pconfig.getBuilder(host);
      hostBuilder.finishCurrentPath();
    }
  }

  /** pop the current control structure for all participating hosts. */
  public void popControlContext() {
    assert !this.controlContext.isEmpty();

    Set<Host> hosts = this.controlContext.peek();
    for (Host host : hosts) {
      StmtBuilder hostBuilder = this.pconfig.getBuilder(host);
      hostBuilder.popControl();
    }
    this.controlContext.pop();
  }
}
