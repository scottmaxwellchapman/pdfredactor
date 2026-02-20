package group.chapmanlaw.pdfredactor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

public class gui extends JFrame {
    private List<String> imagePaths;
    private int totalPages;
    private int currentPage = 0;
    private ImagePanel imagePanel;
    private JButton prevButton, nextButton, skipButton, zoomInButton, zoomOutButton, exitButton, undoButton;
    private JButton finishButton;
    private combiner mycombiner = new combiner();
    private JLabel pageLabel = new JLabel();
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
            dispose();
            return;
        }

        // Create Image Panel with reference to this (gui instance)
        imagePanel = new ImagePanel(loadCurrentPageImage(), this);
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
        finishButton.addActionListener(e -> finishAction());

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

    private BufferedImage loadCurrentPageImage() {
        try {
            return logic.getOrRenderPage(currentPage);
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error rendering page: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    private void skipPage() {
        currentPage = niceties.skipPrompt();
        imagePanel.setImage(loadCurrentPageImage());
        updateButtonState();
        pageLabel.setText("Page "+Integer.toString(getCurrentPage()+1)+" of "+logic.getTotalPages());
        this.repaint();
    }

    private void changePage(int direction) {
        currentPage += direction;
        imagePanel.setImage(loadCurrentPageImage());
        updateButtonState();
        pageLabel.setText("Page "+Integer.toString(getCurrentPage()+1)+" of "+logic.getTotalPages());
        this.repaint();
    }

    private void updateButtonState() {
        prevButton.setEnabled(currentPage > 0);
        nextButton.setEnabled(currentPage < totalPages - 1);
    }

    private void undo() {
        try {
            String currentImagePath = logic.getOrRenderImagePath(currentPage);
            redactor.undo(currentImagePath);
            logic.refreshPageFromDisk(currentPage);
            imagePanel.setImage(loadCurrentPageImage());
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error undoing redaction: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
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

    private void finishAction() {
        try {
            List<String> exportPaths = logic.getAllImagePathsForExport();
            if (exportPaths != null && !exportPaths.isEmpty()) {
                boolean saved = mycombiner.combine(exportPaths);
                if (!saved) {
                    JOptionPane.showMessageDialog(this, "Save cancelled. Continue editing or use Exit Without Saving.", "Cancelled", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                dispose();
                return;
            } else {
                JOptionPane.showMessageDialog(this, "No images to combine.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error preparing pages for export: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }

        resetCoordinates();  // Reset coordinates when finishing redaction
    }

    private void exitClick() {
        int choice = JOptionPane.showConfirmDialog(this,
                "Exit without saving your current redactions?",
                "Confirm Exit",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            niceties.exitAd();
            dispose();
        }
    }

    private void resetCoordinates() {
        coordinatesLabel.setText("X: , Y: ");
        imagePanel.resetSelection();
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
    private int[] currentHover = null;
    private gui parentGui;
    private double zoomFactor = 1.0;

    public ImagePanel(BufferedImage image, gui parentGui) {
        this.image = image;
        this.parentGui = parentGui;
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int clickX = e.getX() - drawX;
                int clickY = e.getY() - drawY;

                if (!isWithinImageBounds(clickX, clickY)) {
                    return;
                }

                int imageX = toImageX(clickX);
                int imageY = toImageY(clickY);

                parentGui.coordinatesLabel.setText("X: " + imageX + ", Y: " + imageY);

                if (firstClick == null) {
                    firstClick = new int[]{imageX, imageY};
                    currentHover = new int[]{imageX, imageY};
                    repaint();
                    return;
                }

                int x1 = Math.min(firstClick[0], imageX);
                int y1 = Math.min(firstClick[1], imageY);
                int x2 = Math.max(firstClick[0], imageX);
                int y2 = Math.max(firstClick[1], imageY);

                firstClick = null;
                currentHover = null;

                if (x1 == x2 || y1 == y2) {
                    JOptionPane.showMessageDialog(parentGui,
                            "Please click two different corners to define a redaction area.",
                            "Invalid area",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }

                try {
                    String imagePath = logic.getOrRenderImagePath(parentGui.getCurrentPage());
                    redactor.redact(imagePath, x1, y1, x2, y2);
                    logic.refreshPageFromDisk(parentGui.getCurrentPage());
                    setImage(logic.getOrRenderPage(parentGui.getCurrentPage()));
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                    resetSelection();
                }
            }
        });

        // MouseMotionListener to update coordinates and preview selection while moving the mouse
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                handleMouseMotion(e);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                handleMouseMotion(e);
            }

            private void handleMouseMotion(MouseEvent e) {
                int mouseX = e.getX() - drawX;
                int mouseY = e.getY() - drawY;

                if (!isWithinImageBounds(mouseX, mouseY)) {
                    return;
                }

                int imageX = toImageX(mouseX);
                int imageY = toImageY(mouseY);
                parentGui.coordinatesLabel.setText("X: " + imageX + ", Y: " + imageY);

                if (firstClick != null) {
                    currentHover = new int[]{imageX, imageY};
                    repaint();
                }
            }
        });
    }


    private boolean isWithinImageBounds(int x, int y) {
        if (image == null) {
            return false;
        }

        int scaledWidth = (int) (drawWidth * zoomFactor);
        int scaledHeight = (int) (drawHeight * zoomFactor);
        return x >= 0 && x <= scaledWidth && y >= 0 && y <= scaledHeight;
    }

    private int toImageX(int panelX) {
        if (image == null || drawWidth <= 0) {
            return 0;
        }
        int scaledWidth = (int) (drawWidth * zoomFactor);
        return clamp((int) ((panelX / (double) scaledWidth) * image.getWidth()), 0, image.getWidth() - 1);
    }

    private int toImageY(int panelY) {
        if (image == null || drawHeight <= 0) {
            return 0;
        }
        int scaledHeight = (int) (drawHeight * zoomFactor);
        return clamp((int) ((panelY / (double) scaledHeight) * image.getHeight()), 0, image.getHeight() - 1);
    }

    private int toPanelX(int imageX) {
        if (image == null) {
            return 0;
        }
        int scaledWidth = (int) (drawWidth * zoomFactor);
        return clamp((int) Math.round((imageX / (double) image.getWidth()) * scaledWidth), 0, scaledWidth);
    }

    private int toPanelY(int imageY) {
        if (image == null) {
            return 0;
        }
        int scaledHeight = (int) (drawHeight * zoomFactor);
        return clamp((int) Math.round((imageY / (double) image.getHeight()) * scaledHeight), 0, scaledHeight);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    public void setImage(BufferedImage newImage) {
        this.image = newImage;
        resetSelection();
        updatePreferredSize();
        repaint();
    }

    public void resetSelection() {
        firstClick = null;
        currentHover = null;
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
        if (image == null) {
            return;
        }
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

            if (firstClick != null && currentHover != null) {
                int previewX1 = toPanelX(firstClick[0]) + drawX;
                int previewY1 = toPanelY(firstClick[1]) + drawY;
                int previewX2 = toPanelX(currentHover[0]) + drawX;
                int previewY2 = toPanelY(currentHover[1]) + drawY;

                int previewX = Math.min(previewX1, previewX2);
                int previewY = Math.min(previewY1, previewY2);
                int previewWidth = Math.abs(previewX2 - previewX1);
                int previewHeight = Math.abs(previewY2 - previewY1);

                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setColor(new Color(255, 105, 180, 90));
                    g2.fillRect(previewX, previewY, previewWidth, previewHeight);
                    g2.setColor(new Color(255, 20, 147));
                    g2.drawRect(previewX, previewY, previewWidth, previewHeight);
                } finally {
                    g2.dispose();
                }
            }

            g.setColor(Color.BLUE);
            g.drawRect(drawX, drawY, (int)(drawWidth * zoomFactor), (int)(drawHeight * zoomFactor));
        }
    }
}
