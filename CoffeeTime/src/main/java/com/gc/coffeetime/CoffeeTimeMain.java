package com.gc.coffeetime;

import com.gc.coffeetime.service.RepositoryService;
import com.gc.coffeetime.service.RepositoryServiceImpl;

public class CoffeeTimeMain {
	
	static RepositoryService rs = new RepositoryServiceImpl();
	
    public static void main(String[] args) {
    	
    	rs.printBanner();    	
    	//We can take a coffee now
    	rs.updateAllPomFiles("C:/Users/gianluigi.colameo/OneDrive - RGI SpA/Desktop/prove");
    	
    }
}
