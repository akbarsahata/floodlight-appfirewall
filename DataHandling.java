package net.floodlightcontroller.appfirewall;

import java.util.ArrayList;
import java.util.Iterator;

import net.floodlightcontroller.appfirewall.URL;

/**
 * Class for Handling Data of URL, File.IO, and threads 
 * @author Akbar Sahata S
 * @since 2016-05-28
 * @version 0.1
 */
public class DataHandling {
	private ArrayList<URL> url;
	private int listIndex;
	
	/**
	 * Constructor method which initiates ArrayList of URL
	 */
	public DataHandling(){
		this.url = new ArrayList<URL>();
		this.listIndex = 0;
	}
	
	/**
	 * To add new URL Object to ArrayList
	 * @param url full text url
	 * @return false if found duplicate and sub-directory is null. otherwise based on URL.setSubdir() or ArrayList.add()
	 */
	public boolean addURL(String url){
		if (url.charAt(url.length()-1) == '/'){
			url = url.substring(0, url.length()-1);
		}
		String hostname = findHostname(url);
		String subdir = findSubdir(url);
		
		if (checkDuplicateHostname(hostname)){
			if (subdir != null) {
				return this.url.get(listIndex).setSubdir(subdir);
			} else{
				return false;
			}
		} else {
			return this.url.add(new URL(url, -1));
		}
	}
	
	/**
	 * To check whether the hostname is listed
	 * @param url full text url
	 * @return true if found duplicate and otherwise
	 */
	private boolean checkDuplicateHostname(String url){
		if (this.url.size() == 0){
			return false;
		}
		
		Iterator<URL> urlIterator = this.url.iterator();
		this.listIndex = 0;
		while (urlIterator.hasNext()){
			URL temp = urlIterator.next();
			if (temp.getHostname().equals(url)){
				return true;
			}
			this.listIndex++;
		}
		return false;
	}
	
	/**
	 * To clean up unnecessary text and to find first directory in url (just before forward slash)
	 * @param url full text url
	 * @return first directory in url
	 */
	private String findHostname(String url){
		if (url.contains("http://")){
			url = url.substring(7);
		} else if (url.contains("https://")){
			url = url.substring(8);
		}
		if (url.contains("www.")){
			url = url.substring(4);
		}
		if (url.contains("/")){
			String[] temp = url.split("/");
			url = temp[0];
		}
		return url;
	}
	
	/**
	 * To clean out unnecessary text and to find the rest of text after url
	 * @param url full text url
	 * @return rest of text after url
	 */
	private String findSubdir(String url){
		if (url.contains("http://")){
			url = url.substring(7);
		} else if (url.contains("https://")){
			url = url.substring(8);
		}
		if (url.contains("www.")){
			url = url.substring(4);
		}
		if (url.contains("/")){
			String[] temp = url.split("/");
			url = url.substring(temp[0].length()+1);
		} else {
			url = null;
		}
		return url;
	}
	
	public ArrayList<URL> getURL(){
		return this.url;
	}
	
	/**
	 * n sukses dihapus sejumlah n
	 * 0 tidak ditemukan
	 * 1 menghapus URL tanpa sub-directory
	 * -1 tidak bisa dihapus karena ada subdir
	 * -2 tidak ditemukan
	 * @param url
	 * @return
	 */
	public int deleteURL(String url){
		if (url.charAt(url.length()-1) == '/'){
			url = url.substring(0, url.length()-1);
		}
		String hostname = findHostname(url);
		String subdir = findSubdir(url);
		int index = 0;
		Iterator<URL> it = this.url.iterator();
		while(it.hasNext()){
			URL temp = it.next();
			if (temp.getHostname().equals(hostname)){
				if (temp.hasSubdir()){
					if (subdir == null){
						return -1;
					} else if (subdir.equals("*")) {
						int size = this.url.get(index).getSize();
						this.url.remove(index);
						return size;
					} else {
						return this.url.get(index).deleteURL(subdir);
					}
				} else {
					if (subdir == null){
						this.url.remove(index);
						return 1;
					} else {
						return -2;
					}
				}
			}
			index++;
		}
		return 0;
	}
	
	/**
	 * to find URL 
	 * @param url
	 * @return true if found or otherwise
	 */
	public boolean findURL(String url){
		if (url.charAt(url.length()-1) == '/'){
			url = url.substring(0, url.length()-1);
		}
		String hostname = findHostname(url);
		String subdir = findSubdir(url);
		
		Iterator<URL> it = this.url.iterator();
		while(it.hasNext()){
			URL temp = it.next();
			if (temp.getHostname().equals(hostname)){
				if (temp.hasSubdir() && subdir != null){
					return temp.findURL(subdir);
				} else if (!temp.hasSubdir() && subdir == null){
					return true;
				} else if (!temp.hasSubdir() && subdir != null){
					return true;
				} else {
					return false;
				}
			} 
		}
		return false;
	}
	
	/**
	 * to get size of one node with all trees
	 * @return number of trees in one node
	 */
	public int getSize(){
		int size = this.url.size();
		if (size == 0){
			return 0;
		}
		Iterator<URL> it = this.url.iterator();
		while (it.hasNext()){
			URL temp = it.next();
			if (temp.hasSubdir()){
				size += temp.getSize();
			}
		}
		return size;
	}
	
	@Override
	public String toString(){
		String output = "";
		Iterator<URL> iterator = this.url.iterator();
		while(iterator.hasNext()){
			URL temp = iterator.next();
			output += "\n"+temp.getHostname();
			if (temp.getSubdirSize() > 0){
				for (int index = 0; index < temp.getSubdirSize(); index++){
					URL subdirTemp = (URL) temp.getSubdirList().get(index);
					output += "\n  /"+subdirTemp.getHostname()+subdirTemp.toString();
				}
			} 
		}
		return output;
	}
}
