package edu.cornell.cs.apl.viaduct.imp;

import edu.cornell.cs.apl.viaduct.imp.ast.Host;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
import edu.cornell.cs.apl.viaduct.imp.dataflow.CopyPropagation;
import edu.cornell.cs.apl.viaduct.imp.visitors.EmptyBlockVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.EraseSecurityVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.SelfCommunicationVisitor;

/**
 * Postprocess protocol instantiation.
 *
 * <p>Remove downgrades, self communication, and security labels on variables.
 */
public class TargetPostprocessor {
  private static final SelfCommunicationVisitor selfComm;
  private static final EraseSecurityVisitor eraseSecurity;
  private static final CopyPropagation copyProp;
  private static final EmptyBlockVisitor emptyBlock;

  static {
    selfComm = new SelfCommunicationVisitor();
    eraseSecurity = new EraseSecurityVisitor();
    copyProp = new CopyPropagation();
    emptyBlock = new EmptyBlockVisitor();
  }

  /** set host of current program, then postprocess. */
  public static StmtNode postprocess(Host h, StmtNode program) {
    StmtNode processedProgram = selfComm.run(h, program);
    processedProgram = eraseSecurity.run(processedProgram);
    processedProgram = copyProp.run(processedProgram);
    processedProgram = emptyBlock.run(processedProgram);
    return processedProgram;
  }
}