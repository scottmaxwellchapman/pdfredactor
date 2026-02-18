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
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class redactor {

    // Per-image backups allow undo to behave correctly across multiple pages.
    private static final Map<String, File> backupFiles = new HashMap<>();

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
            if (image == null) {
                throw new IOException("Unable to read input image: " + inputJpgPath);
            }

            // Create a graphics context on the buffered image
            Graphics2D g2d = image.createGraphics();
            g2d.setColor(Color.BLACK);
            g2d.fillRect(x1, y1, x2 - x1, y2 - y1);
            g2d.dispose();

            // Overwrite the original file with the redacted image
            saveCompressedJPEG(image, imageFile, 1.0f);
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
            // Only create a backup if one does not already exist for this image.
            if (!backupFiles.containsKey(inputJpgPath)) {
                File originalFile = new File(inputJpgPath);
                File backupFile = File.createTempFile("backup_", ".jpg");
                backupFile.deleteOnExit();

                BufferedImage originalImage = ImageIO.read(originalFile);
                if (originalImage == null) {
                    throw new IOException("Unable to read original image: " + inputJpgPath);
                }
                saveCompressedJPEG(originalImage, backupFile, 1.0f);
                backupFiles.put(inputJpgPath, backupFile);
                System.out.println("Backup created: " + backupFile.getAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("Error backing up the image: " + e.getMessage());
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
            param.setCompressionQuality(quality);

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
        File backupFile = backupFiles.get(inputJpgPath);
        if (backupFile != null && backupFile.exists()) {
            File imageFile = new File(inputJpgPath);
            try {
                Files.copy(backupFile.toPath(), imageFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                backupFiles.remove(inputJpgPath);
            } catch (IOException ex) {
                Logger.getLogger(redactor.class.getName()).log(Level.SEVERE, null, ex);
            }
            System.out.println("Undo successful, restored original image: " + inputJpgPath);
        } else {
            System.out.println("No backup found, undo not possible.");
        }
    }

    /**
     * Clears all page-specific undo stacks and removes temporary snapshots.
     */
    public static void clearUndoHistory() {
        for (Deque<Path> pageBackups : backupFilesByPage.values()) {
            while (!pageBackups.isEmpty()) {
                Path backupPath = pageBackups.pop();
                try {
                    Files.deleteIfExists(backupPath);
                } catch (IOException ex) {
                    Logger.getLogger(redactor.class.getName()).log(Level.WARNING, null, ex);
                }
            }
        }
        backupFilesByPage.clear();
    }
}
