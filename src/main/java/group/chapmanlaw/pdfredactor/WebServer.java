package group.chapmanlaw.pdfredactor;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRegistration;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;

import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;

public class WebServer {
    public static int start() throws Exception {
        int port = findOpenPort(8080);

        Tomcat tomcat = new Tomcat();
        tomcat.setBaseDir(createTempDir());
        tomcat.setPort(port);

        String webContentFolder = new File("src/main/webapp").getAbsolutePath();
        Context context = tomcat.addWebapp("", webContentFolder);

        Tomcat.addServlet(context, "pdfRedactorServlet", new PdfRedactorServlet());
        context.addServletMappingDecoded("/api/*", "pdfRedactorServlet");

        context.addServletContainerInitializer((c, ctx) -> {
            ServletContext servletContext = ctx;
            ServletRegistration jspServlet = servletContext.getServletRegistration("jsp");
            if (jspServlet != null) {
                jspServlet.addMapping("*.jsp", "*.jspx");
            }
        }, null);

        tomcat.start();
        openBrowser(port);
        tomcat.getServer().await();
        return port;
    }

    private static int findOpenPort(int preferredPort) {
        int port = preferredPort;
        while (port < preferredPort + 50) {
            try (ServerSocket ignored = new ServerSocket(port)) {
                return port;
            } catch (IOException ex) {
                port++;
            }
        }
        throw new IllegalStateException("No open port found from " + preferredPort + " to " + (preferredPort + 49));
    }

    private static String createTempDir() throws IOException {
        File tempDir = File.createTempFile("tomcat", "");
        if (!tempDir.delete() || !tempDir.mkdir()) {
            throw new IOException("Unable to create temp dir");
        }
        return tempDir.getAbsolutePath();
    }

    private static void openBrowser(int port) {
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("Headless environment detected. Open http://localhost:" + port + " manually.");
            return;
        }

        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(URI.create("http://localhost:" + port));
            } catch (IOException e) {
                System.out.println("Unable to open browser automatically: " + e.getMessage());
            }
        }
    }
}
