package com.gc.coffeetime;

import java.io.File;
import java.util.List;

import com.gc.coffeetime.model.PomInfo;
import com.gc.coffeetime.service.RepositoryService;
import com.gc.coffeetime.service.RepositoryServiceImpl;

public class CoffeeTimeMain {
	
	static RepositoryService rs = new RepositoryServiceImpl();
	
    public static void main(String[] args) {
    	
    	List<File> pomFiles = rs.findAllPomFiles("C://rgi/projects/uma");
    	PomInfo pomInfo;
    	
		for(File pomFile : pomFiles) {
    		pomInfo = rs.readPomFile(pomFile);
    		System.out.println(pomInfo.toString());
    	}        
    }
}
