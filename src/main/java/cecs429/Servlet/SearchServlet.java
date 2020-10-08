package cecs429.Servlet;

import cecs429.csulb.SearchEngine;
import cecs429.documents.GsonDoc;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet(name = "SearchServlet", urlPatterns = {"/search"})
public class SearchServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Gson gson=new Gson();
        ObjectMapper mapper = new ObjectMapper();
        response.setContentType("application/json");
        String dir = request.getParameter("dir");
        String input = request.getParameter("input");
        SearchEngine searchEngine = new SearchEngine();

        try {
            List<GsonDoc> result = searchEngine.search(dir,input);
//            for(GsonDoc gsonDoc : result){
//                mapper.writeValue(response.getWriter(),gsonDoc.getTitle()+"  (" + gsonDoc.getFileName()+ ")");
//            }
            mapper.writeValue(response.getWriter(),result);



        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
//            response.getWriter().print(mapper.writeValueAsString(result.get(0)));
//            List<String> stem = searchEngine.getStem();
//            List<String> vocab = searchEngine.getVocab();
//            response.getWriter().write();


}
}
