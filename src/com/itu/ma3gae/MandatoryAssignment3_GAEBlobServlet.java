package com.itu.ma3gae;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.channels.Channels;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.files.AppEngineFile;
import com.google.appengine.api.files.FileService;
import com.google.appengine.api.files.FileServiceFactory;
import com.google.appengine.api.files.FileWriteChannel;
import com.google.appengine.labs.repackaged.org.json.JSONArray;
import com.google.appengine.labs.repackaged.org.json.JSONObject;
import com.google.appengine.labs.repackaged.org.json.JSONTokener;

public class MandatoryAssignment3_GAEBlobServlet extends HttpServlet{

	private FileService fileService = FileServiceFactory.getFileService();
	private DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
	Key BlobsKey = KeyFactory.createKey("keyBlobs", "keyBlobs");
	private static final int BUFSIZE = 1024;

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		String response = null;
		String blobKeyString = req.getParameter("blobKey");
		if(blobKeyString != null){ //GET from FORM
			String blobName = req.getParameter("blobName");
			BlobKey blobKey = new BlobKey(blobKeyString);
			BlobstoreService blobStoreService = BlobstoreServiceFactory.getBlobstoreService();
			String file = new String(blobStoreService.fetchData(blobKey,0,40000));
			String records[] = file.split(":");
			List<String> list_response = this.createDataSeriesAsArff(blobName, records, 3);
			response ="";
			for(int i=0; i< list_response.size();i++){
				response += list_response.get(i)+"\n";
			}


			//send data
			byte[] testing_array = response.getBytes();
			String mimetype = "application/octet-stream";
			resp.setContentType(mimetype);
			resp.setContentLength(testing_array.length);
			String fileName = blobName;
			// sets HTTP header
			resp.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + ".arff\"");
			byte[] byteBuffer = new byte[BUFSIZE];
			DataInputStream testing_in = new DataInputStream(new ByteArrayInputStream(testing_array));
			int length = 0;
			ServletOutputStream outStream = resp.getOutputStream();
			while ((testing_in != null) && ((length = testing_in.read(byteBuffer)) != -1))
			{
				outStream.write(byteBuffer,0,length);
			}
			testing_in.close();
			outStream.close();

		}else{ // GET from Browser
			Query queryForNames = new Query("keyBlobFile", BlobsKey);
			List<Entity> pathsBlobs = datastore.prepare(queryForNames).asList(FetchOptions.Builder.withDefaults());
			if(pathsBlobs.isEmpty()){
				response = "NO paths";
			}else{
				response = "";
				for(Entity path : pathsBlobs){
					response += "<div style='border:1px #cdcdcd dashed;height: 400px; overflow:auto; width: 600px;margin-top: 10px;'>";			
					response += "path blob: ["+path.getProperty("name")+"]<br/>";

					BlobKey blobKey = new BlobKey(path.getProperty("key").toString());
					BlobstoreService blobStoreService = BlobstoreServiceFactory.getBlobstoreService();
					String file = new String(blobStoreService.fetchData(blobKey,0,40000));
					response += file+"<br />___________________________________________________<br />";
					response += "</div>";
				}
			}				
			resp.getWriter().print(response);
		}
		//		resp.setContentType("text/html");
	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		String response = "Blob Servlet finished!!";
		String nameOfRecord = req.getParameter("nameOfRecord");
		// Create a new Blob file with mime-type "text/plain"			String mimetype = "application/octet-stream";
		AppEngineFile file = fileService.createNewBlobFile("application/octet-stream", nameOfRecord);

		// Open a channel to write to it
		boolean lock = true;
		FileWriteChannel writeChannel = fileService.openWriteChannel(file, lock);
		PrintWriter out = new PrintWriter(Channels.newWriter(writeChannel, "UTF8"));
		JSONArray request;
		try{
			JSONTokener request_tokens = new JSONTokener(req.getParameter("data"));
			JSONObject data = new JSONObject(request_tokens);
			request = (JSONArray)data.get("data");
			for(int i=0; i<request.length();i++){
				JSONObject record_req = (JSONObject)request.get(i);
				String record = record_req.get("x")+","+record_req.get("y")+","+record_req.get("z")+","+record_req.get("activity"); 
				out.print(record+":");
			}
		}catch(Exception e){
			e.printStackTrace();
			response = e.toString();
		}
		out.close();
		writeChannel.closeFinally();
		//save key of file for downloading in get request
		Entity nameRecord = new Entity("keyBlobFile", BlobsKey);
		BlobKey blobKey = fileService.getBlobKey(file);
		nameRecord.setProperty("key", blobKey.getKeyString());
		nameRecord.setProperty("name",nameOfRecord);
		datastore.put(nameRecord);	
		resp.getWriter().println("blobservlet --> "+response+"  \r\n");

	}

	public List<String> createDataSeriesAsArff(String nameoffile, String[] array, int window)
	{
		List<String> listToUse = new ArrayList<String>();
		for(int j=0; j< array.length;j++){
			listToUse.add(array[j]);
		}
//		listToUse.addAll(array);
//		listToUse.remove(0); //no need to remove header


		List<String> temp = new ArrayList<String>();

		temp.add("@relation '"+nameoffile+"'");

		for(int i = 0; i < window; i++){
			temp.add("@attribute x"+i+" numeric");
			temp.add("@attribute y"+i+" numeric");
			temp.add("@attribute z"+i+" numeric");
		}
		temp.add("@attribute activity {walking,sitting,stairsup,running,jumping}");
		temp.add("@data");

		String firstlines = "";
		int lineToStartAt = 0;
		String activity = null;
		for(int y = 0;y<window;y++){
			String[] split = listToUse.get(y).split(",");
			firstlines += split[0]+","+split[1]+","+split[2]+",";
			lineToStartAt = y;
			activity =  split[3];
		}
		firstlines += activity;
		temp.add(firstlines);

		for(int ie = lineToStartAt+1; ie < listToUse.size(); ie++){
			String line = listToUse.get(ie);
			String[] split = listToUse.get(ie).split(",");
			line = split[0]+","+split[1]+","+split[2]+","+split[3];

			String concatadedString = moveStringToLeft(firstlines, line);
			temp.add(concatadedString);
			firstlines = concatadedString;
		}

		return temp;
	}

	public String moveStringToLeft(String linesToMove, String lineToAdd)
	{
		String temp = "";
		String[] split = linesToMove.split(",");
		int index = 0;
		for (String splitstring : split)
		{
			if (index > 2 && index < split.length - 1)
			{
				temp += splitstring + ",";
			}
			index++;
		}
		temp += lineToAdd;
		return temp;
	}
}
