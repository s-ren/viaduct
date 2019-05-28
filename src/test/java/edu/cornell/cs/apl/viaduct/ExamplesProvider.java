package edu.cornell.cs.apl.viaduct;

import java.io.File;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

/** Enumerates names of example IMP source code files. */
public class ExamplesProvider implements ArgumentsProvider {
  @Override
  public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
    Iterable<File> files = () -> FileUtils.iterateFiles(new File("examples"), null, true);
    return StreamSupport.stream(files.spliterator(), false).map(Arguments::of);
  }
}