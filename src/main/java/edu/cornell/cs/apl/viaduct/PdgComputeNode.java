package edu.cornell.cs.apl.viaduct;

/** PDG compute node, which represents expressions or statements. */
public class PdgComputeNode extends PdgNode {
  // only downgrade nodes have two different labels; every other node
  // only has a single label
  Label inLabel;
  Label outLabel;
  boolean isDowngrade;

  /** constructor. */
  public PdgComputeNode(AstNode astNode, Label label) {
    super(astNode);
    this.inLabel = label;
    this.outLabel = label;
    this.isDowngrade = false;
  }

  /** constructor. */
  public PdgComputeNode(AstNode astNode, Label inLabel, Label outLabel) {
    super(astNode);
    this.inLabel = inLabel;
    this.outLabel = outLabel;
    this.isDowngrade = true;
  }

  @Override
  public Label getLabel() {
    return this.getOutLabel();
  }

  @Override
  public void setLabel(Label label) {
    this.inLabel = label;
    this.outLabel = label;
  }

  @Override
  public Label getInLabel() {
    if (this.isDowngrade) {
      return this.inLabel;

    } else {
      return this.getLabel();
    }
  }

  @Override
  public void setInLabel(Label label) {
    this.inLabel = label;
  }

  @Override
  public Label getOutLabel() {
    if (this.isDowngrade) {
      return this.outLabel;
    } else {
      return this.getLabel();
    }
  }

  @Override
  public void setOutLabel(Label label) {
    this.inLabel = label;
  }

  @Override
  public boolean isStorageNode() {
    return false;
  }

  @Override
  public boolean isComputeNode() {
    return true;
  }

  @Override
  public boolean isDowngradeNode() {
    return this.isDowngrade;
  }

  @Override
  public String toString() {
    if (this.isDowngrade) {
      return "<downgrade compute node for " + this.astNode.toString() + ">";

    } else {
      return "<compute node for " + this.astNode.toString() + ">";
    }
  }
}
