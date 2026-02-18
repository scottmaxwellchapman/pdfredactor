package group.chapmanlaw.pdfredactor;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class combiner {

    public static boolean combine(List<String> imagePaths) {
        working myw = new working();
        myw.setLocationRelativeTo(null);
        myw.setVisible(true);

        try (PDDocument document = new PDDocument()) {
            for (String imagePath : imagePaths) {
                File imageFile = new File(imagePath);
                if (!imageFile.exists()) {
                    continue;
                }

                PDPage page = new PDPage(PDRectangle.LETTER);
                document.addPage(page);

                PDImageXObject pdImage = PDImageXObject.createFromFile(imagePath, document);

                float imageWidth = pdImage.getWidth();
                float imageHeight = pdImage.getHeight();

                float pageWidth = PDRectangle.LETTER.getWidth();
                float pageHeight = PDRectangle.LETTER.getHeight();

                float scaleX = pageWidth / imageWidth;
                float scaleY = pageHeight / imageHeight;
                float scale = Math.min(scaleX, scaleY);

                float scaledWidth = imageWidth * scale;
                float scaledHeight = imageHeight * scale;

                float xOffset = (pageWidth - scaledWidth) / 2;
                float yOffset = (pageHeight - scaledHeight) / 2;

                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                    contentStream.drawImage(pdImage, xOffset, yOffset, scaledWidth, scaledHeight);
                }
            }

            String inputPath = logic.inputPath;
            File inputFile = (inputPath == null || inputPath.isBlank()) ? null : new File(inputPath);
            File parentDirectory = (inputFile == null) ? new File(System.getProperty("user.home")) : inputFile.getAbsoluteFile().getParentFile();
            if (parentDirectory == null) {
                parentDirectory = new File(System.getProperty("user.home"));
            }

            String baseFilename = (inputFile == null) ? "redacted_document" : inputFile.getName().replaceFirst("[.][^.]+$", "");
            String defaultFilename = baseFilename + "_redacted.pdf";

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save Combined PDF");
            fileChooser.setCurrentDirectory(parentDirectory);
            fileChooser.setSelectedFile(new File(parentDirectory, defaultFilename));

            int result = fileChooser.showSaveDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                File outputFile = fileChooser.getSelectedFile();
                document.save(outputFile);
                JOptionPane.showMessageDialog(null, "PDF saved successfully!");
                niceties.exitAd();
                return true;
            }

            return false;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error creating PDF: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return false;
        } finally {
            myw.setVisible(false);
            myw.dispose();
        }
    }
}
