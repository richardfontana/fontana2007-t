/*
 * Copyright (C) 2009 Lalit Pant <pant.lalit@gmail.com>
 *
 * The contents of this file are subject to the GNU General Public License
 * Version 3 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.gnu.org/copyleft/gpl.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 */
package net.kogics.kojo;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Toolkit;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import net.kogics.kojo.sprite.SpriteCanvas;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import org.openide.util.ImageUtilities;
import org.netbeans.api.settings.ConvertAsProperties;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(
    dtd="-//net.kogics.kojo//SCanvas//EN",
    autostore=false
)
public final class SCanvasTopComponent extends TopComponent {

    private static SCanvasTopComponent instance;
    /** path to the icon used by the component and its open action */
    static final String ICON_PATH = "net/kogics/kojo/turtle16.png";

    private static final String PREFERRED_ID = "SCanvasTopComponent";

    public void changeApplicationIcon() {
        // need to do this here because this does not work when done in the branding
        // module installer. something must be clobbering it between then and now
        // also - this needs to be done via invokeLater, otherwise it does not work
        // so the clobbering must actually be happening between now and the invokeLater time
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Frame frame = WindowManager.getDefault().getMainWindow();
                List<Image> images = new ArrayList<Image>();
                URL url = getClass().getResource("/images/turtle16.png");
                Image image = Toolkit.getDefaultToolkit().getImage(url);
                images.add(image);

                url = getClass().getResource("/images/turtle32.png");
                image = Toolkit.getDefaultToolkit().getImage(url);
                images.add(image);

                url = getClass().getResource("/images/turtle48.png");
                image = Toolkit.getDefaultToolkit().getImage(url);
                images.add(image);

                frame.setIconImages(images);
            }
        });
    }

    public SCanvasTopComponent() {
        changeApplicationIcon();
        initComponents();

        SpriteCanvas canvas = (SpriteCanvas)SpriteCanvas.instance();
        canvas.setPreferredSize(this.getPreferredSize());
        add(canvas, BorderLayout.CENTER);

        setName(NbBundle.getMessage(SCanvasTopComponent.class, "CTL_SCanvasTopComponent"));
        setToolTipText(NbBundle.getMessage(SCanvasTopComponent.class, "HINT_SCanvasTopComponent"));
        setIcon(ImageUtilities.loadImage(ICON_PATH, true));
//	putClientProperty(TopComponent.PROP_CLOSING_DISABLED, Boolean.TRUE);
//	putClientProperty(TopComponent.PROP_UNDOCKING_DISABLED, Boolean.TRUE);

    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setLayout(new java.awt.BorderLayout());
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
    /**
     * Gets default instance. Do not use directly: reserved for *.settings files only,
     * i.e. deserialization routines; otherwise you could get a non-deserialized instance.
     * To obtain the singleton instance, use {@link #findInstance}.
     */
    public static synchronized SCanvasTopComponent getDefault() {
        if (instance == null) {
            instance = new SCanvasTopComponent();
        }
        return instance;
    }

    /**
     * Obtain the SCanvasTopComponent instance. Never call {@link #getDefault} directly!
     */
    public static synchronized SCanvasTopComponent findInstance() {
        TopComponent win = WindowManager.getDefault().findTopComponent(PREFERRED_ID);
        if (win == null) {
            Logger.getLogger(SCanvasTopComponent.class.getName()).warning(
                    "Cannot find " + PREFERRED_ID + " component. It will not be located properly in the window system.");
            return getDefault();
        }
        if (win instanceof SCanvasTopComponent) {
            return (SCanvasTopComponent) win;
        }
        Logger.getLogger(SCanvasTopComponent.class.getName()).warning(
                "There seem to be multiple components with the '" + PREFERRED_ID + "' ID. That is a potential source of errors and unexpected behavior.");
        return getDefault();
    }

    @Override
    public int getPersistenceType() {
        return TopComponent.PERSISTENCE_NEVER;
    }

    void writeProperties(java.util.Properties p) {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
        // TODO store your settings
    }

    Object readProperties(java.util.Properties p) {
        if (instance == null) {
            instance = this;
        }
        instance.readPropertiesImpl(p);
        return instance;
    }

    private void readPropertiesImpl(java.util.Properties p) {
        String version = p.getProperty("version");
        // TODO read your settings according to their version
    }

    @Override
    protected String preferredID() {
        return PREFERRED_ID;
    }
}
