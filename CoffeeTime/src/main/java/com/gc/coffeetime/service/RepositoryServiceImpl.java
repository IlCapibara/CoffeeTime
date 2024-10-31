package com.gc.coffeetime.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gc.coffeetime.model.PomInfo;
import com.gc.coffeetime.model.ProjectVersions;

public class RepositoryServiceImpl implements RepositoryService {

	@Override
    public PomInfo readPomFile(File pomFile) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(pomFile);
            doc.getDocumentElement().normalize();

            String groupId = getTagValue("groupId", doc);
            String artifactId = getTagValue("artifactId", doc);
            String version = getTagValue("version", doc);

            return new PomInfo(groupId, artifactId, version);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String getTagValue(String tag, Document doc) {
        Element element = (Element) doc.getElementsByTagName(tag).item(0);
        return element != null ? element.getTextContent() : "";
    }
    
    @Override
    public List<File> findAllPomFiles(String startDir) {
        List<File> pomFiles = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(Paths.get(startDir), FileVisitOption.FOLLOW_LINKS)) {
            pomFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equals("pom.xml"))
                    .map(Path::toFile)                    
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return pomFiles;
    }
    
    public void findAllPomFilesInRepository(String directory) {
    	List<File> pomFiles = findAllPomFiles("C:/Users/gianluigi.colameo/.m2/repository");
    	List<ProjectVersions> artifactVersionCouple = new ArrayList<>();
    	PomInfo pomInfo;
    	
		for(File pomFile : pomFiles) {
    		pomInfo = readPomFile(pomFile);
    		
    		artifactVersionCouple.add(null);
    		
    	}
    }
}
