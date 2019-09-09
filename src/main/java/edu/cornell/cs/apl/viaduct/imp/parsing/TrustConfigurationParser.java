package edu.cornell.cs.apl.viaduct.imp.parsing;

import edu.cornell.cs.apl.viaduct.errors.ProcessDeclarationInTrustFileError;
import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.HostDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.ast.TopLevelDeclarationNode;

/** Parser for host trust configurations. */
public class TrustConfigurationParser {
  /** Parse a host trust configuration from the given source file. */
  public static HostTrustConfiguration parse(SourceFile source) throws Exception {
    ProgramNode program = Parser.parse(source);
    return extractHostTrustConfiguration(program);
  }

  /**
   * Extract a trust configuration from a program. Asserts that the program does not define any
   * processes.
   */
  private static HostTrustConfiguration extractHostTrustConfiguration(ProgramNode program) {
    final HostTrustConfiguration.Builder configuration = HostTrustConfiguration.builder();

    for (TopLevelDeclarationNode declaration : program) {
      if (declaration instanceof HostDeclarationNode) {
        configuration.add((HostDeclarationNode) declaration);
      } else {
        throw new ProcessDeclarationInTrustFileError((ProcessDeclarationNode) declaration);
      }
    }

    return configuration.build();
  }
}