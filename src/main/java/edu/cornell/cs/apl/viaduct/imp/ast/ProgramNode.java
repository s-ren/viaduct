package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.errors.NameClashError;
import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.parser.Located;
import edu.cornell.cs.apl.viaduct.imp.visitors.ProgramVisitor;
import edu.cornell.cs.apl.viaduct.security.Label;
import io.vavr.Tuple2;
import io.vavr.collection.SortedMap;
import io.vavr.collection.TreeMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import javax.annotation.Nonnull;

/**
 * A program is a set of process definitions and a host trust configuration. It associates with each
 * process the code running on that process, and with each host its trustworthiness.
 */
public final class ProgramNode extends ImpAstNode
    implements Iterable<Tuple2<ProcessName, StatementNode>> {
  private final SortedMap<ProcessName, StatementNode> processes;
  private final HostTrustConfiguration trustConfiguration;

  // TODO: add mapping to protocols.

  private ProgramNode(
      SortedMap<ProcessName, StatementNode> processes, HostTrustConfiguration trustConfiguration) {
    this.processes = processes;
    this.trustConfiguration = trustConfiguration;
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Return the code to be executed on the given process. */
  public StatementNode getProcessCode(ProcessName processName) {
    return processes
        .get(processName)
        .getOrElseThrow(() -> new NoSuchElementException(processName.toString()));
  }

  /** Return a list of all processes in the configuration. */
  public Iterable<ProcessName> processes() {
    return this.processes.keySet();
  }

  /** Return the host trust configuration. */
  public HostTrustConfiguration getHostTrustConfiguration() {
    return trustConfiguration;
  }

  @Override
  public @Nonnull Iterator<Tuple2<ProcessName, StatementNode>> iterator() {
    return this.processes.iterator();
  }

  public <R> R accept(ProgramVisitor<R> v) {
    return v.visit(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof ProgramNode)) {
      return false;
    }

    final ProgramNode that = (ProgramNode) o;
    return Objects.equals(this.processes, that.processes)
        && Objects.equals(this.trustConfiguration, that.trustConfiguration);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.processes, trustConfiguration);
  }

  @Override
  public String toString() {
    StringBuffer buffer = new StringBuffer();
    for (Tuple2<ProcessName, StatementNode> proc : this.processes) {
      buffer.append(String.format("(process %s %s)%n", proc._1(), proc._2()));
    }
    return buffer.toString();
  }

  public static final class Builder {
    private final Map<ProcessName, StatementNode> processes = new HashMap<>();
    private final HostTrustConfiguration.Builder hostConfigBuilder =
        HostTrustConfiguration.builder();

    private Builder() {}

    /** Return the built program. */
    public ProgramNode build() {
      return new ProgramNode(TreeMap.ofAll(processes), hostConfigBuilder.build());
    }

    /**
     * Add a process definition.
     *
     * @param processName name of the process
     * @param statement code to be executed by this process
     * @throws NameClashError if the process was added previously
     */
    public Builder addProcess(ProcessName processName, StatementNode statement)
        throws NameClashError {
      Objects.requireNonNull(processName);
      Objects.requireNonNull(statement);

      if (processes.containsKey(processName)) {
        // TODO: do a better job of recovering this
        final Located previousDeclaration =
            processes.keySet().stream()
                .filter(processName::equals)
                .findAny()
                .orElseThrow(NullPointerException::new);
        throw new NameClashError(previousDeclaration, processName);
      }
      processes.put(processName, statement);

      return this;
    }

    /** Add a host trust declaration. */
    public Builder addHost(Host host, Label trust) throws NameClashError {
      hostConfigBuilder.addHost(host, trust);
      return this;
    }

    /**
     * Add all process definitions and host declarations in the given program. Overwrites existing
     * definitions with new ones if there are clashes (instead of throwing an exception).
     */
    public Builder addAll(ProgramNode newDefinitions) {
      processes.putAll(newDefinitions.processes.toJavaMap());
      hostConfigBuilder.addAll(newDefinitions.getHostTrustConfiguration());
      return this;
    }

    /**
     * Add all declarations in the given trust configuration. Overwrites existing declarations with
     * the new ones if there are clashes instead of throwing an exception.
     */
    public Builder addHosts(HostTrustConfiguration newTrustDeclarations) {
      hostConfigBuilder.addAll(newTrustDeclarations);
      return this;
    }
  }
}
