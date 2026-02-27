package group.chapmanlaw.pdfredactor;

import javax.swing.JFrame;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class app {

    public static void main(String[] args) {
        if (args != null && args.length > 0) {
            runCli(args);
            return;
        }

        update.checkForUpdates();
        niceties.intro();
        launch myLaunch = new launch();
        myLaunch.setLocationRelativeTo(null);
        myLaunch.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        myLaunch.setVisible(true);
    }

    private static void runCli(String[] args) {
        String mode = args[0];
        try {
            if ("dimensions".equalsIgnoreCase(mode)) {
                runDimensions(args);
                return;
            }
            if ("redact".equalsIgnoreCase(mode)) {
                runRedact(args);
                return;
            }

            printUsage();
            System.exit(1);
        } catch (Exception ex) {
            System.err.println("CLI error: " + ex.getMessage());
            System.exit(1);
        }
    }

    private static void runDimensions(String[] args) throws IOException {
        if (args.length < 2) {
            printUsage();
            throw new IOException("Missing input PDF path for dimensions mode.");
        }
        String inputPdf = args[1];
        float quality = parseOptionalQuality(args, 2);
        headless.openPdf(inputPdf, quality);

        List<Dimension> dimensions = headless.getPageDimensionsInPixels();
        for (int i = 0; i < dimensions.size(); i++) {
            Dimension dimension = dimensions.get(i);
            System.out.println((i + 1) + "," + dimension.width + "," + dimension.height);
        }
    }

    private static void runRedact(String[] args) throws IOException {
        if (args.length < 4) {
            printUsage();
            throw new IOException("Missing arguments for redact mode.");
        }

        String inputPdf = args[1];
        String outputPdf = args[2];
        Map<Integer, List<Rectangle>> redactionsByPage = new HashMap<>();
        Float quality = null;

        for (int i = 3; i < args.length; i++) {
            String token = args[i];
            if (token.startsWith("quality=")) {
                quality = Float.parseFloat(token.substring("quality=".length()));
                continue;
            }

            String[] parts = token.split(":");
            if (parts.length != 2) {
                throw new IOException("Invalid redaction token: " + token + ". Expected page:x,y,width,height");
            }

            int pageNumber = Integer.parseInt(parts[0]);
            String[] box = parts[1].split(",");
            if (box.length != 4) {
                throw new IOException("Invalid box token: " + parts[1] + ". Expected x,y,width,height");
            }

            int x = Integer.parseInt(box[0]);
            int y = Integer.parseInt(box[1]);
            int width = Integer.parseInt(box[2]);
            int height = Integer.parseInt(box[3]);

            if (width <= 0 || height <= 0) {
                throw new IOException("Redaction width and height must be greater than zero.");
            }

            int pageIndex = pageNumber - 1;
            redactionsByPage.computeIfAbsent(pageIndex, ignored -> new ArrayList<>())
                    .add(new Rectangle(x, y, width, height));
        }

        if (redactionsByPage.isEmpty()) {
            throw new IOException("No redactions were provided.");
        }

        headless.openPdf(inputPdf, quality == null ? 1.0f : quality);
        headless.applyPixelRedactions(redactionsByPage, outputPdf);
        System.out.println("Redacted PDF written to: " + outputPdf);
    }

    private static float parseOptionalQuality(String[] args, int index) throws IOException {
        if (args.length <= index) {
            return 1.0f;
        }
        String raw = args[index];
        if (!raw.startsWith("quality=")) {
            throw new IOException("Unexpected argument: " + raw + ". Optional quality argument must use quality=<value>.");
        }
        return Float.parseFloat(raw.substring("quality=".length()));
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  dimensions <input.pdf> [quality=0.1-1.0]");
        System.out.println("  redact <input.pdf> <output.pdf> <page:x,y,width,height>... [quality=0.1-1.0]");
    }
}
