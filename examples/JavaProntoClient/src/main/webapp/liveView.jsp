<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<!DOCTYPE html>
<html>
	<link rel="stylesheet" type="text/css" href="style.css">
	<script type="text/javascript">
		if ('WebSocket' in window) {
			var webSocket = new WebSocket("ws://localhost:8000/JavaProntoClient/websocket");
	    	webSocket.onopen = function(message) {
	    		console.log("Websocket Connected.");
	    	}
	    	webSocket.onmessage = function(message) {
	    		console.log(message.data);
	    		var ul = document.getElementById("events");
	    		var li = document.createElement("li");
	    		li.appendChild(document.createTextNode(message.data));
	    		ul.appendChild(li);
	    	}
	    	webSocket.onclose = function(message) {
	    		console.log("Websocket Closed.");
	    	}
	    	webSocket.onerror = function(message) {
	    		console.log("WebSocket Error.");
	    	}	
		} else {
			alert("WebSocket isn't supported for this browser.")
		}
    </script>

    <title>VANTIQ LiveView</title>
    <body>
    	<h1>Live feed of events on ${eventName}</h1>
    	<form style="text-align: center" action="${pageContext.request.contextPath}/Catalog" method="post">
   			<input type="submit" name="viewCatalog" value="Return to Catalog">
   			<input type="hidden" name="catalogName" value="${catalogName}">
    	</form>
    	<ul style="text-align: center; list-style-type: none; padding: 0px;" id="events">
    	
    	</ul>
    </body>
</html>