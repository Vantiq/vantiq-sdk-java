package io.vantiq.prontoClient.servlet;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.vantiq.client.Vantiq;
import io.vantiq.client.VantiqResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Servlet implementation class ProntoClientServlet
 */
@WebServlet("/AllCatalogs")
public class ProntoClientServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // Parameter strings for index.jsp's form fields
    private static final String PARAM_SUBMIT_PASS   = "submitPass";
    private static final String PARAM_SUBMIT_AUTH   = "submitAuth";
    private static final String PARAM_USERNAME      = "username";
    private static final String PARAM_PASSWORD      = "password";
    private static final String PARAM_AUTHTOKEN     = "authToken";
    
    // Final vars for VANTIQ SDK
    private static final String VANTIQ_SERVER = "http://localhost:8080";
    
    // Global vars
    public static HashMap<String,Vantiq> vantiqMap = new HashMap<String,Vantiq>();
    Gson gson = new Gson();

    /**
     * @see HttpServlet#HttpServlet()
     */
    public ProntoClientServlet() {
        super();
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {   
        // Check if any submit button is pressed
        boolean submitPass = request.getParameter(PARAM_SUBMIT_PASS) != null;
        boolean submitAuth = request.getParameter(PARAM_SUBMIT_AUTH) != null;
        
        // Create and save vantiq instance in vantiqMap so other servlets can access it
        Vantiq vantiq = new io.vantiq.client.Vantiq(VANTIQ_SERVER);
        vantiqMap.put(request.getSession().getId(), vantiq);
                
        // Login button pressed - Using VANTIQ SDK to authenticate with username/password
        if (submitPass) {
            String username = request.getParameter(PARAM_USERNAME);
            String password = request.getParameter(PARAM_PASSWORD);
            VantiqResponse vantiqResponse = vantiq.authenticate(username, password);
            
            if (vantiqResponse.isSuccess()) {                
                // Selecting all of the managers for given user
                // Create object to be used as filter for VANTIQ SDK Select
                HashMap<String,String> selectFilter = new HashMap<String,String>();
                selectFilter.put("ars_properties.manager", "true");
                VantiqResponse managerResponse = vantiq.select("system.nodes", null, selectFilter, null);
                
                // Get list of the managers with their meta data
                ArrayList<JsonObject> managerResponseBody = (ArrayList<JsonObject>) managerResponse.getBody();
                
                // Iterating through the JSON representation for each manager and storing namespace names
                ArrayList<String> managerNames = new ArrayList<String>();
                for (int i = 0; i < managerResponseBody.size(); i++) {
                    String managerName = managerResponseBody.get(0).get("name").getAsString();
                    managerNames.add(managerName);
                }            
                
                // Display the appropriate view
                request.setAttribute("managerData", managerNames);
                RequestDispatcher view = request.getRequestDispatcher("allCatalogs.jsp");
                view.forward(request, response);
            } else {
                // Username/password were invalid, display the appropriate view
                request.setAttribute("invalidCreds", true);
                RequestDispatcher view = request.getRequestDispatcher("index.jsp");
                view.forward(request, response);
            }
            
        // Login button pressed - Using VANTIQ SDK to authenticate with access token
        } else if (submitAuth) {
            String authToken = request.getParameter(PARAM_AUTHTOKEN);
            vantiq.setAccessToken(authToken);
            
            // Selecting all of the managers for given user
            // Create object to be used as filter for VANTIQ SDK Select
            HashMap<String,String> selectFilter = new HashMap<String,String>();
            selectFilter.put("ars_properties.manager", "true");
            VantiqResponse managerResponse = vantiq.select("system.nodes", null, selectFilter, null);
            
            // Get list of the managers with their meta data
            ArrayList<JsonObject> managerResponseBody = (ArrayList<JsonObject>) managerResponse.getBody();
            
            // Iterating through the JSON representation for each manager and storing namespace names
            ArrayList<String> managerNames = new ArrayList<String>();
            for (int i = 0; i < managerResponseBody.size(); i++) {
                String managerName = managerResponseBody.get(0).get("name").getAsString();
                managerNames.add(managerName);
            }            
            
            // Display the appropriate view
            request.setAttribute("managerData", managerNames);
            RequestDispatcher view = request.getRequestDispatcher("allCatalogs.jsp");
            view.forward(request, response);
        }
    }
}
