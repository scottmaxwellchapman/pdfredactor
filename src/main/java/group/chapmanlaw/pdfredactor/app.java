package group.chapmanlaw.pdfredactor;

import javax.swing.JFrame;

public class app {

    public static void main(String[] args) {
        
        update.checkForUpdates();
        niceties.intro();
        launch myLaunch = new launch();
        myLaunch.setLocationRelativeTo(null);
        myLaunch.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        myLaunch.setVisible(true);
    }
    
}
