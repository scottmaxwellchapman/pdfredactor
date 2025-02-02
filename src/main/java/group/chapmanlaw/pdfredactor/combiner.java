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
import javax.swing.JFrame;

public class combiner {

    public static void combine(List<String> imagePaths) {
                    working myw = new working();
            myw.setVisible(true);
            myw.setLocationRelativeTo(null);
        try {
            PDDocument document = new PDDocument();

            // Iterate over the image paths
            for (String imagePath : imagePaths) {
                File imageFile = new File(imagePath);
                if (imageFile.exists()) {
                    // Create a letter-sized page (8.5 x 11 inches)
                    PDPage page = new PDPage(PDRectangle.LETTER);
                    document.addPage(page);

                    // Load the image as a PDImage
                    PDImageXObject pdImage = PDImageXObject.createFromFile(imagePath, document);

                    // Get the image dimensions
                    float imageWidth = pdImage.getWidth();
                    float imageHeight = pdImage.getHeight();

                    // Get the page's dimensions (letter size)
                    float pageWidth = PDRectangle.LETTER.getWidth();
                    float pageHeight = PDRectangle.LETTER.getHeight();

                    // Calculate scale factor to fit image within page bounds (keeping aspect ratio)
                    float scaleX = pageWidth / imageWidth;
                    float scaleY = pageHeight / imageHeight;
                    float scale = Math.min(scaleX, scaleY); // Use the smaller scale to ensure the image fits

                    // Calculate the image's scaled dimensions
                    float scaledWidth = imageWidth * scale;
                    float scaledHeight = imageHeight * scale;

                    // Calculate position to center the image on the page
                    float xOffset = (pageWidth - scaledWidth) / 2;
                    float yOffset = (pageHeight - scaledHeight) / 2;

                    // Create a new content stream and draw the image
                    PDPageContentStream contentStream = new PDPageContentStream(document, page);
                    contentStream.drawImage(pdImage, xOffset, yOffset, scaledWidth, scaledHeight);
                    contentStream.close();
                }
            }

            // Get the folder path and base filename from logic.inputPath
            String inputPath = logic.inputPath;  // Assuming logic.inputPath holds the absolute path of the input file
            File inputFile = new File(inputPath);
            String parentDirectory = inputFile.getParent();  // Get the directory of the input file
            String baseFilename = inputFile.getName().replaceFirst("[.][^.]+$", "");  // Remove the extension from the base filename
            String defaultFilename = baseFilename + "_redacted.pdf";  // Add "_redacted.pdf" to the base filename

            // Prompt the user for the file path to save the PDF
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save Combined PDF");
            fileChooser.setCurrentDirectory(new File(parentDirectory));  // Set default folder location
            fileChooser.setSelectedFile(new File(parentDirectory, defaultFilename));  // Prepopulate filename with base filename + "_redacted.pdf"

            int result = fileChooser.showSaveDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                File outputFile = fileChooser.getSelectedFile();
                document.save(outputFile);
                myw.setVisible(false);
                JOptionPane.showMessageDialog(null, "PDF saved successfully!");
                niceties.exitAd();
            }


            document.close();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error creating PDF: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
        myw.setVisible(false);
    }
}
