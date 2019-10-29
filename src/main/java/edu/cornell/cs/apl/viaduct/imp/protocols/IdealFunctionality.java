package edu.cornell.cs.apl.viaduct.imp.protocols;

import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.HostName;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;
import edu.cornell.cs.apl.viaduct.imp.ast.ReadNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.imp.builders.StmtBuilder;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolInstantiationInfo;
import edu.cornell.cs.apl.viaduct.security.Label;

import java.util.List;
import java.util.Objects;

/** protocol that corresponds to how the ideal functionality for a specification program acts;
 * the protocol interacts with a simulator process as dictated by a given corruption label.
*/
public class IdealFunctionality extends AbstractSingle {
  private static final String CORRUPTED_VALUE = "CORRUPT";

  private final HostName simulator;
  private final Label corruptionLabel;

  /** constructor. */
  public IdealFunctionality(HostName host, HostName simulator, Label corruptionLabel) {
    super(host, Label.strongest());
    this.simulator = simulator;
    this.corruptionLabel = corruptionLabel;
  }

  @Override
  public String getId() {
    return "IdealFunctionality";
  }

  @Override
  public boolean hasSyntheticProcesses() {
    return false;
  }

  @Override
  public void initialize(PdgNode<ImpAstNode> node, ProtocolInstantiationInfo<ImpAstNode> info) {
    return;
  }

  @Override
  protected ExpressionNode getReadValue(
      PdgNode<ImpAstNode> node, List<Variable> readArgs, Variable outVar,
      ProtocolInstantiationInfo<ImpAstNode> info) {

    ExpressionNode actualReadVal = super.getReadValue(node, readArgs, outVar, info);

    // receive corrupted value from simulator if integrity of corruption label is high enough
    if (this.corruptionLabel.integrity().actsFor(node.getLabel().integrity())) {
      StmtBuilder builder = info.getBuilder(this.process);
      final Variable corruptedVar = info.getFreshVar(CORRUPTED_VALUE);
      builder.recv(this.simulator, corruptedVar);
      return ReadNode.builder().setReference(corruptedVar).build();
      // return actualReadVal;

    } else {
      return actualReadVal;
    }
  }

  @Override
  public void writeTo(
      PdgNode<ImpAstNode> node,
      PdgNode<ImpAstNode> readNode,
      ProcessName writeProcess,
      List<ImpAstNode> args,
      ProtocolInstantiationInfo<ImpAstNode> info) {

    super.writeTo(node, readNode, writeProcess, args, info);

    // broadcast to simulator if confidentiality of corruption label is high enough
    if (this.corruptionLabel.confidentiality().actsFor(node.getLabel().confidentiality())) {
      StmtBuilder builder = info.getBuilder(this.process);
      builder.send(this.simulator, ReadNode.builder().setReference(this.outVar).build());
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }

    if (o instanceof IdealFunctionality) {
      IdealFunctionality that = (IdealFunctionality)o;
      return this.process.equals(that.process)
        && this.simulator.equals(that.simulator);

    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.process, this.simulator);
  }

  @Override
  public String toString() {
    return String.format("IdealFunctionality(%s,%s)",
        this.process.toString(), this.simulator.toString());
  }
}