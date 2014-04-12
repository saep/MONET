package com.github.monet.worker;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


//TODO Javadoc & Other Operating Systems
//TODO Total amount RAM on Windows Systems

public class SysInformation {

	/**
	 * This method returns the processor
	 * @return The processor of the system
	 * @throws IOException
	 */
	public static String getProcInfo() throws IOException{
		if(System.getProperty("os.name").contains("Linux")){
			FileReader fr = new FileReader("/proc/cpuinfo");
			BufferedReader br = new BufferedReader(fr);
			String line = br.readLine();
			while(line != null){
				if(line.contains("model name")){
					br.close();
					String tmp[] = line.split(":");
					return (tmp[tmp.length-1]);
					}
				line = br.readLine();
				}
			br.close();
			return null;
			}
		if(System.getProperty("os.name").contains("Windows")){
			return System.getenv("PROCESSOR_IDENTIFIER");
		}
		return "BAD_OS";
	}

	/**
	 * This metod returns the amount of ram from the system.
	 * Linux - Total amount
	 * Windows - Amount from the JVM
	 * @return Ram from the system
	 * @throws IOException
	 */
	public static long getMemInfo() throws IOException{
		if(System.getProperty("os.name").contains("Linux")){
			FileReader fr = new FileReader("/proc/meminfo");
			BufferedReader br = new BufferedReader(fr);
			String line = br.readLine();
			while(line != null){
				if(line.contains("MemTotal")){
					br.close();
				//	String tmp[] = line.split(":");
					Pattern p = Pattern.compile("\\d+");
					Matcher m = p.matcher(line);

					if(m.find()){
						return Long.parseLong(m.group());
					}

					//return (tmp[tmp.length-1]);
					}
				line = br.readLine();
				}
			br.close();
			//return null;
		}
		//TODO Buggy on Windows
		if(System.getProperty("os.name").contains("Windows")){
		    long maxMemory = Runtime.getRuntime().maxMemory();
		    if(maxMemory == Long.MAX_VALUE){
		    	return -1;//"no_limit";
		    }
		    else{
		    	return (( (maxMemory/1000) ));
		    }
		}
		//return "BAD_OS";
		return 0;
	}

	/**
	 * This method returns the hostname from the system
	 *
	 * @return The hostname from the system
	 * @throws UnknownHostException
	 */
	public static String getHostName() throws UnknownHostException{
		if( (System.getProperty("os.name").contains("Linux")) ||
				(System.getProperty("os.name").contains("Windows"))){
			InetAddress addr = InetAddress.getLocalHost();
			String localhostname = addr.getHostName();
			return localhostname;
		}
		return "BAD_OS";
	}

	/**regex
	 * This method returns the IP address from the system
	 *
	 * @return IP address from the system
	 * @throws SocketException
	 * @throws UnknownHostException
	 */
	public static String getIpAddress() throws SocketException, UnknownHostException {
		if(System.getProperty("os.name").contains("Linux")){
			Enumeration<NetworkInterface> netInter = NetworkInterface.getNetworkInterfaces();
			while ( netInter.hasMoreElements() ){
				NetworkInterface ni = netInter.nextElement();
				for ( InetAddress iaddress : Collections.list(ni.getInetAddresses()) ){
					if( (iaddress.isLoopbackAddress() == false) &&
							(iaddress.isSiteLocalAddress() == true)){
						return iaddress.getHostAddress();
						}
					}
				}
			return null;
			}
		if(System.getProperty("os.name").contains("Windows")){
			InetAddress ip = InetAddress.getLocalHost();
			return ip.getHostAddress();
		}
		return "BAD_OS";
	}

}
