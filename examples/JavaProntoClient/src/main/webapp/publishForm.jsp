<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<!DOCTYPE html>
<html>
	<link rel="stylesheet" type="text/css" href="style.css">
    <title>VANTIQ Publish</title>
    <body>
    	<h1>Publishing to: ${publishID}</h1>
    	<h3>Please fill out form in JSON format to publish:</h3>
    	<form style="text-align:center" action="${pageContext.request.contextPath}/Publish" method="post">
    		<textarea rows="10" cols="60" name="publishForm">
{
	"Key": "Value",
	"Key": "Value",
	
	etc...
	
	"Key": "Value"
}	
    		</textarea><br>
   			<c:if test="${success}">
   				<h3>Publish was successful.</h3>
   			</c:if>
   			<c:if test="${fail}">
   				<h3>Publish was unsuccessful.</h3>
   			</c:if>
    		<c:if test="${invalidJSON}">
    			<h3>Invalid JSON, please use proper formatting.</h3>
    		</c:if>
    		<table>
    			<input type="submit" name="formFilled" value="Submit Publish">
    			<input type="hidden" name="publishID" value="${publishID}">
    			<input type="hidden" name="catalogName" value="${catalogName}">
    		</table>
    	</form>
    	<form style="text-align:center" action="${pageContext.request.contextPath}/Catalog" method="post">
 			<input type="submit" name="viewCatalog" value="Return to Catalog">
 			<input type="hidden" name="catalogName" value="${catalogName}">
    	</form>
    </body>
</html>