package demo;
 
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
 
/**
 * The entry point of the demo: {@code Ex1 file.nrrd file.csv file.mp4}.
 *
 * <p>It only checks the arguments, passes the three files to {@link DemoRunner} and turns the
 * result into an exit code. Exit codes: 0 success, 1 unexpected error, 2 wrong usage or an
 * invalid file name, 3 problem with the NRRD file, 4 problem with the CSV file,
 * 5 another file problem. The codes 3 to 5 come from {@link DemoRunner}.</p>
 */
public final class Ex1 {
 
    static final int EXIT_UNEXPECTED = 1;
    static final int EXIT_USAGE = 2;
 
    private Ex1() {
    }
 
    public static void main(String[] args) {
        System.exit(execute(args));
    }
 
    /** Does everything {@code main} does, but returns the exit code instead of ending the program. */
    static int execute(String[] args) {
        if (args.length != 3) {
            System.err.println("Usage: Ex1 file.nrrd file.csv file.mp4");
            return EXIT_USAGE;
        }
        try {
            Path nrrdFilePath = Path.of(args[0]);
            Path csvFilePath = Path.of(args[1]);
            Path mp4FilePath = Path.of(args[2]);
            return new DemoRunner().run(nrrdFilePath, csvFilePath, mp4FilePath);
        } catch (InvalidPathException e) {
            System.err.println("Invalid file name: " + e.getMessage());
            return EXIT_USAGE;
        } catch (RuntimeException e) {
            System.err.println("Unexpected error: " + e);
            return EXIT_UNEXPECTED;
        }
    }
}