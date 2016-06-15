package net.floodlightcontroller.appfirewall;

import org.restlet.resource.ServerResource;
import org.restlet.resource.Get;

import net.floodlightcontroller.appfirewall.AppFirewall;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.slf4j.Logger;

public class AppFirewallResourceAdd extends ServerResource {
    protected static Logger logger;
    
    @Get("json")
    public String add(String url){
    	boolean output;
    	synchronized(AppFirewall.dh){
    		output = AppFirewall.dh.addURL(url);
    		appendURL(url);
    	}
    	if (output){
    		return "{\"status\" : \"URL added\"}";
    	} return "{\"status\" : \"URL add failed, probably already exist\"}";
    }
    
    private void appendURL(String url){
    	BufferedWriter bw; 
    	try {
    		bw = new BufferedWriter(new FileWriter("/home/akbarsahata/Floodlight/floodlight/src/main/java/net/floodlightcontroller/appfirewall/nsfw.txt", true));
    		bw.write(url);
    		bw.newLine();
    		bw.flush();
    		bw.close();
    	} catch (IOException e) {
    		logger.info(e.getMessage());
    	}
    }
}
