package group.chapmanlaw.pdfredactor;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class RedactorTest {

    @Test
    void supportsMultiStepUndo() throws Exception {
        File imageFile = File.createTempFile("redactor-test", ".png");
        imageFile.deleteOnExit();

        BufferedImage base = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = base.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 20, 20);
        g.dispose();
        ImageIO.write(base, "png", imageFile);

        redactor.redact(imageFile.getAbsolutePath(), 0, 0, 10, 10);
        redactor.redact(imageFile.getAbsolutePath(), 10, 10, 20, 20);

        BufferedImage afterTwoEdits = ImageIO.read(imageFile);
        assertEquals(Color.BLACK.getRGB(), afterTwoEdits.getRGB(5, 5));
        assertEquals(Color.BLACK.getRGB(), afterTwoEdits.getRGB(15, 15));

        redactor.undo(imageFile.getAbsolutePath());
        BufferedImage afterFirstUndo = ImageIO.read(imageFile);
        assertEquals(Color.BLACK.getRGB(), afterFirstUndo.getRGB(5, 5));
        assertEquals(Color.WHITE.getRGB(), afterFirstUndo.getRGB(15, 15));

        redactor.undo(imageFile.getAbsolutePath());
        BufferedImage afterSecondUndo = ImageIO.read(imageFile);
        assertEquals(Color.WHITE.getRGB(), afterSecondUndo.getRGB(5, 5));
        assertEquals(Color.WHITE.getRGB(), afterSecondUndo.getRGB(15, 15));
    }
}
