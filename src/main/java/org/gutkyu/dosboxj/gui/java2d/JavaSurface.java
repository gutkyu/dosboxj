package org.gutkyu.dosboxj.gui.java2d;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public final class JavaSurface extends Panel {

    private static final long serialVersionUID = -4145952450081961487L;

    public JavaSurface() {

    }

    //public void update(Graphics g){
    //    this.paint(g);
    //}

    public void paint(Graphics g) {
        if (!requestUpdate) {
            super.paint(g);
            return;
        }
        if (img == null) {
            super.paint(g);
            return;
        }
        if (targetArea == null) {
            targetArea = new Rectangle();
            targetArea.x = 0;
            targetArea.y = 0;
            targetArea.height = img.getHeight();
            targetArea.width = img.getWidth();
        }
        g.drawImage(img, targetArea.x, targetArea.y, targetArea.width, targetArea.height, null);

        super.paint(g);

        targetArea = null;
        requestUpdate = false;
    }

    private boolean requestUpdate = false;
    private BufferedImage img = null;
    // private Rectangle targetArea = null;
    private Rectangle targetArea = new Rectangle(0,0,480,640);

    public void newImage(int width, int height, int type) {
        img = new BufferedImage(width, height, type);
        this.setSize(width, height);
        this.setPreferredSize(new Dimension(width, height));
        repaint();
    }

    public boolean hasImage() {
        return img != null;
    }

    public int[] getIntPixels() {
        return ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
    }

    public int getPixelSize() {
        return img.getColorModel().getPixelSize();
    }

    public void requestUpdate(Rectangle targetArea) {
        // if (this.targetArea == null) {
        //     this.targetArea = targetArea;
        // } else {
        //     this.targetArea.x = Math.min(this.targetArea.x, targetArea.x);
        //     this.targetArea.y = Math.min(this.targetArea.y, targetArea.y);
        //     this.targetArea.height = Math.max(this.targetArea.height, targetArea.height);
        //     this.targetArea.width = Math.max(this.targetArea.width, targetArea.width);
        // }
        requestUpdate = true;
        repaint();
    }

}
