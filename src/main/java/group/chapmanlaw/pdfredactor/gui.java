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
            System.exit(0);
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
                mycombiner.combine(exportPaths);
                JOptionPane.showMessageDialog(this, "PDF created successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
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
    private Rectangle dragSelection = null;
    private Rectangle pendingSelection = null;
    private Point dragStart = null;
    private gui parentGui;
    private double zoomFactor = 1.0;
    private int displayedImageWidth;
    private int displayedImageHeight;

    public ImagePanel(BufferedImage image, gui parentGui) {
        this.image = image;
        this.parentGui = parentGui;

        setFocusable(true);

        InputMap inputMap = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "applySelection");
        actionMap.put("applySelection", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                applyPendingSelection();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancelSelection");
        actionMap.put("cancelSelection", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancelSelection();
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
<<<<<<< codex/replace-two-click-flow-with-click-drag-selection
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                Point imagePoint = toImagePoint(e.getX(), e.getY());
                if (imagePoint != null) {
                    dragStart = imagePoint;
                    dragSelection = new Rectangle(dragStart.x, dragStart.y, 0, 0);
                    pendingSelection = null;
                    repaint();
=======
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
                            try {
                                String imagePath = logic.getOrRenderImagePath(parentGui.getCurrentPage());
                                redactor.redact(imagePath, x1, y1, x2, y2);
                                logic.refreshPageFromDisk(parentGui.getCurrentPage());
                                setImage(logic.getOrRenderPage(parentGui.getCurrentPage()));
                            } catch (IOException ioException) {
                                ioException.printStackTrace();
                            }
                        }

                        firstClick = null;
                    }
>>>>>>> master
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (dragStart == null || dragSelection == null) {
                    return;
                }

                updateSelection(e.getX(), e.getY());
                if (dragSelection.width > 0 && dragSelection.height > 0) {
                    pendingSelection = new Rectangle(dragSelection);
                    parentGui.coordinatesLabel.setText(
                            "X: " + pendingSelection.x + ", Y: " + pendingSelection.y
                                    + " | W: " + pendingSelection.width + ", H: " + pendingSelection.height
                                    + " (Enter=apply, Esc=cancel)"
                    );
                } else {
                    parentGui.coordinatesLabel.setText("X: , Y: ");
                }

                dragStart = null;
                dragSelection = null;
                repaint();
            }
        });

        // MouseMotionListener to update the coordinates while moving the mouse
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                Point imagePoint = toImagePoint(e.getX(), e.getY());
                if (imagePoint != null && pendingSelection == null) {
                    parentGui.coordinatesLabel.setText("X: " + imagePoint.x + ", Y: " + imagePoint.y + " | W: 0, H: 0");
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragStart == null) {
                    return;
                }

                updateSelection(e.getX(), e.getY());
                if (dragSelection != null) {
                    parentGui.coordinatesLabel.setText(
                            "X: " + dragSelection.x + ", Y: " + dragSelection.y
                                    + " | W: " + dragSelection.width + ", H: " + dragSelection.height
                    );
                    repaint();
                }
            }
        });
    }

<<<<<<< codex/replace-two-click-flow-with-click-drag-selection
    private void updateSelection(int panelX, int panelY) {
        Point imagePoint = toImagePoint(panelX, panelY);
        if (imagePoint == null) {
            return;
        }

        int x = Math.min(dragStart.x, imagePoint.x);
        int y = Math.min(dragStart.y, imagePoint.y);
        int width = Math.abs(imagePoint.x - dragStart.x);
        int height = Math.abs(imagePoint.y - dragStart.y);
        dragSelection = new Rectangle(x, y, width, height);
    }

    private Point toImagePoint(int panelX, int panelY) {
        if (image == null) {
            return null;
        }

        if (panelX < drawX || panelY < drawY || panelX > drawX + displayedImageWidth || panelY > drawY + displayedImageHeight) {
            return null;
        }

        int clampedX = Math.max(drawX, Math.min(panelX, drawX + displayedImageWidth));
        int clampedY = Math.max(drawY, Math.min(panelY, drawY + displayedImageHeight));

        double xRatio = (clampedX - drawX) / (double) displayedImageWidth;
        double yRatio = (clampedY - drawY) / (double) displayedImageHeight;

        int imageX = Math.min(image.getWidth() - 1, Math.max(0, (int) (xRatio * image.getWidth())));
        int imageY = Math.min(image.getHeight() - 1, Math.max(0, (int) (yRatio * image.getHeight())));
        return new Point(imageX, imageY);
    }

    private void applyPendingSelection() {
        if (pendingSelection == null || pendingSelection.width <= 0 || pendingSelection.height <= 0) {
            return;
        }

        String imagePath = logic.getImagePaths().get(parentGui.getCurrentPage());
        int x1 = pendingSelection.x;
        int y1 = pendingSelection.y;
        int x2 = pendingSelection.x + pendingSelection.width;
        int y2 = pendingSelection.y + pendingSelection.height;

        redactor.redact(imagePath, x1, y1, x2, y2);
        pendingSelection = null;
        parentGui.coordinatesLabel.setText("X: , Y: ");
        setImage(loadImage(imagePath));
    }

    private void cancelSelection() {
        dragStart = null;
        dragSelection = null;
        pendingSelection = null;
        parentGui.coordinatesLabel.setText("X: , Y: ");
        repaint();
    }

    private BufferedImage loadImage(String path) {
        try {
            return ImageIO.read(new File(path));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

=======
>>>>>>> master
    public void setImage(BufferedImage newImage) {
        this.image = newImage;
        cancelSelection();
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
            displayedImageWidth = (int)(drawWidth * zoomFactor);
            displayedImageHeight = (int)(drawHeight * zoomFactor);

            g.drawImage(image, drawX, drawY, displayedImageWidth, displayedImageHeight, this);

            g.setColor(Color.BLUE);
            g.drawRect(drawX, drawY, displayedImageWidth, displayedImageHeight);

            Graphics2D g2d = (Graphics2D) g.create();
            Rectangle overlayRect = dragSelection != null ? dragSelection : pendingSelection;
            if (overlayRect != null) {
                int overlayX = drawX + (int) ((overlayRect.x / (double) image.getWidth()) * displayedImageWidth);
                int overlayY = drawY + (int) ((overlayRect.y / (double) image.getHeight()) * displayedImageHeight);
                int overlayWidth = (int) ((overlayRect.width / (double) image.getWidth()) * displayedImageWidth);
                int overlayHeight = (int) ((overlayRect.height / (double) image.getHeight()) * displayedImageHeight);

                g2d.setColor(new Color(0, 102, 255, 80));
                g2d.fillRect(overlayX, overlayY, overlayWidth, overlayHeight);
                g2d.setColor(new Color(0, 102, 255));
                g2d.drawRect(overlayX, overlayY, overlayWidth, overlayHeight);
            }

            if (pendingSelection != null) {
                g2d.setColor(new Color(0, 0, 0, 170));
                g2d.fillRoundRect(drawX + 10, drawY + 10, 220, 24, 8, 8);
                g2d.setColor(Color.WHITE);
                g2d.drawString("Enter: apply  Esc: cancel", drawX + 18, drawY + 27);
            }
            g2d.dispose();
        }
    }
}
