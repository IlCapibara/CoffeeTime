package com.gc.coffeetime.service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.gc.coffeetime.model.ProjectVersions;
import com.github.lalyos.jfiglet.FigletFont;

public class RepositoryServiceImpl implements RepositoryService {

    // Constructor
    public RepositoryServiceImpl() {
        printBanner();
    }

    // Print ASCII banner for the tool
    @Override
    public void printBanner() {
        try {
            String asciiArt = FigletFont.convertOneLine("CoffeeTime");
            System.out.println(asciiArt);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("==========================================================");
        System.out.println("This program scans all pom.xml files in the selected      ");
        System.out.println("folder, finds the latest local versions for dependencies, ");
        System.out.println("and updates the pom.xml files accordingly.                ");
        System.out.println("==========================================================\n");
    }

    // Find all pom.xml files in the given directory, excluding the "target" directory
    @Override
    public List<File> findAllPomFiles(String startDir) {
        List<File> pomFiles = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(Paths.get(startDir), FileVisitOption.FOLLOW_LINKS)) {
            pomFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> !path.toString().contains(File.separator + "target" + File.separator))
                    .filter(path -> path.getFileName().toString().equals("pom.xml"))
                    .map(Path::toFile)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return pomFiles;
    }

    // Retrieve main POM info (groupId, artifactId, version)
    public ProjectVersions readPomFileInfo(File pomFile) {
        try {
            Document doc = Jsoup.parse(pomFile, "UTF-8", "", org.jsoup.parser.Parser.xmlParser());

            // Remove <parent> to avoid unwanted inheritance in main POM info
            Element parentElement = doc.selectFirst("parent");
            if (parentElement != null) {
                parentElement.remove();
            }

            String groupId = doc.selectFirst("groupId").text();
            String artifactId = doc.selectFirst("artifactId").text();

            // Obtain version if it's defined outside of dependencies
            Element versionElement = doc.selectFirst("> version");
            String version = null;

            if (versionElement != null) {
                version = resolveVersion(versionElement.text(), doc);
            } else {
                // Check parent version if local version is absent
                Element parentVersionElement = doc.selectFirst("parent > version");
                if (parentVersionElement != null) {
                    version = resolveVersion(parentVersionElement.text(), doc);
                }
            }

            System.out.println("Parsed values - GroupId: " + groupId + ", ArtifactId: " + artifactId + ", Version: " + version);
            return new ProjectVersions(groupId, artifactId, version);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Retrieve dependencies information from POM
    public List<ProjectVersions> readPomDependencies(File pomFile) {
        List<ProjectVersions> dependencies = new ArrayList<>();
        try {
            Document doc = Jsoup.parse(pomFile, "UTF-8", "", org.jsoup.parser.Parser.xmlParser());

            for (Element dependency : doc.select("dependencies > dependency")) {
                String groupId = dependency.selectFirst("groupId").text();
                String artifactId = dependency.selectFirst("artifactId").text();
                Element versionElement = dependency.selectFirst("version");

                String version = null;
                if (versionElement != null) {
                    version = resolveVersion(versionElement.text(), doc);
                }

                dependencies.add(new ProjectVersions(groupId, artifactId, version));
                System.out.println("Dependency - GroupId: " + groupId + ", ArtifactId: " + artifactId + ", Version: " + version);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dependencies;
    }

    // Resolve version by checking if it's a property or a direct version value
    private String resolveVersion(String version, Document doc) {
        if (version.startsWith("${") && version.endsWith("}")) {
            String propertyName = version.substring(2, version.length() - 1);
            Element propertyElement = doc.selectFirst("properties > " + propertyName);
            if (propertyElement != null) {
                return propertyElement.text();
            } else {
                System.out.println("Warning: Property " + propertyName + " not found in <properties>.");
                return null;
            }
        }
        return version;
    }

    // Update POM dependencies based on the latest versions provided
    public void updatePomFile(File pomFile, List<ProjectVersions> latestVersions) {
        try {
            Document doc = Jsoup.parse(pomFile, "UTF-8", "", org.jsoup.parser.Parser.xmlParser());

            // Get the parent version if defined
            Element parentElement = doc.selectFirst("parent > version");
            String parentVersion = parentElement != null ? parentElement.text() : null;

            for (Element dependency : doc.select("dependencies > dependency")) {
                String groupId = dependency.selectFirst("groupId").text();
                String artifactId = dependency.selectFirst("artifactId").text();
                Element versionElement = dependency.selectFirst("version");

                // Skip updating if version is inherited from the parent or undefined
                if (versionElement == null && parentVersion != null) {
                    System.out.println("Dependency " + groupId + ":" + artifactId + " inherits version from parent (" + parentVersion + "), skipping update.");
                    continue;
                }

                if (versionElement != null) {
                    String currentVersion = versionElement.text();
                    if (currentVersion.startsWith("${") && currentVersion.endsWith("}")) {
                        String propertyName = currentVersion.substring(2, currentVersion.length() - 1);
                        if (!isLocalProperty(doc, propertyName)) {
                            System.out.println("Dependency " + groupId + ":" + artifactId + " uses parent version property (" + propertyName + "), skipping update.");
                            continue;
                        }
                    }

                    // Update only if current version differs from the latest version
                    String latestVersion = getLatestVersion(groupId, artifactId, latestVersions);
                    if (latestVersion != null && !latestVersion.equals(currentVersion)) {
                        versionElement.text(latestVersion);
                        System.out.println("Updated " + groupId + ":" + artifactId + " from version " + currentVersion + " to " + latestVersion);
                    }
                }
            }

            saveDocumentToFile(doc, pomFile);

        } catch (Exception e) {
            System.err.println("Error processing file: " + pomFile.getAbsolutePath());
            e.printStackTrace();
        }
    }

    // Check if a property is defined locally within the current POM document
    private boolean isLocalProperty(Document doc, String propertyName) {
        Element propertiesElement = doc.selectFirst("properties");
        return propertiesElement != null && propertiesElement.selectFirst(propertyName) != null;
    }

    // Get the latest version for a dependency from the list of known versions
    private String getLatestVersion(String groupId, String artifactId, List<ProjectVersions> latestVersions) {
        for (ProjectVersions version : latestVersions) {
            if (version.getGroupId().equals(groupId) && version.getArtifactId().equals(artifactId)) {
                return version.getVersion();
            }
        }
        return null;
    }

    // Save the updated XML document back to the file
    private void saveDocumentToFile(Document doc, File file) {
        try {
            Files.write(file.toPath(), doc.outerHtml().getBytes(StandardCharsets.UTF_8));
            System.out.println("File saved successfully: " + file.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error saving file: " + file.getAbsolutePath());
            e.printStackTrace();
        }
    }

    // Update all POM files in the specified directory
    public void updateAllPomFiles(String startDir) {
        List<File> pomFiles = findAllPomFiles(startDir);
        List<ProjectVersions> latestVersions = getLocalVersions(pomFiles);

        for (File pomFile : pomFiles) {
            System.out.println("Updating " + pomFile.getAbsolutePath());
            updatePomFile(pomFile, latestVersions);
        }

        System.out.println("All pom.xml files have been updated.");
    }

    // Get versions for all local POM files
    public List<ProjectVersions> getLocalVersions(List<File> pomFiles) {
        List<ProjectVersions> pomListWithVersion = new ArrayList<>();
        for (File pomFile : pomFiles) {
            ProjectVersions info = readPomFileInfo(pomFile);
            if (info != null) {
                pomListWithVersion.add(info);
            }
        }
        return pomListWithVersion;
    }
}
