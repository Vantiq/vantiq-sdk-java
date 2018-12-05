<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<!DOCTYPE html>
<html>
    <link rel="stylesheet" type="text/css" href="style.css">
    <title>Pronto Client</title>
    <body>
        <h1>VANTIQ Event Catalog</h1>
        <br>
        <form style="text-align:center; font-family:arial,sans-serif" action="${pageContext.request.contextPath}/AllCatalogs" method="post">
            Username:<br>
            <input type="text" name="username" style="margin-top: 5px; margin-bottom: 10px" placeholder="username">
            <br>
            Password:<br>
            <input type="password" name="password" style="margin-top: 5px; margin-bottom: 10px" placeholder="password">
            <br>
            <input type="submit" name="submitPass" value="Submit">
        </form>
        <h2>--OR--</h2>
        <form style="text-align:center; font-family:arial,sans-serif" action="${pageContext.request.contextPath}/AllCatalogs" method="post">
            Existing Token:<br>
            <input type="text" name="authToken" style="margin-top: 5px; margin-bottom: 10px" placeholder="auth token">
            <br>
            <input type="submit" name="submitAuth" value="Submit">
        </form>
        <c:if test="${invalidCreds}">
            <h3>Invalid username and password, please try again.</h3>
        </c:if>
    </body>
</html>