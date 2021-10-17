package donmez.yusuf.shortener.config;


import donmez.yusuf.shortener.services.MySQLService;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

public class JettyServer {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(JettyServer.class);
    private static int port = 8080;
	private Server server;
	 
    public void start() throws Exception {
        
        server = new Server(port);
        
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.setConfigLocation("donmez.yusuf.shortener.config");
        
        ServletContextHandler contextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        contextHandler.setErrorHandler(null);
        contextHandler.addServlet(new ServletHolder(new DispatcherServlet(context)),"/");
        
        server.setHandler(contextHandler);

        System.out.printf("jetty is about to start():%s \n",port);

        server.start();
    }
    
    public static void main(String[] args) throws Exception {
        System.out.println("main.");
        System.out.println("main.");
        try {
            port = Integer.valueOf(args[0]);
            // System.out.println("port is read %s \n", args[0]);
            
        } catch (Exception e) {
            LOGGER.error("port arg exception %s \n ", args, e.fillInStackTrace());
        }
        
        MySQLService.load();
        
		(new JettyServer()).start();
	
	}
}

