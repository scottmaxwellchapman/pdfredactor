package group.chapmanlaw.pdfredactor;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.Loader;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class logic {
    private static final int PAGE_CACHE_SIZE = 3;
    private static final float BASE_RENDER_DPI = 300f;
    private static List<String> imagePaths = new ArrayList<>();
    private static int totalPages = 0;
    private static PDDocument document;
    private static PDFRenderer renderer;
    private static final Map<Integer, BufferedImage> pageCache = new LinkedHashMap<Integer, BufferedImage>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, BufferedImage> eldest) {
            return size() > PAGE_CACHE_SIZE;
        }
    };
    public static String inputPath="";
    public static float qualitySetting = 1.0f;

    public static boolean convertPDFToImages() {
        // Prompt user for PDF file
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select a PDF file");
        int result = fileChooser.showOpenDialog(null);

        if (result != JFileChooser.APPROVE_OPTION) {
            System.out.println("No file selected.");
            return false;
        }

        File pdfFile = fileChooser.getSelectedFile();
        inputPath = pdfFile.getAbsolutePath();
        System.out.println("Selected PDF: " + pdfFile.getAbsolutePath());

        try {
            closeOpenDocument();
            document = Loader.loadPDF(pdfFile);
            renderer = new PDFRenderer(document);
            totalPages = document.getNumberOfPages();
            imagePaths.clear();
            for (int i = 0; i < totalPages; i++) {
                imagePaths.add(null);
            }
            pageCache.clear();
            working myw = new working();
            myw.setVisible(true);
            myw.setLocationRelativeTo(null);
            myw.setVisible(false);
            System.out.println("Opened document. Total pages: " + totalPages);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static BufferedImage getOrRenderPage(int pageIndex) throws IOException {
        validatePageIndex(pageIndex);

        BufferedImage image = pageCache.get(pageIndex);
        if (image == null) {
            ensureRenderer();
            image = renderer.renderImageWithDPI(pageIndex, getRenderDpi(), ImageType.RGB);
            pageCache.put(pageIndex, image);
        }

        String pagePath = imagePaths.get(pageIndex);
        if (pagePath == null) {
            File tempFile = File.createTempFile("pdfredactor_" + (pageIndex + 1), ".png");
            tempFile.deleteOnExit();
            ImageIO.write(image, "png", tempFile);
            imagePaths.set(pageIndex, tempFile.getAbsolutePath());
            System.out.println("Rendered page " + (pageIndex + 1) + " to: " + tempFile.getAbsolutePath());
        }

        prefetchAdjacentPages(pageIndex);
        return image;
    }

    public static String getOrRenderImagePath(int pageIndex) throws IOException {
        validatePageIndex(pageIndex);
        getOrRenderPage(pageIndex);
        return imagePaths.get(pageIndex);
    }

    public static void refreshPageFromDisk(int pageIndex) throws IOException {
        validatePageIndex(pageIndex);
        String pagePath = imagePaths.get(pageIndex);
        if (pagePath == null) {
            return;
        }

        BufferedImage updatedImage = ImageIO.read(new File(pagePath));
        if (updatedImage != null) {
            pageCache.put(pageIndex, updatedImage);
        }
    }

    public static List<String> getAllImagePathsForExport() throws IOException {
        List<String> renderedPaths = new ArrayList<>();
        for (int i = 0; i < totalPages; i++) {
            renderedPaths.add(getOrRenderImagePath(i));
        }
        return renderedPaths;
    }

    private static void ensureRenderer() throws IOException {
        if (renderer == null || document == null) {
            if (inputPath == null || inputPath.isEmpty()) {
                throw new IOException("No PDF is loaded.");
            }

            document = Loader.loadPDF(new File(inputPath));
            renderer = new PDFRenderer(document);
            totalPages = document.getNumberOfPages();
        }
    }

    private static void prefetchAdjacentPages(int pageIndex) {
        prefetchPage(pageIndex - 1);
        prefetchPage(pageIndex + 1);
    }

    private static void prefetchPage(int pageIndex) {
        if (pageIndex < 0 || pageIndex >= totalPages || pageCache.containsKey(pageIndex)) {
            return;
        }

        try {
            ensureRenderer();
            BufferedImage image = renderer.renderImageWithDPI(pageIndex, getRenderDpi(), ImageType.RGB);
            pageCache.put(pageIndex, image);
        } catch (IOException e) {
            System.err.println("Unable to prefetch page " + (pageIndex + 1) + ": " + e.getMessage());
        }
    }


    private static float getRenderDpi() {
        float clampedQuality = Math.max(0.1f, Math.min(1.0f, qualitySetting));
        return BASE_RENDER_DPI * clampedQuality;
    }

    private static void validatePageIndex(int pageIndex) {
        if (pageIndex < 0 || pageIndex >= totalPages) {
            throw new IllegalArgumentException("Invalid page index: " + pageIndex);
        }
    }

    private static void closeOpenDocument() throws IOException {
        if (document != null) {
            document.close();
        }
        document = null;
        renderer = null;
    }

    public static List<String> getImagePaths() {
        return imagePaths;
    }

    public static int getTotalPages() {
        return totalPages;
    }
}
