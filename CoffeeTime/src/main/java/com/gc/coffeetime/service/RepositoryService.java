package com.gc.coffeetime.service;

import java.io.File;
import java.util.List;

import com.gc.coffeetime.model.ProjectVersions;

public interface RepositoryService {

	List<File> findAllPomFiles(String startDir);

	void updatePomFile(File pomFile, List<ProjectVersions> latestVersions);

	void updateAllPomFiles(String startDir);
	
	void printBanner();

}
