package edu.cornell.cs.apl.viaduct.imp.parser;

import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import java.io.StringReader;
import java_cup.runtime.Scanner;

/** Parser for Imp source code. */
public class Parser {
  /** Parse the given string and return the AST. */
  public static ProgramNode parse(String source) throws Exception {
    return parse(SourceFile.from("<string>", new StringReader(source)));
  }

  /** Read and parse the given Imp source file and return the AST. */
  public static ProgramNode parse(SourceFile sourceFile) throws Exception {
    ComplexSymbolFactory symbolFactory = new ComplexSymbolFactory(sourceFile);
    Scanner scanner = new ImpLexer(sourceFile.getReader(), symbolFactory);
    ImpParser parser = new ImpParser(scanner, symbolFactory);
    return (ProgramNode) parser.parse().value;
  }
}
