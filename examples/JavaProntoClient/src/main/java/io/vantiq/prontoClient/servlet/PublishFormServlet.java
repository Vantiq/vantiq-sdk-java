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
import java.util.HashMap;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

/**
 * Servlet implementation class ProntoClientServlet
 */
@WebServlet("/Publish")
public class PublishFormServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // Parameter strings for catalog.jsp's form fields
    private static final String PARAM_CATALOG_NAME  = "catalogName";
    private static final String PARAM_PUBLISH_ID    = "publishID";
    private static final String PARAM_EXECUTE_PUBLISH  = "formFilled";
    private static final String PARAM_PUBLISH_FORM  = "publishForm";
    
    // Global vars
    HashMap<String,Vantiq> vantiqMap = ProntoClientServlet.vantiqMap;
    Gson gson = new Gson();

    /**
     * @see HttpServlet#HttpServlet()
     */
    public PublishFormServlet() {
        super();
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {   
        // Check if submit button is pressed
        boolean executePublish  = request.getParameter(PARAM_EXECUTE_PUBLISH) != null;
        
        // Retrieving all relevant data from view
        String catalogName  = request.getParameter(PARAM_CATALOG_NAME);
        String publishID    = request.getParameter(PARAM_PUBLISH_ID);
        String publishForm  = request.getParameter(PARAM_PUBLISH_FORM);
        
        // Get vantiq instance based on session
        Vantiq vantiq = vantiqMap.get(request.getSession().getId());
        
        // Submit Publish button pressed - Publishes to VANTIQ using the data from the Publish Form as the payload
        if (executePublish) {
            JsonObject payload = new JsonObject();
            try {
                payload = gson.fromJson(publishForm, JsonObject.class);
                String topicID = publishID.replaceFirst("^/topics", "");
                VantiqResponse publishResponse = vantiq.publish(Vantiq.SystemResources.TOPICS.value(), topicID, payload);
                
                // Setting appropriate attribute depending on if publish is successful
                if (publishResponse.isSuccess()) {
                    request.setAttribute("success", true);
                } else {
                    request.setAttribute("fail", true);
                }
                // Displays form used to get the payload for VANTIQ Publish
                request.setAttribute("catalogName", catalogName);
                request.setAttribute("publishID", publishID);
                RequestDispatcher view = request.getRequestDispatcher("publishForm.jsp");
                view.forward(request, response);
            } catch (JsonSyntaxException e) {
                // Displays form used to get the payload for VANTIQ Publish, including invalid JSON error
                request.setAttribute("invalidJSON", true);
                request.setAttribute("catalogName", catalogName);
                request.setAttribute("publishID", publishID);
                RequestDispatcher view = request.getRequestDispatcher("publishForm.jsp");
                view.forward(request, response);
            }
        } else {
            request.setAttribute("catalogName", catalogName);
            request.setAttribute("publishID", publishID);
            RequestDispatcher view = request.getRequestDispatcher("publishForm.jsp");
            view.forward(request, response);
        }
    }
}