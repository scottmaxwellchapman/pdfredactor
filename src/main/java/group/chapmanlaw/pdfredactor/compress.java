package group.chapmanlaw.pdfredactor;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.PdfDictionary;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfStream;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.xobject.PdfImageXObject;
import com.itextpdf.layout.element.Image;

public class compress {

    // Logging
    private static final Logger logger = LoggerFactory.getLogger(compress.class);

    public static void manipulatePdf(String src, String dest, Float resizeFactor) throws IOException {
        if (resizeFactor == null || resizeFactor <= 0) {
            throw new IllegalArgumentException("resizeFactor must be greater than 0.");
        }

        try (PdfDocument pdfDoc = new PdfDocument(new PdfReader(src), new PdfWriter(dest))) {
            for (int i = 1; i <= pdfDoc.getNumberOfPages(); i++) {
                PdfPage page = pdfDoc.getPage(i);
                PdfDictionary pageDict = page.getPdfObject();
                PdfDictionary resources = pageDict.getAsDictionary(PdfName.Resources);
                if (resources == null) {
                    continue;
                }

                PdfDictionary xObjects = resources.getAsDictionary(PdfName.XObject);
                if (xObjects == null) {
                    continue;
                }

                for (Iterator<PdfName> iter = xObjects.keySet().iterator(); iter.hasNext(); ) {
                    PdfName imgRef = iter.next();
                    PdfStream stream = xObjects.getAsStream(imgRef);
                    if (stream == null) {
                        continue;
                    }

                    PdfImageXObject image = new PdfImageXObject(stream);
                    BufferedImage bi = image.getBufferedImage();
                    if (bi == null) {
                        continue;
                    }

                    int width = Math.max(1, (int) (bi.getWidth() * resizeFactor));
                    int height = Math.max(1, (int) (bi.getHeight() * resizeFactor));
                    BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                    AffineTransform at = AffineTransform.getScaleInstance(resizeFactor, resizeFactor);

                    Graphics2D g = img.createGraphics();
                    try {
                        g.drawRenderedImage(bi, at);
                    } finally {
                        g.dispose();
                    }

                    ByteArrayOutputStream imgBytes = new ByteArrayOutputStream();
                    ImageIO.write(img, "JPG", imgBytes);
                    Image imgNew = new Image(ImageDataFactory.create(imgBytes.toByteArray()));

                    xObjects.put(imgRef, imgNew.getXObject().getPdfObject());
                }
            }
        }

        logger.info("Compressed PDF created at {}", dest);
    }
}
