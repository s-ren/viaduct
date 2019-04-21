package edu.cornell.cs.apl.viaduct;

public class PdgLabelDataflow<T extends AstNode> extends PdgDataflow<Label, T> {
  protected Label input(PdgNode<T> node) {
    return node.getInLabel();
  }

  protected Label output(PdgNode<T> node) {
    return node.getOutLabel();
  }

  protected Label transfer(PdgNode<T> node, Label nextInput) {
    // if the node is a downgrade node, prevent transfer;
    // the out label is permanently the downgrade label
    if (node.isDowngradeNode()) {
      return node.getOutLabel();
    } else {
      return nextInput;
    }
  }

  protected void updateInput(PdgNode<T> node, Label nextInput) {
    node.setInLabel(nextInput);
  }

  protected void updateOutput(PdgNode<T> node, Label nextOutput) {
    node.setOutLabel(nextOutput);
  }
}
