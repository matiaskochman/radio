package radio;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

public class Main {

	public static void main(String[] args){
		for (int i = 2; i <= 30; i++) {
			LocalDate date = LocalDate.of(2016, Month.NOVEMBER, i);
			
			downloadBastaDeTodoForSpecificDate(date);
			
		}		
	}
	public static void downloadBastaDeTodoForSpecificDate(LocalDate date){
		try {
			// tengo que poner 17 horas porque son 14 horas + 3 horas de diferencia con gmt
			LocalDateTime start = LocalDateTime.of(date.getYear(), date.getMonth(), date.getDayOfMonth(),17,0);
			
			ZonedDateTime zoneStart = start.atZone(ZoneId.of("GMT"));
			Long fromTimeInMillis = zoneStart.toInstant().toEpochMilli();
			System.out.println(fromTimeInMillis);
			
			// tengo que poner 17 horas porque son 14 horas + 3 horas de diferencia con gmt
			LocalDateTime finish = LocalDateTime.of(date.getYear(), date.getMonth(), date.getDayOfMonth(),21,0);
			
			ZonedDateTime zoneFinish = finish.atZone(ZoneId.of("GMT"));
			Long toTimeInMillis = zoneFinish.toInstant().toEpochMilli();		
			
			System.out.println(toTimeInMillis);
			String startmilis = fromTimeInMillis.toString();
			
			// tomo el chunknumber
			Integer chunkNumber = new Integer(startmilis.substring(0,6));
			
			boolean finished = false;
			
			List<String> urlList = new ArrayList<String>();
			List<Integer> timeList = new ArrayList<Integer>();
			while(!finished){
				String url = "http://chunkserver.radiocut.fm//server/get_chunks/metro951/"+ chunkNumber+"/";
				
				System.out.println(url);
				String data = getData(url);
				
				
				InputStream stream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));

	            JsonReader reader = Json.createReader(stream);
	            JsonObject empObj = reader.readObject();			
	            JsonObject query = empObj.getJsonObject(chunkNumber.toString());
	            JsonArray chunks = query.getJsonArray("chunks");
	            
	            
	            Integer intTimeInSeconds = null;
	            for (int i = 0; i < chunks.size(); i++) {
					JsonObject o = chunks.getJsonObject(i);
					String filename = o.getJsonString("filename").toString().replaceAll("\"", "");
					String s = "http://cdn-gs.radiocut.com.ar/metro951/"+chunkNumber.toString().substring(0,3)+"/"+chunkNumber.toString().substring(3,6)+"/"+filename;
					String time = filename.substring(0, 10);
					intTimeInSeconds = new Integer(time);
					
					if((fromTimeInMillis/1000)<intTimeInSeconds && (toTimeInMillis/1000)>intTimeInSeconds){
						
						//agarrar uno antes que se cumpla la consigna
						
						urlList.add(s);
						timeList.add(intTimeInSeconds);
					}
					if((toTimeInMillis/1000)<intTimeInSeconds){
						finished=true;
						break;
					}else{
						if(i == chunks.size()-1){
							chunkNumber++;
						}
					}
					
				}
				
			}
			
			
			List<String> fileNameList = new LinkedList<String>();
            for (String urlString : urlList) {
            	Pattern pattern = Pattern.compile("1(\\d|\\.|\\-)*.mp3");
            	Matcher matcher = pattern.matcher(urlString);
            	String filename = null;
            	if(matcher.find()){
            		filename = urlString.substring(matcher.start(), matcher.end());
            		download(urlString,filename);
            		fileNameList.add(filename);
            	}
			}
            
            executeCommands(fileNameList,date);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
	public static String getData(String address) throws Exception {
		
		//Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("172.18.3.100", 8080));
		//HttpURLConnection conn = (HttpURLConnection) new URL(address).openConnection(proxy);
		HttpURLConnection conn = (HttpURLConnection) new URL(address).openConnection();
		conn.setConnectTimeout(30000);
		conn.setReadTimeout(120000);
		
	    URL url = new URL(address);
	    //StringBuffer text = new StringBuffer();
	    StringBuilder sb = new StringBuilder();
	    
		conn.setRequestMethod("GET");
		conn.setRequestProperty("Accept", "text/html");
		
		if (conn.getResponseCode() != 200) {
			throw new Exception("Failed : HTTP error code : "
					+ conn.getResponseCode());
		}
		
	    //conn.connect();
	    InputStreamReader in = new InputStreamReader((InputStream) conn.getContent());
	    BufferedReader buff = new BufferedReader(in);
	    String line;
	    do {
	      line = buff.readLine();
	      sb.append(line);
	    } while (line != null);
	    
	    in.close();
	    
	    return sb.toString();
	}
	
	public static void download(String url,String filename){

		try {
			HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
			conn.setConnectTimeout(30000);
			conn.setReadTimeout(120000);
			

			
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
			conn.setRequestProperty("Accept-Encoding","gzip, deflate, sdch");
			conn.setRequestProperty("Accept-Language","en-US,en;q=0.8,es;q=0.6");
			conn.setRequestProperty("Connection","keep-alive");
			conn.setRequestProperty("Host","cdn-gs.radiocut.com.ar");
			conn.setRequestProperty("User-Agent","Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.95 Safari/537.36");
			
			if (conn.getResponseCode() < 200 ||conn.getResponseCode() >= 300) {
				throw new Exception("Failed : HTTP error code : "
						+ conn.getResponseCode());
			}
			
			InputStream is = conn.getInputStream();
			System.out.println(filename);
			OutputStream outstream = new FileOutputStream(new File("/Users/matiaskochman/recordings/basta/"+filename));
			byte[] buffer = new byte[4096];
			int len;
			int count = 0;
			while ((len = is.read(buffer)) > 0) {
				if(count == 20){
					System.out.println(len);
					count = 0;
				}
				outstream.write(buffer, 0, len);
				count++;
			}
			outstream.close();		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void executeCommands(List<String> listaArchivos, LocalDate date) throws IOException {

	    File tempScript = createTempScript(listaArchivos,date);

	    try {
	        ProcessBuilder pb = new ProcessBuilder("bash", tempScript.toString());
	        pb.inheritIO();
	        Process process = pb.start();
	        process.waitFor();
	    } catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
	        tempScript.delete();
	    }
	}

	public static File createTempScript(List<String> listaArchivos, LocalDate date) throws IOException {
		
		StringBuffer sb = new StringBuffer();
		
		sb.append("cat ");
		for (String filename : listaArchivos) {
			sb.append(filename+" ");
		}
		sb.append(" > basta-"+date.getYear()+"-"+date.getMonthValue()+"-"+date.getDayOfMonth()+".mp3");
		
	    File tempScript = File.createTempFile("script", null);

	    Writer streamWriter = new OutputStreamWriter(new FileOutputStream(
	            tempScript));
	    PrintWriter printWriter = new PrintWriter(streamWriter);

	    printWriter.println("#!/bin/bash");
	    printWriter.println("cd /Users/matiaskochman/recordings/basta");
	    printWriter.println(sb.toString());
	    printWriter.println("ls");

	    printWriter.close();

	    return tempScript;
	}	
	
}
