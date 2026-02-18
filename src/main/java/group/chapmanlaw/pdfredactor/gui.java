package group.chapmanlaw.pdfredactor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import javax.imageio.ImageIO;

public class gui extends JFrame {
    private List<String> imagePaths;
    private int totalPages;
    private int currentPage = 0;
    private ImagePanel imagePanel;
    private JButton prevButton, nextButton, skipButton, zoomInButton, zoomOutButton, exitButton, undoButton;
    private JButton finishButton;
    private combiner mycombiner = new combiner();
    private JLabel pageLabel = new JLabel();
    private JLabel instructionLabel = new JLabel();
    JLabel coordinatesLabel = new JLabel("X: , Y: ");  // Label to display coordinates

    public gui() {
        setTitle("PDF Redactor Viewer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLayout(new BorderLayout());

        // Load global image paths from Logic
        imagePaths = logic.getImagePaths();
        totalPages = logic.getTotalPages();

        pageLabel.setText("Page "+Integer.toString(getCurrentPage()+1)+" of "+logic.getTotalPages());

        if (imagePaths.isEmpty() || totalPages == 0) {
            JOptionPane.showMessageDialog(this, "No images loaded. Run PDF conversion first.", "Error", JOptionPane.ERROR_MESSAGE);
            niceties.exitAd();
            System.exit(0);
        }

        // Create Image Panel with reference to this (gui instance)
        imagePanel = new ImagePanel(loadImage(imagePaths.get(currentPage)), this);
        JScrollPane scrollPane = new JScrollPane(imagePanel);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        add(scrollPane, BorderLayout.CENTER);

        // Create Navigation Panel
        JPanel navPanel = new JPanel();
        prevButton = new JButton("Previous");
        nextButton = new JButton("Next");
        skipButton = new JButton("Go to Page #");
        exitButton = new JButton("Exit Without Saving");
        undoButton = new JButton("Undo Redactions for this Page");
        prevButton.addActionListener(e -> changePage(-1));
        nextButton.addActionListener(e -> changePage(1));
        skipButton.addActionListener(e -> skipPage());
        exitButton.addActionListener(e -> exitClick());
        undoButton.addActionListener(e -> undo());
        
        // Zoom buttons
        zoomInButton = new JButton("Zoom In");
        zoomInButton.addActionListener(e -> zoomIn());
        zoomOutButton = new JButton("Zoom Out");
        zoomOutButton.addActionListener(e -> zoomOut());

        finishButton = new JButton("Finish");
        finishButton.addActionListener(e -> finishAction(imagePaths));

        navPanel.add(undoButton);
        navPanel.add(prevButton);
        navPanel.add(pageLabel);
        navPanel.add(nextButton);
        navPanel.add(skipButton);
        navPanel.add(zoomInButton);
        navPanel.add(zoomOutButton);
        navPanel.add(finishButton);
        navPanel.add(exitButton);
        navPanel.add(coordinatesLabel);  // Add coordinates label to the navigation panel
        add(navPanel, BorderLayout.SOUTH);

        instructionLabel.setText("Click top-left then bottom-right to redact. Undo to reset page. Use Zoom In/Out as needed. Click Finish to save PDF.");
        instructionLabel.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        add(instructionLabel, BorderLayout.NORTH);

        updateButtonState();
        setVisible(true);
        imagePanel.zoomIn();

        // Add Escape Key Listener to reset coordinates when pressed
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "resetCoordinates");
        getRootPane().getActionMap().put("resetCoordinates", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resetCoordinates();
            }
        });
    }

    private BufferedImage loadImage(String path) {
        try {
            return ImageIO.read(new File(path));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void skipPage() {
        Integer requestedPage = niceties.skipPrompt();
        if (requestedPage == null) {
            JOptionPane.showMessageDialog(this, "Please enter a page number between 1 and " + totalPages + ".", "Invalid Page", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (requestedPage < 0 || requestedPage >= totalPages) {
            JOptionPane.showMessageDialog(this, "Page number must be between 1 and " + totalPages + ".", "Invalid Page", JOptionPane.ERROR_MESSAGE);
            return;
        }

        currentPage = requestedPage;
        imagePanel.setImage(loadImage(imagePaths.get(currentPage)));
        updateButtonState();
        pageLabel.setText("Page "+Integer.toString(getCurrentPage()+1)+" of "+logic.getTotalPages());
        this.repaint();
    }

    private void changePage(int direction) {
        currentPage += direction;
        imagePanel.setImage(loadImage(imagePaths.get(currentPage)));
        updateButtonState();
        pageLabel.setText("Page "+Integer.toString(getCurrentPage()+1)+" of "+logic.getTotalPages());
        this.repaint();
    }

    private void updateButtonState() {
        prevButton.setEnabled(currentPage > 0);
        nextButton.setEnabled(currentPage < totalPages - 1);
    }

    private void undo() {
        redactor.undo(imagePaths.get(currentPage));
        imagePanel.setImage(loadImage(imagePaths.get(currentPage)));
        updateButtonState();
        pageLabel.setText("Page "+Integer.toString(getCurrentPage()+1)+" of "+logic.getTotalPages());
        this.repaint();
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new gui());
    }

    private void finishAction(List<String> imagePaths) {
        if (imagePaths != null && !imagePaths.isEmpty()) {
            // Assuming combiner is an object that has the combine method
            mycombiner.combine(imagePaths);
            JOptionPane.showMessageDialog(this, "PDF created successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "No images to combine.", "Error", JOptionPane.ERROR_MESSAGE);
        }

        resetCoordinates();  // Reset coordinates when finishing redaction
    }

    private void exitClick() {
        niceties.exitAd();
    }

    private void resetCoordinates() {
        coordinatesLabel.setText("X: , Y: ");
    }

    // Zoom In and Zoom Out methods
    private void zoomIn() {
        imagePanel.zoomIn();
    }

    private void zoomOut() {
        imagePanel.zoomOut();
    }
}

class ImagePanel extends JPanel {
    private BufferedImage image;
    private int drawX, drawY, drawWidth, drawHeight;
    private int[] firstClick = null;
    private gui parentGui;
    private double zoomFactor = 1.0;

    public ImagePanel(BufferedImage image, gui parentGui) {
        this.image = image;
        this.parentGui = parentGui;
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int clickX = (int) ((e.getX() - drawX) / zoomFactor);
                int clickY = (int) ((e.getY() - drawY) / zoomFactor);

                if (clickX >= 0 && clickX <= drawWidth / zoomFactor && clickY >= 0 && clickY <= drawHeight / zoomFactor) {
                    int imageX = (int) ((clickX / (double) drawWidth) * image.getWidth());
                    int imageY = (int) ((clickY / (double) drawHeight) * image.getHeight());

                    parentGui.coordinatesLabel.setText("X: " + imageX + ", Y: " + imageY);  // Update coordinates in real time

                    if (firstClick == null) {
                        firstClick = new int[]{imageX, imageY};
                    } else {
                        int x1 = firstClick[0], y1 = firstClick[1];
                        int x2 = imageX, y2 = imageY;

                        if (x2 > x1 && y2 > y1) {
                            String imagePath = logic.getImagePaths().get(parentGui.getCurrentPage());
                            redactor.redact(imagePath, x1, y1, x2, y2);
                            setImage(loadImage(imagePath));
                        }

                        firstClick = null;
                    }
                }
            }
        });

        // MouseMotionListener to update the coordinates while moving the mouse
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int mouseX = (int) ((e.getX() - drawX) / zoomFactor);
                int mouseY = (int) ((e.getY() - drawY) / zoomFactor);

                if (mouseX >= 0 && mouseX <= drawWidth / zoomFactor && mouseY >= 0 && mouseY <= drawHeight / zoomFactor) {
                    int imageX = (int) ((mouseX / (double) drawWidth) * image.getWidth());
                    int imageY = (int) ((mouseY / (double) drawHeight) * image.getHeight());

                    parentGui.coordinatesLabel.setText("X: " + imageX + ", Y: " + imageY);  // Update coordinates on mouse move
                }
            }
        });
    }

    private BufferedImage loadImage(String path) {
        try {
            return ImageIO.read(new File(path));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void setImage(BufferedImage newImage) {
        this.image = newImage;
        updatePreferredSize();
        repaint();
    }

    public void zoomIn() {
        if (zoomFactor < 3.0) {
            zoomFactor += 0.1;
            updatePreferredSize();
            revalidate();
            repaint();
        }
    }

    public void zoomOut() {
        if (zoomFactor > 0.5) {
            zoomFactor -= 0.1;
            updatePreferredSize();
            revalidate();
            repaint();
        }
    }

    private void updatePreferredSize() {
        int width = (int) (image.getWidth() * zoomFactor);
        int height = (int) (image.getHeight() * zoomFactor);
        setPreferredSize(new Dimension(width, height));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (image != null) {
            int panelWidth = getWidth();
            int panelHeight = getHeight();

            double imageAspect = (double) image.getWidth() / image.getHeight();
            double panelAspect = (double) panelWidth / panelHeight;

            if (panelAspect > imageAspect) {
                drawHeight = panelHeight;
                drawWidth = (int) (drawHeight * imageAspect);
            } else {
                drawWidth = panelWidth;
                drawHeight = (int) (drawWidth / imageAspect);
            }

            drawX = (panelWidth - drawWidth) / 2;
            drawY = (panelHeight - drawHeight) / 2;

            g.drawImage(image, drawX, drawY, (int)(drawWidth * zoomFactor), (int)(drawHeight * zoomFactor), this);

            g.setColor(Color.BLUE);
            g.drawRect(drawX, drawY, (int)(drawWidth * zoomFactor), (int)(drawHeight * zoomFactor));
        }
    }
}
