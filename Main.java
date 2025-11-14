package Konfig2;

import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {

    public static void main(String[] args) {
        Config cfg = parseArgs(args);
        if (cfg == null) return;

        System.out.println("package=" + cfg.packageName);
        System.out.println("repo=" + cfg.repo);
        System.out.println("testMode=" + cfg.test);
        System.out.println("filter=" + cfg.filter);

        try {
            List<AptPackage> packages = cfg.test
                    ? loadLocal(cfg.repo)
                    : loadRemote(cfg.repo);

            Map<String, AptPackage> map = new HashMap<>();
            for (AptPackage p : packages) map.put(p.name, p);

            System.out.println();
            System.out.println("=== Dependency Tree ===");

            printTree(cfg.packageName, map, new HashSet<>(), 0, cfg.filter);

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    public static class Config {
        String packageName;
        String repo;
        boolean test = false;
        String filter = "";
    }

    private static Config parseArgs(String[] args) {
        Config c = new Config();
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--package": c.packageName = args[++i]; break;
                case "--repo": c.repo = args[++i]; break;
                case "--test": c.test = true; break;
                case "--filter": c.filter = args[++i]; break;
                case "--help":
                    System.out.println("Usage: java Main --package <name> --repo <url or file> [--test] [--filter x]");
                    return null;
            }
        }
        if (c.packageName == null || c.repo == null) {
            System.err.println("Missing required params");
            return null;
        }
        return c;
    }

    private static List<AptPackage> loadLocal(String path) throws IOException {
        return parsePackages(new String(Files.readAllBytes(new File(path).toPath())));
    }

    private static List<AptPackage> loadRemote(String repoUrl) throws IOException {
        String url = repoUrl.endsWith("Packages") ? repoUrl : repoUrl + "/Packages";
        String data = new String(new URL(url).openStream().readAllBytes());
        return parsePackages(data);
    }

    public static class AptPackage {
        String name;
        List<String> depends = new ArrayList<>();
    }

    private static List<AptPackage> parsePackages(String text) {
        List<AptPackage> list = new ArrayList<>();
        AptPackage current = null;
        String dependsBuf = null;

        for (String line : text.split("\n")) {
            line = line.trim();
            if (line.isEmpty()) {
                if (current != null) {
                    if (dependsBuf != null) {
                        current.depends = parseDependencies(dependsBuf);
                    }
                    list.add(current);
                }
                current = null;
                dependsBuf = null;
                continue;
            }

            if (line.startsWith("Package:")) {
                current = new AptPackage();
                current.name = line.substring(8).trim();
            } else if (line.startsWith("Depends:")) {
                dependsBuf = line.substring(8).trim();
            } else if (line.startsWith(" ") && dependsBuf != null) {
                dependsBuf += " " + line.trim();
            }
        }

        if (current != null) {
            if (dependsBuf != null) {
                current.depends = parseDependencies(dependsBuf);
            }
            list.add(current);
        }
        return list;
    }

    private static List<String> parseDependencies(String line) {
        List<String> out = new ArrayList<>();
        line = line.replace("\n", " ").replaceAll("\\s+", " ").trim();

        if (line.isEmpty()) return out;

        for (String part : line.split(",")) {
            part = part.trim();
            if (part.isEmpty()) continue;

            part = part.replaceAll("\\(.*?\\)", "").trim();

            if (part.contains("|")) {
                for (String alt : part.split("\\|")) {
                    alt = alt.trim();
                    if (!alt.isEmpty() && !out.contains(alt)) {
                        out.add(alt);
                    }
                }
            } else if (!part.isEmpty() && !out.contains(part)) {
                out.add(part);
            }
        }
        return out;
    }

    private static void printTree(String pkg, Map<String, AptPackage> map, Set<String> visited, int depth, String filter) {
        if (!filter.isEmpty() && !pkg.contains(filter)) {
            return;
        }

        printIndent(depth);
        System.out.println(pkg);

        if (visited.contains(pkg)) {
            printIndent(depth + 1);
            System.out.println("(cyclic dependency)");
            return;
        }

        AptPackage p = map.get(pkg);
        if (p == null) {
            printIndent(depth + 1);
            System.out.println("(package not found)");
            return;
        }

        visited.add(pkg);

        for (String dep : p.depends) {
            printTree(dep, map, new HashSet<>(visited), depth + 1, filter);
        }

        visited.remove(pkg);
    }

    private static void printIndent(int depth) {
        for (int i = 0; i < depth; i++) {
            if (i == depth - 1) {
                System.out.print("└── ");
            } else {
                System.out.print("    ");
            }
        }
    }
}