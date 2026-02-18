package group.chapmanlaw.pdfredactor;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.Loader;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class logic {
    private static List<String> imagePaths = new ArrayList<>();
    private static int totalPages = 0;
    private static Path sessionTempDirectory;
    public static String inputPath="";
    public static float qualitySetting = 0.5f;
    public static void convertPDFToImages() {
        // Prompt user for PDF file
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select a PDF file");
        int result = fileChooser.showOpenDialog(null);

        if (result != JFileChooser.APPROVE_OPTION) {
            System.out.println("No file selected. Exiting.");
            return;
        }

        File pdfFile = fileChooser.getSelectedFile();
        inputPath = pdfFile.getAbsolutePath();
        System.out.println("Selected PDF: " + pdfFile.getAbsolutePath());

        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            initializeSessionTempDirectory();
            PDFRenderer renderer = new PDFRenderer(document);
            totalPages = document.getNumberOfPages();
            imagePaths.clear();
            working myw = new working();
            myw.setVisible(true);
            myw.setLocationRelativeTo(null);

            for (int i = 0; i < totalPages; i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, 150, ImageType.RGB); // Lower DPI for compression

                File tempFile = createSessionTempFile("pdfredactor_page_" + (i + 1) + "_", ".jpg", "pages");

                saveCompressedJPEG(image, tempFile, qualitySetting); // Save with 30% quality

                imagePaths.add(tempFile.getAbsolutePath());

                System.out.println("Saved page " + (i + 1) + " as: " + tempFile.getAbsolutePath());
            }
            System.out.println("Conversion completed. Total pages: " + totalPages);
            myw.setVisible(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void initializeSessionTempDirectory() throws IOException {
        cleanupSessionTempArtifacts();
        sessionTempDirectory = Files.createTempDirectory("pdfredactor-session-");
        sessionTempDirectory.toFile().deleteOnExit();
    }

    public static File createSessionTempFile(String prefix, String suffix, String subdirectory) throws IOException {
        if (sessionTempDirectory == null) {
            initializeSessionTempDirectory();
        }

        Path targetDirectory = sessionTempDirectory;
        if (subdirectory != null && !subdirectory.isBlank()) {
            targetDirectory = sessionTempDirectory.resolve(subdirectory);
            Files.createDirectories(targetDirectory);
            targetDirectory.toFile().deleteOnExit();
        }

        File file = Files.createTempFile(targetDirectory, prefix, suffix).toFile();
        file.deleteOnExit();
        return file;
    }

    public static void cleanupSessionTempArtifacts() {
        redactor.clearBackups();

        if (sessionTempDirectory == null) {
            imagePaths.clear();
            totalPages = 0;
            return;
        }

        try {
            Files.walk(sessionTempDirectory)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        } catch (IOException e) {
            System.err.println("Unable to fully clean session files: " + e.getMessage());
        } finally {
            sessionTempDirectory = null;
            imagePaths.clear();
            totalPages = 0;
        }
    }

    private static void saveCompressedJPEG(BufferedImage image, File file, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            throw new IOException("No JPEG writer available.");
        }
        ImageWriter writer = writers.next();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(file)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality); // Set compression quality (0.0 - 1.0)

            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
    }

    public static List<String> getImagePaths() {
        return imagePaths;
    }

    public static int getTotalPages() {
        return totalPages;
    }
}
