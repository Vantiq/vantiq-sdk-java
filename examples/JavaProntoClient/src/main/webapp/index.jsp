<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<!DOCTYPE html>
<html>
    <title>Pronto Client</title>
    <body>
        <h1>VANTIQ Event Catalog</h1>
        <br>
        <form action="${pageContext.request.contextPath}/AllCatalogs" method="post">
     		Username:<br>
		  	<input type="text" name="username" class="text" placeholder="username">
		  	<br>
		  	Password:<br>
		  	<input type="password" name="password" class="text" placeholder="password">
		  	<br>
		  	<input type="submit" name="submitPass" value="Submit">
        </form>
        <h2>--OR--</h2>
        <form action="${pageContext.request.contextPath}/AllCatalogs" method="post">
        	Existing Token:<br>
		  	<input type="text" name="authToken" class="text" placeholder="auth token">
		  	<br>
		  	<input type="submit" name="submitAuth" value="Submit">
        </form>
        <c:if test="${invalidCreds}">
        	<h3>Invalid username and password, please try again.</h3>
        </c:if>
    </body>
</html>

<style type="text/css">
	h1,h2,h3{
        text-align: center;
        font-family: arial, sans-serif;
    }
    form{
    	text-align: center;
        font-family: arial, sans-serif;
    }
    .text{
        margin-top: 5px;
        margin-bottom: 10px;
    }
</style>