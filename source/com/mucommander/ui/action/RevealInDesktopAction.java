
package com.mucommander.ui.action;

import com.mucommander.PlatformManager;
import com.mucommander.text.Translator;
import com.mucommander.ui.MainFrame;

import java.util.Hashtable;


/**
 * This action reveals the currently selected file or folder in the native Desktop's file manager
 * (e.g. Finder for Mac OS X, Explorer for Windows, etc...).
 *
 * @author Maxence Bernard
 */
public class RevealInDesktopAction extends MucoAction {

    public RevealInDesktopAction(MainFrame mainFrame, Hashtable properties) {
        super(mainFrame, properties);
        setLabel(Translator.get(getClass().getName()+".label", PlatformManager.getFileManagerName()));

        // Disable this action if the platform is not capable of opening files in the default file manager
        if(!PlatformManager.canOpenInFileManager())
            setEnabled(false);
    }

    public void performAction() {
        PlatformManager.openInFileManager(mainFrame.getActiveTable().getFolderPanel().getCurrentFolder());
    }
}
