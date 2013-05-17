<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="com.google.appengine.api.datastore.DatastoreService" %>
<%@ page import="com.google.appengine.api.datastore.DatastoreServiceFactory"%>
<%@ page import="com.google.appengine.api.datastore.Entity"%>
<%@ page import="com.google.appengine.api.datastore.FetchOptions"%>
<%@ page import="com.google.appengine.api.datastore.Key"%>
<%@ page import="com.google.appengine.api.datastore.KeyFactory"%>
<%@ page import="com.google.appengine.api.datastore.Query"%>
<%@ page import="com.google.appengine.api.datastore.Query.Filter"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<html>
	<head>
		<link rel="stylesheet" type="text/css" href="/css/default.css">
	</head>
	<body>
		<a href="/">Back</a>
		<p>Click to download</p>
		<div id="recordsDataStore">
			<h2>Datastore</h2>
			<%
				DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
				Key recordsNamesKey = KeyFactory.createKey("NamesRecords", "records");
				Query queryForNames = new Query("nameRecord", recordsNamesKey);
				List<Entity> namesOfRecords = datastore.prepare(queryForNames).asList(FetchOptions.Builder.withDefaults());
				if (!namesOfRecords.isEmpty()) {
					for (Entity nameRecord : namesOfRecords) {
						String name = nameRecord.getProperty("name").toString();
						pageContext.setAttribute("nameOfRecord", name);
						%>
							<form action="mandatoryassignment3_gae" method="get">
							    <div><input type="submit" name="nameOfRecord" value="${nameOfRecord}" /></div>
							</form>
						<%
					}
				}else{
					%>
					<p>No files</p>
				<%
				}
			%>
		</div><!-- end of recordsDataStore-->
		<div id="recordsBlob">
			<h2>Blobs</h2>
			<%
				Key BlobsKey = KeyFactory.createKey("keyBlobs", "keyBlobs");
				Query queryForBlobNames = new Query("keyBlobFile", BlobsKey);
				List<Entity> pathsBlobs = datastore.prepare(queryForBlobNames).asList(FetchOptions.Builder.withDefaults());
				if(!pathsBlobs.isEmpty()){
					for(Entity path : pathsBlobs){
						String nameBlob = path.getProperty("name").toString();
						String key = path.getProperty("key").toString();
						pageContext.setAttribute("nameBlob", nameBlob);
						pageContext.setAttribute("key", key);
						%>
						<form action="mandatoryassignment3_gaeblob" method="get">
						    <div>
						    	<input type="hidden" name="blobKey" value="${key}">
						    	<input type="submit" name="blobName" value="${nameBlob}" />
						    </div>
						</form>
						<%
					}
				}else{
					%>
					<p>No blobs</p>
				<%
				}
				%>
		</div><!-- end of recordsBlob-->
	</body>
</html>