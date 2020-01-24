/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package web.filters;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.http.HttpSession;

@WebFilter(filterName= "CheckSessionFilter", 
urlPatterns= "/pages/*")
public class CheckSessionFilter implements Filter {

    private static final boolean debug = false;
    private FilterConfig filterConfig = null;

    public CheckSessionFilter() {
    }

    private void doBeforeProcessing(RequestWrapper request, ResponseWrapper response)
            throws IOException, ServletException {
        if (debug) {
            log("CheckSessionFilter:DoBeforeProcessing");
        }
    }

    private void doAfterProcessing(RequestWrapper request, ResponseWrapper response)
            throws IOException, ServletException {
        if (debug) {
            log("CheckSessionFilter:DoAfterProcessing");
        }
    }

   
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain)
            throws IOException, ServletException {

        if (debug) {
            log("CheckSessionFilter:doFilter()");
        }

        RequestWrapper wrappedRequest = new RequestWrapper((HttpServletRequest) request);
        ResponseWrapper wrappedResponse = new ResponseWrapper((HttpServletResponse) response);

        doBeforeProcessing(wrappedRequest, wrappedResponse);

        Throwable problem = null;

        HttpSession session = wrappedRequest.getSession(false);
        if (session == null || session.isNew()){                
            wrappedResponse.sendRedirect(wrappedRequest.getContextPath() + "/index.xhtml");
        } else {
            chain.doFilter(wrappedRequest, wrappedResponse);
        }

        doAfterProcessing(wrappedRequest, wrappedResponse);

        if (problem != null) {
            if (problem instanceof ServletException) {
                throw (ServletException) problem;
            }
            if (problem instanceof IOException) {
                throw (IOException) problem;
            }
            sendProcessingError(problem, response);
        }
    }

    public FilterConfig getFilterConfig() {
        return (this.filterConfig);
    }

  
    public void setFilterConfig(FilterConfig filterConfig) {
        this.filterConfig = filterConfig;
    }

    
    public void destroy() {
    }

   
    public void init(FilterConfig filterConfig) {
        this.filterConfig = filterConfig;
        if (filterConfig != null) {
            if (debug) {
                log("CheckSessionFilter: Initializing filter");
            }
        }
    }

    
    @Override
    public String toString() {
        if (filterConfig == null) {
            return ("CheckSessionFilter()");
        }
        StringBuilder sb = new StringBuilder("CheckSessionFilter(");
        sb.append(filterConfig);
        sb.append(")");
        return (sb.toString());

    }

    private void sendProcessingError(Throwable t, ServletResponse response) {
        String stackTrace = getStackTrace(t);

        if (stackTrace != null && !stackTrace.equals("")) {
            try {
                response.setContentType("text/html");
                PrintStream ps = new PrintStream(response.getOutputStream());
                PrintWriter pw = new PrintWriter(ps);
                pw.print("<html>\n<head>\n<title>Error</title>\n</head>\n<body>\n"); 

                pw.print("<h1>The resource did not process correctly</h1>\n<pre>\n");
                pw.print(stackTrace);
                pw.print("</pre></body>\n</html>"); 
                pw.close();
                ps.close();
                response.getOutputStream().close();
            } catch (Exception ex) {
            }
        } else {
            try {
                PrintStream ps = new PrintStream(response.getOutputStream());
                t.printStackTrace(ps);
                ps.close();
                response.getOutputStream().close();
            } catch (Exception ex) {
            }
        }
    }

    public static String getStackTrace(Throwable t) {
        String stackTrace = null;
        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            pw.close();
            sw.close();
            stackTrace = sw.getBuffer().toString();
        } catch (Exception ex) {
        }
        return stackTrace;
    }

    public void log(String msg) {
        filterConfig.getServletContext().log(msg);
    }

    
    class RequestWrapper extends HttpServletRequestWrapper {

        public RequestWrapper(HttpServletRequest request) {
            super(request);
        }
        
        protected Hashtable localParams = null;

        public void setParameter(String name, String[] values) {
            if (debug) {
                System.out.println("CheckSessionFilter::setParameter(" + name + "=" + values + ")" + " localParams = " + localParams);
            }

            if (localParams == null) {
                localParams = new Hashtable();
                
                Map wrappedParams = getRequest().getParameterMap();
                Set keySet = wrappedParams.keySet();
                for (Iterator it = keySet.iterator(); it.hasNext();) {
                    Object key = it.next();
                    Object value = wrappedParams.get(key);
                    localParams.put(key, value);
                }
            }
            localParams.put(name, values);
        }

        @Override
        public String getParameter(String name) {
            if (debug) {
                System.out.println("CheckSessionFilter::getParameter(" + name + ") localParams = " + localParams);
            }
            if (localParams == null) {
                return getRequest().getParameter(name);
            }
            Object val = localParams.get(name);
            if (val instanceof String) {
                return (String) val;
            }
            if (val instanceof String[]) {
                String[] values = (String[]) val;
                return values[0];
            }
            return (val == null ? null : val.toString());
        }

        @Override
        public String[] getParameterValues(String name) {
            if (debug) {
                System.out.println("CheckSessionFilter::getParameterValues(" + name + ") localParams = " + localParams);
            }
            if (localParams == null) {
                return getRequest().getParameterValues(name);
            }
            return (String[]) localParams.get(name);
        }

        @Override
        public Enumeration getParameterNames() {
            if (debug) {
                System.out.println("CheckSessionFilter::getParameterNames() localParams = " + localParams);
            }
            if (localParams == null) {
                return getRequest().getParameterNames();
            }
            return localParams.keys();
        }

        @Override
        public Map getParameterMap() {
            if (debug) {
                System.out.println("CheckSessionFilter::getParameterMap() localParams = " + localParams);
            }
            if (localParams == null) {
                return getRequest().getParameterMap();
            }
            return localParams;
        }
    }

    
    class ResponseWrapper extends HttpServletResponseWrapper {

        public ResponseWrapper(HttpServletResponse response) {
            super(response);
        }
    }
}
