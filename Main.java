package Konfig2;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Stage 1: Minimal CLI prototype for dependency-graph visualizer.
 *
 * Supported options:
 *   --package, -p  <name>      : name of the analyzed package (required)
 *   --repo,    -r  <url|path>  : repository URL or path to test repo (required)
 *   --test,    -t [true|false] : enable test-repository mode (flag or value, default=false)
 *   --filter,  -f  <substring> : substring to filter package names (optional)
 *   --help,    -h             : show usage
 *
 * On launch the program prints all user-configurable parameters in key=value format.
 * Error handling is provided for all parameters.
 */
public class Main {

    public static void main(String[] args) {
        try {
            Map<String, String> params = parseArgs(args);
            validateParams(params);
            // Print parameters as key=value (requirement 3)
            System.out.println("package=" + params.get("package"));
            System.out.println("repo=" + params.get("repo"));
            System.out.println("testMode=" + params.get("testMode"));
            System.out.println("filter=" + (params.get("filter") == null ? "" : params.get("filter")));
        } catch (IllegalArgumentException ex) {
            System.err.println("Error: " + ex.getMessage());
            System.err.println();
            printUsage();
            System.exit(2);
        } catch (Exception ex) {
            System.err.println("Unexpected error: " + ex.getMessage());
            ex.printStackTrace(System.err);
            System.exit(3);
        }
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> params = new HashMap<>();
        // defaults
        params.put("testMode", "false");

        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            switch (a) {
                case "--help":
                case "-h":
                    printUsage();
                    System.exit(0);
                case "--package":
                case "-p":
                    i = putNextArg(args, i, params, "package");
                    break;
                case "--repo":
                case "-r":
                    i = putNextArg(args, i, params, "repo");
                    break;
                case "--filter":
                case "-f":
                    i = putNextArg(args, i, params, "filter");
                    break;
                case "--test":
                case "-t":
                    // support both "--test" (flag) and "--test true/false"
                    if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                        String val = args[++i];
                        params.put("testMode", String.valueOf(parseBooleanValue(val)));
                    } else {
                        params.put("testMode", "true");
                    }
                    break;
                default:
                    // unknown option or stray argument
                    if (a.startsWith("-")) {
                        throw new IllegalArgumentException("Unknown option: " + a);
                    } else {
                        throw new IllegalArgumentException("Unexpected argument: " + a);
                    }
            }
        }

        return params;
    }

    private static int putNextArg(String[] args, int i, Map<String, String> params, String key) {
        if (i + 1 >= args.length) {
            throw new IllegalArgumentException("Missing value for option: " + args[i]);
        }
        String val = args[++i];
        if (val.startsWith("-")) {
            throw new IllegalArgumentException("Missing value for option: " + args[i - 1]);
        }
        params.put(key, val);
        return i;
    }

    private static boolean parseBooleanValue(String val) {
        if (val.equalsIgnoreCase("true") || val.equalsIgnoreCase("yes") || val.equals("1")) return true;
        if (val.equalsIgnoreCase("false") || val.equalsIgnoreCase("no") || val.equals("0")) return false;
        throw new IllegalArgumentException("Invalid boolean value: " + val + ". Expected true/false.");
    }

    private static void validateParams(Map<String, String> params) {
        // package name required and non-empty
        String pkg = params.get("package");
        if (pkg == null || pkg.trim().isEmpty()) {
            throw new IllegalArgumentException("Package name is required (--package <name>).\n");
        }

        // repo required
        String repo = params.get("repo");
        if (repo == null || repo.trim().isEmpty()) {
            throw new IllegalArgumentException("Repository URL or path is required (--repo <url|path>).\n");
        }

        // testMode must be boolean (already parsed) but check
        String testModeStr = params.get("testMode");
        boolean testMode = Boolean.parseBoolean(testModeStr);

        if (testMode) {
            // then repo should be a path to existing file or directory
            Path p = Path.of(repo);
            if (!Files.exists(p)) {
                throw new IllegalArgumentException("Test mode is enabled but repository path does not exist: " + repo);
            }
        } else {
            // non-test mode -> expect valid URL
            try {
                // allow URLs like http(s) and file: but prefer http(s)
                URL u = new URL(repo);
                String proto = u.getProtocol();
                if (!proto.equals("http") && !proto.equals("https") && !proto.equals("file")) {
                    throw new IllegalArgumentException("Repository URL must use http(s) protocol (or file: for local). Given: " + proto);
                }
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("Invalid repository URL: " + repo + ". If you want to use a local path in test mode, enable --test." );
            }
        }

        // filter may be present; no special validation needed other than length check
        String filter = params.get("filter");
        if (filter != null && filter.length() > 200) {
            throw new IllegalArgumentException("Filter substring is too long (max 200 chars).");
        }
    }

    private static void printUsage() {
        String u = "Usage:\n"
                + "  java -jar depviz-stage1.jar --package <name> --repo <url|path> [--test] [--filter <substring>]\n\n"
                + "Options:\n"
                + "  --package, -p   <name>      Name of the analyzed package (required)\n"
                + "  --repo, -r      <url|path>  Repository URL or path to test repository (required)\n"
                + "  --test, -t                   Enable test-repository mode. Can be used as a flag or with true/false.\n"
                + "  --filter, -f    <substring> Substring to filter package names (optional)\n"
                + "  --help, -h                   Show this help and exit.\n\n"
                + "Examples:\n"
                + "  java -jar depviz-stage1.jar -p libfoo -r https://example.com/ubuntu -f core\n"
                + "  java -jar depviz-stage1.jar -p A -r /path/to/testrepo -t -f X\n";
        System.out.println(u);
    }
}
