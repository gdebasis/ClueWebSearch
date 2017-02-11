/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package servlets;

import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.*;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import searcher.WT10GRetriever;
import javax.servlet.http.HttpSession;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import searcher.ClueWebRetriever;
import searcher.MockRetriever;
/**
 *
 * @author dganguly
 */
public class SearchServlet extends HttpServlet {

    String propFileName;
    WT10GRetriever retriever;
    
    @Override
    public void init(ServletConfig config) throws ServletException {
        try {
            propFileName= config.getInitParameter("configFile");
            //retriever = new WT10GRetriever(propFileName);
            
            // Create an object for either WT10G or ClueWeb retriever
            Properties prop = new Properties();
            prop.load(new FileReader(propFileName));
            String retrClassName = prop.getProperty("retriever.type");
            if (retrClassName.equals("WT10G"))
                retriever = new WT10GRetriever(propFileName);
            else
                retriever = new ClueWebRetriever(propFileName);
        }
        catch (Exception ex) {
            ex.printStackTrace();            
        }
    }
    
    /* Return the snapshot of the current page */
    String getPageViewOfResultList(HttpServletRequest request,
            String query, String indexNumStr, String pageNumberStr,HttpSession session)
            throws Exception {
        
        int indexNumber = -1;

		if(indexNumStr != null)
			indexNumber = Integer.parseInt(indexNumStr);
        int pageNumber = Integer.parseInt(pageNumberStr);
        
        HashMap<Integer, Integer> hitOrder = retriever.chooseIndexHitOrder(session, query);        
        String key = null;
		if (indexNumStr != null)
        	key = query + "." + indexNumStr;
		else
			key = query;
        System.out.println("key : :: "+key);
		TopDocs topDocs = (TopDocs)session.getAttribute(key);
        if (topDocs != null) {
            ScoreDoc[] scoreDocs = topDocs.scoreDocs;
            System.out.println("Entered in session");
            return retriever.constructJSONForRetrievedSet(hitOrder, query, scoreDocs, indexNumber, pageNumber);
        }       
        
			
        topDocs = retriever.retrieve(hitOrder, query, indexNumber, pageNumber);

        session.setAttribute(key, topDocs);
        return retriever.constructJSONForRetrievedSet(hitOrder, query, topDocs.scoreDocs, indexNumber, pageNumber);
    }
    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
		HttpSession session = request.getSession();
        System.out.println((String)session.getId());
        String html = null;
        try {
            String queryStr = request.getParameter("query");
            System.out.println("query = |" + queryStr + "|");
            String pageNum = request.getParameter("pageNum");
            System.out.println("page = |" + pageNum + "|");
            String indexNum = request.getParameter("index");
            System.out.println("index = |" + indexNum + "|");
            
            String selection = request.getParameter("selection");
            ArrayList<Integer> selectionArray = new ArrayList<>();

	    if(selection != null)
            {
		selection = selection.replace("[", "");
            	selection = selection.replace("]", "");
            	String[]selectionAr = selection.split(",");

		for(int i = 0; i <  selectionAr.length;i++)
                 	selectionArray.add(Integer.parseInt(selectionAr[i]));
            }
       	   if (pageNum == null && selection == null) { // no pagination workflow
           {
                html = retriever.retrieve(queryStr);
		out.println(html);}
           }
           else { // pagination workflow
                System.out.println("entered...........");
		if(selection != null)
		{
             		TopDocs topDocs = (TopDocs)session.getAttribute(queryStr);
             
             		if(topDocs == null)
             		{
               			out.print("No query found");
             		}		
             		else
             		{	    
				System.out.println("session found...........");
				MockRetriever ret = new MockRetriever();
				out.print(ret.constructJSONForRetrievedSet(topDocs.scoreDocs, selectionArray));
			}
		}
                else
               {
		 	html = getPageViewOfResultList(request, queryStr, indexNum, pageNum,session);
			out.println(html);
		}
            }
            
            out.close();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }        
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}

