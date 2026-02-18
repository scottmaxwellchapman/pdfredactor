package group.chapmanlaw.pdfredactor;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WebRedactionSession {
    private static final float RENDER_DPI = 300f;

    private final File sourcePdf;
    private final File workspaceDirectory;
    private final List<File> renderedPages;
    private final float quality;

    private WebRedactionSession(File sourcePdf, File workspaceDirectory, List<File> renderedPages, float quality) {
        this.sourcePdf = sourcePdf;
        this.workspaceDirectory = workspaceDirectory;
        this.renderedPages = renderedPages;
        this.quality = quality;
    }

    public static WebRedactionSession fromUpload(File sourcePdf, float quality) throws IOException {
        File workspaceDirectory = new File(System.getProperty("java.io.tmpdir"), "pdfredactor-web-" + System.nanoTime());
        if (!workspaceDirectory.mkdirs()) {
            throw new IOException("Unable to create temp workspace.");
        }

        List<File> renderedPages = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(sourcePdf)) {
            PDFRenderer renderer = new PDFRenderer(document);
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, RENDER_DPI, ImageType.RGB);
                File outputImage = new File(workspaceDirectory, "page-" + (i + 1) + ".png");
                ImageIO.write(image, "png", outputImage);
                renderedPages.add(outputImage);
            }
        }

        return new WebRedactionSession(sourcePdf, workspaceDirectory, renderedPages, quality);
    }

    public int getTotalPages() {
        return renderedPages.size();
    }

    public File getPageFile(int pageIndex) {
        if (pageIndex < 0 || pageIndex >= renderedPages.size()) {
            throw new IllegalArgumentException("Invalid page index.");
        }
        return renderedPages.get(pageIndex);
    }

    public void redact(int pageIndex, int x1, int y1, int x2, int y2) {
        File pageFile = getPageFile(pageIndex);
        redactor.redact(pageFile.getAbsolutePath(), Math.min(x1, x2), Math.min(y1, y2), Math.max(x1, x2), Math.max(y1, y2));
    }

    public void undo(int pageIndex) {
        File pageFile = getPageFile(pageIndex);
        redactor.undo(pageFile.getAbsolutePath());
    }

    public File buildCompressedPdf() throws IOException {
        File mergedPdf = new File(workspaceDirectory, "redacted-merged.pdf");
        File finalPdf = new File(workspaceDirectory, "redacted-final.pdf");
        List<String> imagePaths = renderedPages.stream().map(File::getAbsolutePath).toList();

        if (!combinerWeb.combine(imagePaths, mergedPdf)) {
            throw new IOException("Unable to build redacted PDF.");
        }

        compress.manipulatePdf(mergedPdf.getAbsolutePath(), finalPdf.getAbsolutePath(), quality);
        return finalPdf;
    }

    public String getSourceName() {
        return sourcePdf.getName();
    }
}
