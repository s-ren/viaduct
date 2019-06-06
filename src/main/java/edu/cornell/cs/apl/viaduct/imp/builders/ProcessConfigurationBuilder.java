package edu.cornell.cs.apl.viaduct.imp.builders;

import edu.cornell.cs.apl.viaduct.FreshNameGenerator;
import edu.cornell.cs.apl.viaduct.imp.DuplicateProcessDefinitionException;
import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.Host;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.imp.dataflow.CopyPropagation;
import edu.cornell.cs.apl.viaduct.imp.visitors.TargetPostprocessVisitor;
import java.util.HashMap;
import java.util.Map;

// TODO: roll into ProgramNode.Builder

/** Builds process configurations. */
public class ProcessConfigurationBuilder {
  private final Map<Host, StmtBuilder> configBuilder;
  private final FreshNameGenerator freshNameGenerator;

  /** create statement builders for each host. */
  public ProcessConfigurationBuilder(HostTrustConfiguration config) {
    this.configBuilder = new HashMap<>();
    this.freshNameGenerator = new FreshNameGenerator();
    for (Host h : config.hosts()) {
      this.configBuilder.put(h, new StmtBuilder());
    }
  }

  /** Retrieve the process configuration. */
  public ProgramNode build() {
    ProgramNode.Builder programBuilder = ProgramNode.builder();
    TargetPostprocessVisitor postprocessor = new TargetPostprocessVisitor();

    try {
      CopyPropagation copyProp = new CopyPropagation();
      for (Map.Entry<Host, StmtBuilder> kv : configBuilder.entrySet()) {
        Host host = kv.getKey();
        StmtNode program = kv.getValue().build();
        StmtNode postprocessedProgram = postprocessor.postprocess(host, program);
        StmtNode postprocessedProgram2 = copyProp.propagateCopies(postprocessedProgram);
        programBuilder.addProcess(new ProcessName(host), postprocessedProgram2);
      }
    } catch (DuplicateProcessDefinitionException e) {
      throw new Error(e);
    }

    return programBuilder.build();
  }

  /**
   * Create a new process at the given host.
   *
   * @return true if a process at the host did not already exist
   */
  public boolean createProcess(Host h) {
    if (!this.configBuilder.containsKey(h)) {
      this.configBuilder.put(h, new StmtBuilder());
      return true;

    } else {
      return false;
    }
  }

  public StmtBuilder getBuilder(Host h) {
    return this.configBuilder.get(h);
  }

  /** Get a fresh name. */
  public String getFreshName(String base) {
    return this.freshNameGenerator.getFreshName(base);
  }

  /** Get a fresh variable. */
  public Variable getFreshVar(String base) {
    return new Variable(getFreshName(base));
  }
}