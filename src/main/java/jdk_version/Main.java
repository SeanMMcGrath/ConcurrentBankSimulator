package jdk_version;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.runner.RunnerException;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException, RunnerException {
        org.openjdk.jmh.Main.main(args);

    }


    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void init() {
        // Do nothing

        //jdk_version.BankManager bm = new jdk_version.BankManager();
        //bm.run();
    }

}
