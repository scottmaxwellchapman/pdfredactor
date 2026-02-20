package group.chapmanlaw.pdfredactor;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class redactor {

    // Multi-step per-image backups support multiple undo operations.
    private static final Map<String, Deque<File>> backupFiles = new HashMap<>();

    /**
     * Redacts a given area on an image by drawing a black rectangle.
     *
     * @param inputJpgPath Path to the image file.
     * @param x1           X-coordinate of the top-left corner of the box.
     * @param y1           Y-coordinate of the top-left corner of the box.
     * @param x2           X-coordinate of the bottom-right corner of the box.
     * @param y2           Y-coordinate of the bottom-right corner of the box.
     */
    public static void redact(String inputJpgPath, int x1, int y1, int x2, int y2) {
        if (inputJpgPath == null || inputJpgPath.isBlank()) {
            System.err.println("Error processing image: image path is empty.");
            return;
        }

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
            ImageIO.write(image, "png", imageFile);
            System.out.println("Redacted area saved: " + inputJpgPath);
        } catch (IOException e) {
            System.err.println("Error processing image: " + e.getMessage());
        }
    }

    /**
     * Backs up the original image to a temporary file.
     *
     * @param inputJpgPath Path to the image file.
     */
    private static void backupImage(String inputJpgPath) {
        if (inputJpgPath == null || inputJpgPath.isBlank()) {
            return;
        }

        try {
            File originalFile = new File(inputJpgPath);
            File backupFile = File.createTempFile("backup_", ".png");
            backupFile.deleteOnExit();

            BufferedImage originalImage = ImageIO.read(originalFile);
            if (originalImage == null) {
                throw new IOException("Unable to read original image: " + inputJpgPath);
            }
            ImageIO.write(originalImage, "png", backupFile);

            backupFiles.computeIfAbsent(inputJpgPath, key -> new ArrayDeque<>()).push(backupFile);
            System.out.println("Backup created: " + backupFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error backing up the image: " + e.getMessage());
        }
    }

    /**
     * Undoes the last redaction by restoring the backup file.
     *
     * @param inputJpgPath Path to the image file.
     */
    public static void undo(String inputJpgPath) {
        if (inputJpgPath == null || inputJpgPath.isBlank()) {
            System.out.println("No backup found, undo not possible.");
            return;
        }

        Deque<File> history = backupFiles.get(inputJpgPath);
        if (history != null && !history.isEmpty()) {
            File backupFile = history.pop();
            if (backupFile.exists()) {
                File imageFile = new File(inputJpgPath);
                try {
                    Files.copy(backupFile.toPath(), imageFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ex) {
                    Logger.getLogger(redactor.class.getName()).log(Level.SEVERE, null, ex);
                }
                if (history.isEmpty()) {
                    backupFiles.remove(inputJpgPath);
                }
                System.out.println("Undo successful, restored previous image state: " + inputJpgPath);
                return;
            }
        }

        System.out.println("No backup found, undo not possible.");
    }
}
