package group.chapmanlaw.pdfredactor;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HeadlessTest {

    @Test
    void reportsPerPageDimensionsInPixels() throws Exception {
        File inputPdf = createLetterPdf(2);

        headless.openPdf(inputPdf.getAbsolutePath(), 1.0f);
        List<Dimension> dimensions = headless.getPageDimensionsInPixels();

        assertEquals(2, dimensions.size());
        assertEquals(new Dimension(2550, 3300), dimensions.get(0));
        assertEquals(new Dimension(2550, 3300), dimensions.get(1));
    }

    @Test
    void appliesPerPagePixelRedactions() throws Exception {
        File inputPdf = createLetterPdf(1);
        File outputPdf = File.createTempFile("redacted-out", ".pdf");
        outputPdf.deleteOnExit();

        headless.openPdf(inputPdf.getAbsolutePath(), 1.0f);
        headless.applyPixelRedactions(
                Map.of(0, List.of(new Rectangle(100, 100, 80, 60))),
                outputPdf.getAbsolutePath()
        );

        try (PDDocument outDoc = Loader.loadPDF(outputPdf)) {
            PDFRenderer renderer = new PDFRenderer(outDoc);
            BufferedImage page = renderer.renderImageWithDPI(0, 300f, ImageType.RGB);
            assertEquals(Color.BLACK.getRGB(), page.getRGB(120, 120));
        }
    }

    private File createLetterPdf(int pages) throws Exception {
        File file = File.createTempFile("headless-test", ".pdf");
        file.deleteOnExit();
        try (PDDocument doc = new PDDocument()) {
            for (int i = 0; i < pages; i++) {
                doc.addPage(new PDPage(PDRectangle.LETTER));
            }
            doc.save(file);
        }
        return file;
    }
}
