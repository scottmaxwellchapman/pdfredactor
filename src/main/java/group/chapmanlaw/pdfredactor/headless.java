package group.chapmanlaw.pdfredactor;

import javax.imageio.ImageIO;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
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

    public static List<PageImageInfo> exportPageImages(String outputDirectoryPath) throws IOException {
        return exportPageImages(outputDirectoryPath, "page");
    }

    public static List<PageImageInfo> exportPageImages(String outputDirectoryPath, String filePrefix) throws IOException {
        if (outputDirectoryPath == null || outputDirectoryPath.isBlank()) {
            throw new IOException("No output directory provided.");
        }

        String effectivePrefix = (filePrefix == null || filePrefix.isBlank()) ? "page" : filePrefix;
        Path outputDirectory = Path.of(outputDirectoryPath);
        Files.createDirectories(outputDirectory);

        int totalPages = logic.getTotalPages();
        int pageNumberPadding = Math.max(4, String.valueOf(Math.max(1, totalPages)).length());
        List<PageImageInfo> pages = new ArrayList<>();

        for (int pageIndex = 0; pageIndex < totalPages; pageIndex++) {
            BufferedImage image = logic.getOrRenderPage(pageIndex);
            String fileName = String.format("%s-%0" + pageNumberPadding + "d.png", effectivePrefix, pageIndex + 1);
            Path outputPath = outputDirectory.resolve(fileName);
            ImageIO.write(image, "png", outputPath.toFile());

            pages.add(new PageImageInfo(
                    pageIndex,
                    pageIndex + 1,
                    image.getWidth(),
                    image.getHeight(),
                    outputPath.toAbsolutePath().toString()
            ));
        }

        pages.sort(Comparator.comparingInt(PageImageInfo::pageIndex));
        return pages;
    }

    public static void writePageManifestJson(String outputFilePath, List<PageImageInfo> pages) throws IOException {
        if (outputFilePath == null || outputFilePath.isBlank()) {
            throw new IOException("No manifest output file provided.");
        }

        Path outputPath = Path.of(outputFilePath);
        Path parent = outputPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
            writer.write("{\n");
            writer.write("  \"pages\": [\n");
            for (int i = 0; i < pages.size(); i++) {
                PageImageInfo page = pages.get(i);
                writer.write("    {\n");
                writer.write("      \"pageIndex\": " + page.pageIndex() + ",\n");
                writer.write("      \"pageNumber\": " + page.pageNumber() + ",\n");
                writer.write("      \"widthPixels\": " + page.widthPixels() + ",\n");
                writer.write("      \"heightPixels\": " + page.heightPixels() + ",\n");
                writer.write("      \"imagePath\": \"" + escapeJson(page.imagePath()) + "\"\n");
                writer.write("    }");
                if (i < pages.size() - 1) {
                    writer.write(",");
                }
                writer.write("\n");
            }
            writer.write("  ]\n");
            writer.write("}\n");
        }
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

    private static String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    public record PageImageInfo(int pageIndex, int pageNumber, int widthPixels, int heightPixels, String imagePath) {
    }
}
