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

import java.awt.Rectangle;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JViewport;
import javax.swing.text.DefaultEditorKit;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.util.actions.SystemAction;
import org.openide.windows.IOContainer;
import org.openide.windows.IOContainer.CallBacks;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(dtd = "-//net.kogics.kojo//Output//EN",
autostore = false)
public final class OutputTopComponent extends TopComponent implements IOContainer.Provider {

    private static OutputTopComponent instance;
    /** path to the icon used by the component and its open action */
//    static final String ICON_PATH = "SET/PATH/TO/ICON/HERE";
    private static final String PREFERRED_ID = "OutputTopComponent";
    JComponent ioComp;
    CallBacks ioCb;
    JEditorPane outputPane;

    public

     OutputTopComponent() {
        initComponents();

        // Disable cut key
        Object cutKey = SystemAction.get(org.openide.actions.CutAction.class).getActionMapKey();
        final Action cutAction = new DefaultEditorKit.CutAction();
        cutAction.setEnabled(false);
        getActionMap().put(cutKey, cutAction);

        setName(NbBundle.getMessage(OutputTopComponent.class, "CTL_OutputTopComponent"));
        setToolTipText(NbBundle.getMessage(OutputTopComponent.class, "HINT_OutputTopComponent"));
//        setIcon(ImageUtilities.loadImage(ICON_PATH, true));
        putClientProperty(TopComponent.PROP_CLOSING_DISABLED, Boolean.TRUE);
//        putClientProperty(TopComponent.PROP_DRAGGING_DISABLED, Boolean.TRUE);
//        putClientProperty(TopComponent.PROP_MAXIMIZATION_DISABLED, Boolean.TRUE);
//        putClientProperty(TopComponent.PROP_SLIDING_DISABLED, Boolean.TRUE);
//        putClientProperty(TopComponent.PROP_UNDOCKING_DISABLED, Boolean.TRUE);
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
    public static synchronized OutputTopComponent getDefault() {
        if (instance == null) {
            instance = new OutputTopComponent();
        }
        return instance;
    }

    /**
     * Obtain the OutputTopComponent instance. Never call {@link #getDefault} directly!
     */
    public static synchronized OutputTopComponent findInstance() {
        TopComponent win = WindowManager.getDefault().findTopComponent(PREFERRED_ID);
        if (win == null) {
            Logger.getLogger(OutputTopComponent.class.getName()).warning(
                    "Cannot find " + PREFERRED_ID + " component. It will not be located properly in the window system.");
            return getDefault();
        }
        if (win instanceof OutputTopComponent) {
            return (OutputTopComponent) win;
        }
        Logger.getLogger(OutputTopComponent.class.getName()).warning(
                "There seem to be multiple components with the '" + PREFERRED_ID
                + "' ID. That is a potential source of errors and unexpected behavior.");
        return getDefault();
    }

    @Override
    public int getPersistenceType() {
        return TopComponent.PERSISTENCE_ALWAYS;
    }

    public void add(JComponent comp, CallBacks cb) {
        if (ioComp != null) {
            remove(ioComp);
            if (ioCb != null) {
                ioCb.closed();
            }
        }
        ioComp = comp;
        ioCb = cb;
        add(ioComp);

        JViewport vp = (JViewport) ((JComponent) ioComp.getComponent(0)).getComponent(0);
        outputPane = (JEditorPane) vp.getComponent(0);

        // Link local actions to Menu
        Object findKey = SystemAction.get(org.openide.actions.FindAction.class).getActionMapKey();
        Action findAction = comp.getActionMap().get("Find...");
        getActionMap().put(findKey, findAction);

        Object copyKey = SystemAction.get(org.openide.actions.CopyAction.class).getActionMapKey();
        Action copyAction = comp.getActionMap().get("Copy");
        getActionMap().put(copyKey, copyAction);

        validate();
    }

    public JComponent getSelected() {
        return ioComp;
    }
    boolean activated;

    public boolean isActivated() {
        return activated;
    }

    @Override
    protected void componentActivated() {
        super.componentActivated();
        activated = true;
        if (ioCb != null) {
            ioCb.activated();
            ioComp.requestFocusInWindow();
        }
    }

    @Override
    protected void componentDeactivated() {
        super.componentDeactivated();
        activated = false;
        if (ioCb != null) {
            ioCb.deactivated();
        }
    }

    public boolean isCloseable(JComponent comp) {
        return false;
    }

    public void remove(JComponent comp) {
        if (comp == ioComp) {
            ioComp = null;
            ioCb = null;
        }
    }

    public void select(JComponent comp) {
    }

    public void setIcon(JComponent comp, Icon icon) {
    }

    public void setTitle(JComponent comp, String name) {
    }

    public void setToolTipText(JComponent comp, String text) {
    }

    public void setToolbarActions(JComponent comp, Action[] toolbarActions) {
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

    public void switchFocusToCodeEditor() {
        CodeEditorTopComponent.findInstance().requestActive();
    }

    public void scrollToEnd() {
        if (outputPane == null) {
            // we get here in the unit tests. need to look more into that
            return;
        }
        
        int len = outputPane.getDocument().getLength();
        outputPane.setCaretPosition(len);
    }
}
