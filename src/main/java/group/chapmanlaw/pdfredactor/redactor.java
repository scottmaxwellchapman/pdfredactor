package group.chapmanlaw.pdfredactor;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

public class redactor {

    // Temporary backup file to store the original image before redaction
    private static File backupFile = null;

    /**
     * Redacts a given area on a JPEG image by drawing a black rectangle.
     *
     * @param inputJpgPath Path to the JPEG file.
     * @param x1           X-coordinate of the top-left corner of the box.
     * @param y1           Y-coordinate of the top-left corner of the box.
     * @param x2           X-coordinate of the bottom-right corner of the box.
     * @param y2           Y-coordinate of the bottom-right corner of the box.
     */
    public static void redact(String inputJpgPath, int x1, int y1, int x2, int y2) {
        try {
            // Backup the original image before making any changes
            backupImage(inputJpgPath);

            File imageFile = new File(inputJpgPath);
            BufferedImage image = ImageIO.read(imageFile);

            // Create a graphics context on the buffered image
            Graphics2D g2d = image.createGraphics();
            g2d.setColor(Color.BLACK); // Set color to black
            g2d.fillRect(x1, y1, x2 - x1, y2 - y1); // Draw filled rectangle
            g2d.dispose(); // Release resources

            // Overwrite the original file with the redacted image
            saveCompressedJPEG(image, imageFile, 1.0f); // Save with 75% quality
            System.out.println("Redacted area saved: " + inputJpgPath);
        } catch (IOException e) {
            System.err.println("Error processing image: " + e.getMessage());
        }
    }

    /**
     * Backs up the original image to a temporary file.
     *
     * @param inputJpgPath Path to the JPEG file.
     */
    private static void backupImage(String inputJpgPath) {
        try {
            // Only create a backup if it doesn't already exist
            if (backupFile == null) {
                File originalFile = new File(inputJpgPath);
                backupFile = File.createTempFile("backup_", ".jpg"); // Create a temporary backup file
                backupFile.deleteOnExit(); // Ensure the backup is deleted on exit

                // Copy the original file to the backup file
                BufferedImage originalImage = ImageIO.read(originalFile);
                saveCompressedJPEG(originalImage, backupFile, 1.0f); // Save backup at full quality
                System.out.println("Backup created: " + backupFile.getAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("Error backing up the image: " + e.getMessage());
        }
    }

    /**
     * Saves a BufferedImage as a compressed JPEG file.
     *
     * @param image   The BufferedImage to save.
     * @param file    The file to save the image to.
     * @param quality The compression quality (0.0 - 1.0).
     * @throws IOException If an error occurs while writing the file.
     */
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
            param.setCompressionQuality(quality); // Set compression quality

            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
    }

    /**
     * Undoes the last redaction by restoring the backup file.
     *
     * @param inputJpgPath Path to the JPEG file.
     */
    public static void undo(String inputJpgPath) {
        if (backupFile != null && backupFile.exists()) {
            // Restore the original image from the backup
            File imageFile = new File(inputJpgPath);
            if (imageFile.exists()) {
                imageFile.delete(); // Delete the redacted file
            }
            try {
                // Copy the backup file back to the original file location
                Files.copy(backupFile.toPath(), imageFile.toPath());
            } catch (IOException ex) {
                Logger.getLogger(redactor.class.getName()).log(Level.SEVERE, null, ex);
            }
            System.out.println("Undo successful, restored original image: " + inputJpgPath);
        } else {
            System.out.println("No backup found, undo not possible.");
        }
    }
}
