package group.chapmanlaw.pdfredactor;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class combinerWeb {
    public static boolean combine(List<String> imagePaths, File outputFile) {
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
                float scale = Math.min(pageWidth / imageWidth, pageHeight / imageHeight);

                float scaledWidth = imageWidth * scale;
                float scaledHeight = imageHeight * scale;
                float xOffset = (pageWidth - scaledWidth) / 2;
                float yOffset = (pageHeight - scaledHeight) / 2;

                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                    contentStream.drawImage(pdImage, xOffset, yOffset, scaledWidth, scaledHeight);
                }
            }

            document.save(outputFile);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
