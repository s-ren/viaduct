package edu.cornell.cs.apl.viaduct.cli;

import com.github.rvesse.airline.annotations.Command;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.visitors.ElaborationVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.PrintVisitor;
import java.io.BufferedWriter;

@Command(name = "format", description = "Pretty print source program")
public class FormatCommand extends BaseCommand {
  @Override
  public Void call() throws Exception {
    // parse
    final ProgramNode program = this.input.parse();

    // typecheck
    // final TypeCheckVisitor typeChecker = new TypeCheckVisitor();
    // typeChecker.run(program);

    // print (de-parse)
    try (BufferedWriter writer = this.output.newOutputWriter()) {
      // writer.write(PrintVisitor.run(new ElaborationVisitor().run(program)));
      writer.write(PrintVisitor.run(program));
      writer.newLine();
    }
    return null;
  }
}
