package group.chapmanlaw.pdfredactor;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

@MultipartConfig
public class PdfRedactorServlet extends HttpServlet {
    private static final String SESSION_KEY = "pdf-redactor-session";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo() == null ? "" : req.getPathInfo();
        WebRedactionSession session = getSession(req);

        switch (path) {
            case "/status" -> writeStatus(resp, session);
            case "/page" -> writePage(req, resp, session);
            case "/download" -> writeDownload(resp, session);
            default -> sendJson(resp, HttpServletResponse.SC_NOT_FOUND, "{\"error\":\"Unknown endpoint.\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String path = req.getPathInfo() == null ? "" : req.getPathInfo();

        switch (path) {
            case "/upload" -> handleUpload(req, resp);
            case "/redact" -> handleRedact(req, resp);
            case "/undo" -> handleUndo(req, resp);
            default -> sendJson(resp, HttpServletResponse.SC_NOT_FOUND, "{\"error\":\"Unknown endpoint.\"}");
        }
    }

    private void handleUpload(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        var filePart = req.getPart("pdf");
        if (filePart == null || filePart.getSize() == 0) {
            sendJson(resp, HttpServletResponse.SC_BAD_REQUEST, "{\"error\":\"Please upload a PDF file.\"}");
            return;
        }

        float quality;
        try {
            quality = Float.parseFloat(req.getParameter("quality"));
        } catch (Exception ex) {
            sendJson(resp, HttpServletResponse.SC_BAD_REQUEST, "{\"error\":\"Quality must be a number between 0.1 and 1.0.\"}");
            return;
        }

        if (quality < 0.1f || quality > 1.0f) {
            sendJson(resp, HttpServletResponse.SC_BAD_REQUEST, "{\"error\":\"Quality must be a number between 0.1 and 1.0.\"}");
            return;
        }

        File uploadedPdf = File.createTempFile("pdfredactor-upload-", ".pdf");
        try (InputStream inputStream = filePart.getInputStream()) {
            Files.copy(inputStream, uploadedPdf.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }

        WebRedactionSession redactionSession = WebRedactionSession.fromUpload(uploadedPdf, quality);
        req.getSession(true).setAttribute(SESSION_KEY, redactionSession);
        writeStatus(resp, redactionSession);
    }

    private void handleRedact(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        WebRedactionSession session = getSession(req);
        if (session == null) {
            sendJson(resp, HttpServletResponse.SC_BAD_REQUEST, "{\"error\":\"No active session. Upload a PDF first.\"}");
            return;
        }

        try {
            int page = Integer.parseInt(req.getParameter("page"));
            int x1 = Integer.parseInt(req.getParameter("x1"));
            int y1 = Integer.parseInt(req.getParameter("y1"));
            int x2 = Integer.parseInt(req.getParameter("x2"));
            int y2 = Integer.parseInt(req.getParameter("y2"));

            session.redact(page, x1, y1, x2, y2);
            sendJson(resp, HttpServletResponse.SC_OK, "{\"status\":\"ok\"}");
        } catch (Exception e) {
            sendJson(resp, HttpServletResponse.SC_BAD_REQUEST, "{\"error\":\"Invalid redaction coordinates.\"}");
        }
    }

    private void handleUndo(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        WebRedactionSession session = getSession(req);
        if (session == null) {
            sendJson(resp, HttpServletResponse.SC_BAD_REQUEST, "{\"error\":\"No active session. Upload a PDF first.\"}");
            return;
        }

        try {
            int page = Integer.parseInt(req.getParameter("page"));
            session.undo(page);
            sendJson(resp, HttpServletResponse.SC_OK, "{\"status\":\"ok\"}");
        } catch (Exception e) {
            sendJson(resp, HttpServletResponse.SC_BAD_REQUEST, "{\"error\":\"Invalid page number.\"}");
        }
    }

    private void writePage(HttpServletRequest req, HttpServletResponse resp, WebRedactionSession session) throws IOException {
        if (session == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "No active session.");
            return;
        }

        int page = Integer.parseInt(req.getParameter("page"));
        File pageFile = session.getPageFile(page);
        resp.setContentType("image/png");
        Files.copy(pageFile.toPath(), resp.getOutputStream());
    }

    private void writeDownload(HttpServletResponse resp, WebRedactionSession session) throws IOException {
        if (session == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "No active session.");
            return;
        }

        File finalPdf = session.buildCompressedPdf();
        String outputName = session.getSourceName().replaceFirst("[.][^.]+$", "") + "_redacted.pdf";
        resp.setContentType("application/pdf");
        resp.setHeader("Content-Disposition", "attachment; filename=\"" + outputName + "\"");
        Files.copy(finalPdf.toPath(), resp.getOutputStream());
    }

    private void writeStatus(HttpServletResponse resp, WebRedactionSession session) throws IOException {
        if (session == null) {
            sendJson(resp, HttpServletResponse.SC_OK, "{\"loaded\":false}");
            return;
        }
        String json = "{\"loaded\":true,\"totalPages\":" + session.getTotalPages() + "}";
        sendJson(resp, HttpServletResponse.SC_OK, json);
    }

    private WebRedactionSession getSession(HttpServletRequest req) {
        HttpSession httpSession = req.getSession(false);
        if (httpSession == null) {
            return null;
        }
        return (WebRedactionSession) httpSession.getAttribute(SESSION_KEY);
    }

    private void sendJson(HttpServletResponse resp, int status, String json) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.getWriter().write(json);
    }
}
