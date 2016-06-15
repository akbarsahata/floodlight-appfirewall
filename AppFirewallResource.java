package net.floodlightcontroller.appfirewall;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.restlet.resource.ServerResource;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Delete;

import net.floodlightcontroller.appfirewall.AppFirewall;

import org.slf4j.Logger;

public class AppFirewallResource extends ServerResource {
    protected static Logger logger;
    
    @Get("json")
    public String listAll(){
    	if(AppFirewall.dh.toString() == null){
    		return "System is not ready yet";
    	} else {
    		String output = "{\"URL\" : \""+AppFirewall.dh.toString()+"\"}";
    		return output;
    	}
    }
    
    @Post("json")
    public String add(String url){
    	logger.info(url);
    	if (AppFirewall.dh.addURL(url)){
    		return "{\"status\" : \"URL added\"}";
    	} return "{\"status\" : \"URL add failed\"}";
    }
    
    @Delete
    public String delete(String url){
    	int statusCode;
    	boolean delete;
    	
    	synchronized(AppFirewall.dh){
    		statusCode = AppFirewall.dh.deleteURL(url);
    		delete = deleteURL(url);
    	}
    	
    	if (statusCode == 0 || statusCode == -2){
    		return "{\"status\" : \"URL is not found\"}";
    	} else if (statusCode == -1){
    		return "{\"status\" : \"URL cannot be deleted, sub-directory exist\"}";
    	} else if (statusCode >= 1 && delete){
    		return "{\"status\" : \""+url+" deleted\"}";
    	} else {
    		return "{\"status\" : \"Error\"}";
    	}
    	
    }
    
    private boolean deleteURL(String url){
    	File inputFile = new File("/home/akbarsahata/Floodlight/floodlight/src/main/java/net/floodlightcontroller/appfirewall/nsfw.txt");
    	File tempFile = new File("/home/akbarsahata/Floodlight/floodlight/src/main/java/net/floodlightcontroller/appfirewall/temp_nsfw.txt");
    	
    	try {
    		BufferedReader reader = new BufferedReader(new FileReader(inputFile));
        	BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));
        	
        	String lineToRemove = url;
        	String currentLine;
        	
        	if (!url.contains("*")){
	        	while((currentLine = reader.readLine()) != null) {
	        	    // trim newline when comparing with lineToRemove
	        	    String trimmedLine = currentLine.trim();
	        	    if(trimmedLine.equals(lineToRemove) || trimmedLine.equals("http://"+lineToRemove) || trimmedLine.equals("https://"+lineToRemove)) continue;
	        	    writer.write(currentLine + System.getProperty("line.separator"));
	        	}
        	} else {
        		while((currentLine = reader.readLine()) != null) {
	        	    // trim newline when comparing with lineToRemove
	        	    String trimmedLine = currentLine.trim();
	        	    if(trimmedLine.contains(url.substring(0, url.indexOf("*") - 1))) continue;
	        	    writer.write(currentLine + System.getProperty("line.separator"));
	        	}
        		writer.write("http://"+url.substring(0, url.indexOf("*") - 1) + System.getProperty("line.separator"));
        		synchronized(AppFirewall.dh){
        			AppFirewall.dh.addURL(url.substring(0, url.indexOf("*") - 1));
        		}
        	}
        	writer.close(); 
        	reader.close(); 
        } catch (IOException e) {
			logger.info(e.getMessage());
		}
    	
    	boolean successful = tempFile.renameTo(inputFile);

    	return successful;
    }
}
