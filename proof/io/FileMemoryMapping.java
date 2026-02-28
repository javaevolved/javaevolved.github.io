///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.lang.foreign.*;

/// Proof: file-memory-mapping
/// Source: content/io/file-memory-mapping.yaml
void main() throws Exception {
    var path = Path.of(System.getProperty("java.io.tmpdir"), "proof-mmap.dat");
    Files.write(path, new byte[1024]);
    FileChannel channel =
        FileChannel.open(path,
            StandardOpenOption.READ,
            StandardOpenOption.WRITE);
    try (Arena arena = Arena.ofShared()) {
        MemorySegment segment =
            channel.map(
                FileChannel.MapMode.READ_WRITE,
                0, channel.size(), arena);
        // No size limit
        // ...
    } // Deterministic cleanup
}
