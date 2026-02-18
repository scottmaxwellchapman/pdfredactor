package group.chapmanlaw.pdfredactor;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;


public class niceties {
private static final File neverShowFlagFile = new File("never_show.flag");

public static void intro() {
    // Check if the "never_show.flag" exists. If it does, don't show the message.
    if (neverShowFlagFile.exists()) {
        return;
    }

    // Define the long message
    String message = "THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS “AS IS” AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.";

    // Create a JTextArea to handle word wrapping
    JTextArea textArea = new JTextArea(message);
    textArea.setEditable(false);
    textArea.setWrapStyleWord(true);
    textArea.setLineWrap(true);
    textArea.setCaretPosition(0);

    // Display the message using JOptionPane
    int response = JOptionPane.showOptionDialog(null,
            new JScrollPane(textArea), // Add the JTextArea inside a JScrollPane
            "READ ME",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.INFORMATION_MESSAGE,
            null,
            new Object[]{"Ok, I understand.", "Never show this again."},
            "Ok, I understand.");

    // If the user selects "Never show again", create the "never_show.flag" file
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
    // Check if the "never_show.flag" exists. If it does, don't show the message.
    if (neverShowFlagFile2.exists()) {
        return;
    }

    // Define the second message
    String message = "READ ME: This works by clicking the top left and then the bottom right of the area you want redacted. If you do not follow this procedure, the redaction will not be applied. It takes one to two seconds to apply the redaction. This is an experimental application, so it may not work as expected, especially with large or complex PDFs.";

    // Create a JTextArea to handle word wrapping
    JTextArea textArea = new JTextArea(message);
    textArea.setEditable(false);
    textArea.setWrapStyleWord(true);
    textArea.setLineWrap(true);
    textArea.setCaretPosition(0);

    // Display the message using JOptionPane
    int response = JOptionPane.showOptionDialog(null,
            new JScrollPane(textArea), // Add the JTextArea inside a JScrollPane
            "READ ME",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.INFORMATION_MESSAGE,
            null,
            new Object[]{"Ok, I understand.", "Never show this again."},
            "Ok, I understand.");

    // If the user selects "Never show again", create the "never_show.flag" file
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
    // Advertisement message text
    try{
        //openBrowser(update.getExitUrl());
    }catch (Exception e){
        e.printStackTrace();
    }

    // Exit the application
    System.exit(0);
}


    // Helper method to open a URL in the default web browser
    private static void openBrowser(String url) {
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(new URI(url));
            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
            }
        }
    }
    
    public static Integer skipPrompt(){
        String response = JOptionPane.showInputDialog("Skip to page #");

        if (response == null) {
            return null;
        }

        String trimmedResponse = response.trim();
        if (trimmedResponse.isEmpty()) {
            return null;
        }

        try {
            return Integer.parseInt(trimmedResponse) - 1;
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
}
