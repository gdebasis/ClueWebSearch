/*ange this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package servlets;

import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import searcher.ClueWebRetriever;
import searcher.MockRetriever;
import searcher.WT10GRetriever;

/**
 *
 * @author Procheta
 */
public class SelectionServlet extends HttpServlet {
    String propFileName;
    MockRetriever retriever;
    
     public void init(ServletConfig config) throws ServletException {
        try {
            propFileName = config.getInitParameter("configFile");
            //retriever = new WT10GRetriever(propFileName);

            // Create an object for either WT10G or ClueWeb retriever
            Properties prop = new Properties();
            prop.load(new FileReader(propFileName));
            retriever = new MockRetriever();
           
        } catch (Exception ex) {
            ex.printStackTrace();
        }
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
            throws ServletException, IOException, Exception {
        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            /* TODO output your page here. You may use following sample code. */
            
            String queryStr = request.getParameter("query");
            System.out.println("query = |" + queryStr + "|");
            String []selection = request.getParameterValues("selection");
            ArrayList<Integer> selectionArray = new ArrayList<>();
            for(int i = 0; i <  selectionArray.size();i++)
                selectionArray.add(Integer.parseInt(selection[i]));
            HttpSession session = request.getSession();
             TopDocs topDocs = (TopDocs) session.getAttribute(queryStr);
             
             if(topDocs == null)
             {
                 out.print("No query found");
             }
             else
             {
             
                 out.print(retriever.constructJSONForRetrievedSet(topDocs.scoreDocs, selectionArray));
             
             }
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
        try {
            processRequest(request, response);
        } catch (Exception ex) {
            Logger.getLogger(SelectionServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
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
        try {
            processRequest(request, response);
        } catch (Exception ex) {
            Logger.getLogger(SelectionServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
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
