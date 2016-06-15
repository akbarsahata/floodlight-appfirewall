package net.floodlightcontroller.appfirewall;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Class of URL data which contains String of hostname and list (HashSet) of URL objects (recursion intended)
 * @author Akbar Sahata S 	
 * @since 2016-05-28
 * @version 0.1
 */
public class URL {
	private String hostname;
	private String subdir;
	private int listIndex;
	private int level;
	private ArrayList<URL> subdirList;
	
	/**
	 * Construct method without URL
	 * @param level leveling sub-directory, for first time initiated should be -1
	 */
	public URL(int level){
		this.subdirList = new ArrayList<URL>();
		this.listIndex = 0;
		this.level = level + 1;
	}
	
	/**
	 * Construct method with URL. Inserts subdir if available
	 * @param url full url
	 */
	
	public URL(String url, int level){
		this.subdirList = new ArrayList<URL>();
		this.listIndex = 0;
		this.level = level + 1;
		
		this.hostname = findHostname(url); 
		this.subdir = findSubdir(url);
		
		if (this.subdir != null){
			setSubdir(this.subdir);
		}
	}
	
	/**
	 * To clean up unnecessary text and to find first directory in url (just before forward slash)
	 * @param url full text url
	 * @return first directory in url
	 */
	private String findHostname(String url){
		url = url.trim();
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
		url = url.trim();
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
	
	/**
	 * To check whether a first level sub-directory is listed
	 * @param subdir the rest of text after hostname
	 * @return true if found duplicate and otherwise
	 */
	private boolean checkDuplicateSubdir(String subdir){
		if (this.subdirList.size() == 0){
			return false;
		}
		if (subdir.contains("/")){
			subdir = findHostname(subdir);
		}
		
		Iterator<URL> subdirIterator = this.subdirList.iterator();
		this.listIndex = 0;
		while (subdirIterator.hasNext()){
			URL temp = (URL) subdirIterator.next();
			if (temp.getHostname().equals(subdir)){
				return true;
			}
			this.listIndex++;
		}
		
		return false;
	}
	
	/**
	 * to check whether any sub-directory is listed
	 * @return true if there is at least one data and otherwise
	 */
	public boolean hasSubdir(){
		if (this.subdirList.size() > 0){
			return true;
		} else return false;
	}
	
	/**
	 * hostname field setter
	 * @param hostname
	 */
	public void setHostname(String hostname){
		this.hostname = hostname;
	}
	
	/**
	 * To add another sub-directory in list, or sub-sub-directory (recursion intended)
	 * if first level sub-directory found duplicate, sub-sub-directory will be set
	 * otherwise new sub-directory will be added
	 * @param url full text url
	 * @return false if sub-directory is already listed and sub-sub-directory null, otherwise based on ArrayList.add() 
	 */
	public boolean setSubdir(String url){
		String dir = findHostname(url);
		String subdir = findSubdir(url);
		if (checkDuplicateSubdir(dir)) {
			if (subdir == null ){
				return false;
			} else return this.subdirList.get(listIndex).setSubdir(subdir);
		} else {
			return this.subdirList.add(new URL(url, this.level));
		}
	}
	
	/**
	 * hostname field getter
	 * @return hostname field
	 */
	public String getHostname(){
		return this.hostname;
	}
	
	/**
	 * subdirlist field getter
	 * @return arraylist of subdirlist
	 */
	public ArrayList getSubdirList(){
		return this.subdirList;
	}
	
	/**
	 * to get size of one node with all trees
	 * @return number of trees in one node
	 */
	public int getSize(){
		int size = this.subdirList.size();
		if (size == 0){
			return 0;
		}
		Iterator<URL> it = this.subdirList.iterator();
		while (it.hasNext()){
			URL temp = it.next();
			if (temp.hasSubdir()){
				size += temp.getSize();
			}
		}
		return size;
	}
	
	/**
	 * to find the size of subdirlist
	 * @return subdirList.size()
	 */
	public int getSubdirSize(){
		return this.subdirList.size();
	}
	
	/**
	 * n sukses dihapus sejumlah n
	 * 0 tidak ditemukan
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
		Iterator<URL> it = this.subdirList.iterator();
		while(it.hasNext()){
			URL temp = it.next();
			if (temp.getHostname().equals(hostname)){
				if (temp.hasSubdir()){
					if (subdir == null){
						return -1;
					} else if (subdir.equals("*")) {
						int size = this.subdirList.get(index).getSubdirList().size();
						this.subdirList.get(index).getSubdirList().clear();
						return size;
					} else {
						return this.subdirList.get(index).deleteURL(subdir);
					}
				} else {
					if (subdir == null){
						this.subdirList.remove(index);
						return 3;
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
		String hostname = findHostname(url);
		String subdir = findSubdir(url);
		
		Iterator<URL> it = this.subdirList.iterator();
		while(it.hasNext()){
			URL temp = it.next();
			if (temp.getHostname().equals(hostname)){
				if (temp.hasSubdir() && subdir != null){
					return temp.findURL(subdir);
				} else if (!temp.hasSubdir() && subdir == null){
					return true;
				} else {
					return false;
				}
			}
		}
		return false;
	}
	
	/**
	 * to give proper whitespace to make it seen leveled
	 * @return whitespaces
	 */
	public String whiteSpace(){
		String whiteSpace = "";
		for (int index = 0; index <= this.level; index++){
			whiteSpace += "  ";
		}
		return whiteSpace;
	}
	
	@Override
	public String toString(){
		String output = "";
		Iterator<URL> iteratorSubdir = this.subdirList.iterator();
		while(iteratorSubdir.hasNext()){
			URL temp = iteratorSubdir.next();
			output += "\n"+whiteSpace()+"/"+temp.getHostname()+temp.toString();
		}
		return output;
	}
}
