package edu.cornell.cs.apl.viaduct.cli;

import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ImpCommunicationCostEstimator;
import edu.cornell.cs.apl.viaduct.imp.ImpProtocolSearchStrategy;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
import edu.cornell.cs.apl.viaduct.imp.informationflow.InformationFlowChecker;
import edu.cornell.cs.apl.viaduct.imp.parser.TrustConfigurationParser;
import edu.cornell.cs.apl.viaduct.imp.visitors.ImpPdgBuilderPreprocessVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.ImpPdgBuilderVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.ImpProtocolInstantiationVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.PrintVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.TypeCheckVisitor;
import edu.cornell.cs.apl.viaduct.pdg.PdgDotPrinter;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import edu.cornell.cs.apl.viaduct.pdg.ProgramDependencyGraph;
import edu.cornell.cs.apl.viaduct.protocol.Protocol;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolSelection;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.engine.GraphvizCmdLineEngine;
import guru.nidi.graphviz.engine.GraphvizServerEngine;
import guru.nidi.graphviz.engine.GraphvizV8Engine;
import guru.nidi.graphviz.model.MutableGraph;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.apache.commons.io.FilenameUtils;

@Command(name = "compile", description = "Compile ideal protocol to secure distributed program")
public class CompileCommand extends BaseCommand {
  /** Stop graphviz-java from using the deprecated Nashorn Javascript engine. */
  static {
    Graphviz.useEngine(
        new GraphvizCmdLineEngine(), new GraphvizV8Engine(), new GraphvizServerEngine());
  }

  @Option(
      name = {"-c", "--hosts"},
      title = "files",
      description =
          "Read host trust configuration from <files>."
              + " If multiple files are given, declarations in all files are combined together."
              + " Declarations in later files overwrite earlier ones when there are multiple"
              + " declarations for the same host.")
  private List<String> hostConfigurationFiles = null;

  @Option(
      name = {"-l", "--label-graph"},
      title = "file.ext",
      description =
          "Write program dependency graph with security labels to <file.ext>."
              + " File extension determines output format."
              + " Supported formats are the same as the ones in Graphviz."
              + " Most useful ones are .dot, .svg, .png, and .pdf.")
  // TODO: enable checks when we learn how to remove them from generated help.
  // @Once
  // @com.github.rvesse.airline.annotations.restrictions.File(readable = false)
  private String labelGraphOutput = null;

  @Option(
      name = {"-p", "--protocol-graph"},
      title = "file.ext",
      description =
          "Write program dependency graph with selected cryptographic protocols to <file.ext>"
              + " File extension determines output format. See --label-graph.")
  // TODO: same.
  // @Once
  // @com.github.rvesse.airline.annotations.restrictions.File(readable = false)
  private String protocolGraphOutput = null;

  /**
   * Write the given graph to the output file if the filename is not {@code null}. Do nothing
   * otherwise. The output format is determined automatically from the file extension.
   *
   * @param graph graph to output
   * @param file name of the file to output to
   */
  private static void dumpGraph(Supplier<MutableGraph> graph, String file) throws Exception {
    if (file == null) {
      return;
    }

    String fileExtension = FilenameUtils.getExtension(file);
    if (fileExtension.equals("dot")) {
      try (Writer writer =
          new BufferedWriter(
              new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
        writer.write(graph.get().toString());
      }
    } else {
      Format format = formatFromExtension(fileExtension);
      Graphviz.fromGraph(graph.get()).render(format).toFile(new File(file));
    }
  }

  /** Compute graph output format from the file extension. */
  private static Format formatFromExtension(String extension) throws Exception {
    switch (extension.toLowerCase()) {
      case "json":
        return Format.JSON0;
      case "png":
        return Format.PNG;
      case "svg":
        return Format.SVG;
      case "txt":
        return Format.PLAIN;
      case "xdot":
        return Format.XDOT;
      default:
        throw new Exception("Unknown extension: " + extension);
    }
  }

  @Override
  public Void call() throws Exception {
    final ProgramNode program = this.parse();

    final HostTrustConfiguration trustConfiguration = program.getHostTrustConfiguration();

    StmtNode main = program.getProcessCode(ProcessName.getMain());
    ImpPdgBuilderPreprocessVisitor preprocessor = new ImpPdgBuilderPreprocessVisitor();
    main = preprocessor.run(main);

    final TypeCheckVisitor typeChecker = new TypeCheckVisitor();
    typeChecker.run(main);

    // information flow constraint solving
    InformationFlowChecker.run(main);

    // Generate program dependency graph.
    final ProgramDependencyGraph<ImpAstNode> pdg = new ImpPdgBuilderVisitor().generatePDG(main);

    PrintVisitor printer = new PrintVisitor(false);
    // Dump PDG with information flow labels to a file (if requested).
    dumpGraph(() -> PdgDotPrinter.pdgDotGraphWithLabels(pdg, printer),
        labelGraphOutput);

    // Select cryptographic protocols for each node.
    final ImpProtocolSearchStrategy strategy =
        new ImpProtocolSearchStrategy(new ImpCommunicationCostEstimator());
    final Map<PdgNode<ImpAstNode>, Protocol<ImpAstNode>> protocolMap =
        new ProtocolSelection<>(strategy).selectProtocols(trustConfiguration, pdg);

    System.out.println("SYNTHESIZED PROTOCOL:");
    System.out.println(strategy.estimatePdgCost(protocolMap, pdg));

    // Dump PDG with protocol information to a file (if requested).
    dumpGraph(() -> PdgDotPrinter.pdgDotGraphWithProtocols(pdg, protocolMap, strategy, printer),
        protocolGraphOutput);

    if (pdg.getOrderedNodes().size() == protocolMap.size()) {
      // Found a protocol for every node! Output synthesized distributed program.
      final ProgramNode generatedProgram =
          new ImpProtocolInstantiationVisitor(trustConfiguration, pdg, protocolMap, main).run();

      try (BufferedWriter writer = output.newOutputWriter()) {
        writer.write(PrintVisitor.run(generatedProgram));
        writer.newLine();
      }

      // TODO: This gets printed in between System.out
      // int protocolCost = costEstimator.estimatePdgCost(protocolMap, pdg);
      // System.err.println("Protocol cost: " + protocolCost);
    } else {
      // We couldn't find protocols for some nodes.
      final StringBuilder error = new StringBuilder();

      error.append("Could not find protocols for some nodes.\r\n");
      for (PdgNode<ImpAstNode> node : pdg.getOrderedNodes()) {
        final String astStr = node.getAstNode().toString();

        final Protocol<ImpAstNode> protocol = protocolMap.get(node);
        final String protocolStr = protocol == null ? "NO PROTOCOL" : protocol.toString();

        final String labelStr = node.getLabel().toString();

        error.append("\r\n");
        error.append(String.format("%s (label: %s) => %s", astStr, labelStr, protocolStr));
      }

      throw new Exception(error.toString());
    }

    return null;
  }

  /** Parse input program as well as all host configuration files, and return them in one bundle. */
  private ProgramNode parse() throws Exception {
    final ProgramNode.Builder builder = ProgramNode.builder();
    builder.addAll(this.input.parse());

    // Append trust configurations.
    if (hostConfigurationFiles != null) {
      for (String hostConfig : hostConfigurationFiles) {
        builder.addHosts(TrustConfigurationParser.parse(new File(hostConfig)));
      }
    }

    return builder.build();
  }
}
