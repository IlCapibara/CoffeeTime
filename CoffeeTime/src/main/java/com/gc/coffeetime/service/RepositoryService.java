package com.gc.coffeetime.service;

import java.io.File;
import java.util.List;

import com.gc.coffeetime.model.PomInfo;

public interface RepositoryService {

	PomInfo readPomFile(File pomFile);

	List<File> findAllPomFiles(String startDir);

}
