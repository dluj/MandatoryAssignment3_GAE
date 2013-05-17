package com.itu.ma3gae;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.labs.repackaged.org.json.JSONArray;
import com.google.appengine.labs.repackaged.org.json.JSONObject;
import com.google.appengine.labs.repackaged.org.json.JSONTokener;

@SuppressWarnings("serial")
public class MandatoryAssignment3_GAEServlet extends HttpServlet {

	private static final int BUFSIZE = 1024;

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {

		String nameOfRecord = req.getParameter("nameOfRecord");
		if(nameOfRecord != null){//GET from FORM
			String response = this.getQuery(true);
			byte[] testing_array = response.getBytes();
			/*
			 * populate file
			 */
			String mimetype = "application/octet-stream";
			resp.setContentType(mimetype);
			resp.setContentLength(testing_array.length);

			String fileName = nameOfRecord;
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
		}else{//GET from browser
			String response = this.getQuery(false);
			resp.setContentType("text/html");
			resp.getWriter().println(response);


		}
	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		String response = "finished uploading correcyly!";
		// We have one entity group per Guestbook with all Greetings residing
		// in the same entity group as the Guestbook to which they belong.
		// This lets us run a transactional ancestor query to retrieve all
		// Greetings for a given Guestbook.  However, the write rate to each
		// Guestbook should be limited to ~1/second.
		//nameOfRecord, data

		//nameOfRecord = 10_simmi-walks_000000
		//simmi-walks will be the key of the record
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		//store the name of record
		String recordName = req.getParameter("nameOfRecord");

		Key recordNamesKey = KeyFactory.createKey("NamesRecords", "records");
		//
		Query queryForNames = new Query("nameRecord", recordNamesKey).addFilter("name",Query.FilterOperator.EQUAL,recordName);
		Entity namesOfRecords = datastore.prepare(queryForNames).asSingleEntity();
		if(namesOfRecords == null){
			Entity nameRecord = new Entity("nameRecord",recordNamesKey);
			nameRecord.setProperty("name", recordName);
			datastore.put(nameRecord);	
		}
		JSONArray request;
		//recordName is going to be stored with key NamesRecords->records
		Key recordKey = KeyFactory.createKey("Records", recordName);
		try{
			//{nameOfRecord:name,data:[{}]}
			JSONTokener request_tokens = new JSONTokener(req.getParameter("data"));
			JSONObject data = new JSONObject(request_tokens);

			request = (JSONArray)data.get("data");

			for(int i=0; i<request.length();i++){
				JSONObject record_req = (JSONObject)request.get(i);
				Entity record = new Entity("record", recordKey);
				record.setProperty("timestamp", record_req.get("timestamp"));
				record.setProperty("x", record_req.get("x"));
				record.setProperty("y", record_req.get("y"));
				record.setProperty("z", record_req.get("z"));
				record.setProperty("activity", record_req.get("activity"));
				datastore.put(record);
			}
		}catch(Exception e){
			e.printStackTrace();
			response = e.toString();
		}
		resp.getWriter().println(recordName + " -------> " +response +"\r\n");
	}

	public String getQuery(boolean file){
		String response = "null";
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Key recordsNamesKey = KeyFactory.createKey("NamesRecords", "records");
		// Run an ancestor query to ensure we see the most up-to-date
		// view of the Greetings belonging to the selected Guestbook.
		//.addSort("date_added", Query.SortDirection.DESCENDING)
		Query queryForNames = new Query("nameRecord", recordsNamesKey);
		List<Entity> namesOfRecords = datastore.prepare(queryForNames).asList(FetchOptions.Builder.withDefaults());
		//checking if there are no files stored
		if (!namesOfRecords.isEmpty()) {
			if(file){ //return response as file
				response ="";
				for (Entity nameRecord : namesOfRecords) {
					String name = nameRecord.getProperty("name").toString();
					response += "NAME,"+name+"\n";
					Key recordsKey = KeyFactory.createKey("Records", name);
					Query queryForRecords = new Query("record", recordsKey).addSort("timestamp", Query.SortDirection.DESCENDING);
					List<Entity> records = datastore.prepare(queryForRecords).asList(FetchOptions.Builder.withDefaults());
					/**
					 * 
					 * % 1. Title: Mandatory Assignment 3
					   % 
					   % 2. Sources:
					   %      (a) Creator: SSJ, DLV
					   %      (b) Date: May, 2013
					   % 
					   @RELATION iris
					
					   @ATTRIBUTE sepallength  NUMERIC
					   @ATTRIBUTE sepalwidth   NUMERIC
					   @ATTRIBUTE petallength  NUMERIC
					   @ATTRIBUTE petalwidth   NUMERIC
					   @ATTRIBUTE class        {Iris-setosa,Iris-versicolor,Iris-virginica}
					 */
					response = "@relation '"+name+"'\n" +
							"@attribute x numeric \n" +
							"@attribute y numeric \n" +
							"@attribute z numeric \n" +
							"@attribute activity {sitting,stairsup,walking,jumping} \n" +
							"@data \n";

					for(Entity record : records){
						response += record.getProperty("x")+","
									+record.getProperty("y")+","
									+record.getProperty("z")+","
									+record.getProperty("activity")+"\n"
								;
					}
				}
			}else{//return response as html
				response = "<a href=\"/\">Back</a><br />";
				for (Entity nameRecord : namesOfRecords) {
					String name = nameRecord.getProperty("name").toString();
					response += "NAME: ["+name+"]<br/>";
					Key recordsKey = KeyFactory.createKey("Records", name);
					Query queryForRecords = new Query("record", recordsKey).addSort("timestamp", Query.SortDirection.DESCENDING);
					List<Entity> records = datastore.prepare(queryForRecords).asList(FetchOptions.Builder.withDefaults());
					response += "<div style='border:1px #cdcdcd dashed;height: 400px; overflow:auto; width: 600px;'>";
					for(Entity record : records){
						response += "date: ["+record.getProperty("timestamp")+"] " +
								"activity: ["+record.getProperty("activity")+"] " +
								"x: ["+record.getProperty("x")+"] " +
								"y: ["+record.getProperty("y")+"] " +
								"z: ["+record.getProperty("z")+"]<br/>"
								;
					}
					response += "</div><br />____________________________________________________________________________________________________<br />";
				}
			}			
		}else{
			response = "no files yet!";
		}
		return response;
	}

}
