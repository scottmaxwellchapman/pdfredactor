package group.chapmanlaw.pdfredactor;

public class app {

    public static void main(String[] args) {
        update.checkForUpdates();
        niceties.intro();
        try {
            WebServer.start();
        } catch (Exception exception) {
            exception.printStackTrace();
            System.exit(1);
        }
    }
}
