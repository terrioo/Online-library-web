/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package web.servlets;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import web.controllers.BookListController;

@WebServlet(name = "PdfContent",
urlPatterns = {"/PdfContent"})
public class PdfContent extends HttpServlet {

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/pdf");
        OutputStream out = response.getOutputStream();
        try {
            int id = Integer.valueOf(request.getParameter("id"));
            Boolean save = Boolean.valueOf(request.getParameter("save"));
            String filename = request.getParameter("filename");
            
            BookListController searchController = (BookListController) request.getSession(false).getAttribute("bookListController");
            byte[] content = searchController.getContent(id);
            response.setContentLength(content.length);
            if (save) {
                response.setHeader("Content-Disposition", "attachment;filename="+ URLEncoder.encode(filename,"UTF-8")+".pdf");
            }
            out.write(content);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            out.close();
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>
}
