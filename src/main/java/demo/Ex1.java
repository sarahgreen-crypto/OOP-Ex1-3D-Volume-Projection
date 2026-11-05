package demo;

import java.nio.file.Path;

public final class Ex1 {

    private Ex1(){}

    public static void main (String[] args) {
        if (args.length != 3) {
            System.err.println("Usage: Ex1 file.nrrd file.csv file.mp4");
            System.exit(2);
        }

         int exitCode = new DemoRunner().run(
                Path.of(args[0]), Path.of(args[1]), Path.of(args[2]));
        System.exit(exitCode);
    }
}
