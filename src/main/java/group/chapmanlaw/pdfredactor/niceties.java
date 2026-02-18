package group.chapmanlaw.pdfredactor;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class niceties {
    private static final File neverShowFlagFile = new File("never_show.flag");

    public static void intro() {
        if (neverShowFlagFile.exists()) {
            return;
        }

        String message = "THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS “AS IS” AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.";

        JTextArea textArea = new JTextArea(message);
        textArea.setEditable(false);
        textArea.setWrapStyleWord(true);
        textArea.setLineWrap(true);
        textArea.setCaretPosition(0);

        int response = JOptionPane.showOptionDialog(null,
                new JScrollPane(textArea),
                "READ ME",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                new Object[]{"Ok, I understand.", "Never show this again."},
                "Ok, I understand.");

        if (response == JOptionPane.NO_OPTION) {
            try {
                if (neverShowFlagFile.createNewFile()) {
                    System.out.println("Preference to never show again saved.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static final File neverShowFlagFile2 = new File("never_show2.flag");

    public static void intro2() {
        if (neverShowFlagFile2.exists()) {
            return;
        }

        String message = "READ ME: This works by clicking the top left and then the bottom right of the area you want redacted. If you do not follow this procedure, the redaction will not be applied. It takes one to two seconds to apply the redaction. This is an experimental application, so it may not work as expected, especially with large or complex PDFs.";

        JTextArea textArea = new JTextArea(message);
        textArea.setEditable(false);
        textArea.setWrapStyleWord(true);
        textArea.setLineWrap(true);
        textArea.setCaretPosition(0);

        int response = JOptionPane.showOptionDialog(null,
                new JScrollPane(textArea),
                "READ ME",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                new Object[]{"Ok, I understand.", "Never show this again."},
                "Ok, I understand.");

        if (response == JOptionPane.NO_OPTION) {
            try {
                if (neverShowFlagFile2.createNewFile()) {
                    System.out.println("Preference to never show again saved.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void exitAd() {
        try {
            // openBrowser(update.getExitUrl());
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.exit(0);
    }

    private static void openBrowser(String url) {
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(new URI(url));
            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
            }
        }
    }

    public static int skipPrompt() {
        String userInput = JOptionPane.showInputDialog("Skip to page #");
        if (userInput == null) {
            return 0;
        }

        try {
            int out = Integer.parseInt(userInput.trim()) - 1;
            int maxIndex = Math.max(0, logic.getTotalPages() - 1);
            if (out < 0) {
                return 0;
            }
            return Math.min(out, maxIndex);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(null,
                    "Invalid page number. Staying on the first page.",
                    "Invalid page",
                    JOptionPane.WARNING_MESSAGE);
            return 0;
        }
    }
}
