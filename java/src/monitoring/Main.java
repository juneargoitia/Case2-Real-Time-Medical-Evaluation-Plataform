package monitoring;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class Main {
    public static void main(String[] args) throws IOException {
        Path out = Paths.get(args.length > 0 ? args[0] : "results");
        Files.createDirectories(out);

        try (PrintWriter env = new PrintWriter(Files.newBufferedWriter(out.resolve("environment.txt")))) {
            env.println("java=" + System.getProperty("java.version"));
            env.println("os=" + System.getProperty("os.name"));
            env.println("cores=" + Runtime.getRuntime().availableProcessors());
        }

        FailureSimulation.run(out);
        Benchmark.run(out);
    }
}
