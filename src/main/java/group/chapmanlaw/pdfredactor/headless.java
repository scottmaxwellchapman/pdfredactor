package group.chapmanlaw.pdfredactor;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class headless {

    public static void openPdf(String inputPdfPath, float quality) throws IOException {
        logic.qualitySetting = quality;
        logic.openPdf(inputPdfPath);
    }

    public static List<Dimension> getPageDimensionsInPixels() throws IOException {
        return logic.getAllPageDimensionsInPixels();
    }

    public static void applyPixelRedactions(Map<Integer, List<Rectangle>> redactionsByPage, String outputPdfPath) throws IOException {
        for (Map.Entry<Integer, List<Rectangle>> entry : redactionsByPage.entrySet()) {
            int pageIndex = entry.getKey();
            String pageImagePath = logic.getOrRenderImagePath(pageIndex);
            List<Rectangle> redactions = entry.getValue();
            if (redactions == null) {
                continue;
            }
            for (Rectangle redaction : redactions) {
                if (redaction == null) {
                    continue;
                }
                int x1 = redaction.x;
                int y1 = redaction.y;
                int x2 = redaction.x + redaction.width;
                int y2 = redaction.y + redaction.height;
                redactor.redact(pageImagePath, x1, y1, x2, y2);
            }
            logic.refreshPageFromDisk(pageIndex);
        }

        List<String> pages = new ArrayList<>(logic.getAllImagePathsForExport());
        combiner.combineToFile(pages, new File(outputPdfPath));
    }
}
