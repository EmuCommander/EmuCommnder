package com.mucommander.ui.action;

import com.mucommander.file.AbstractFile;
import com.mucommander.ui.MainFrame;
import com.mucommander.ui.table.FileTable;

import java.util.Hashtable;

/**
 * This action triggers in-table renaming of the currently selected file, if no file is marked.
 * If files are marked, it simply invokes 'Move dialog' just like {@link com.mucommander.ui.action}.
 *
 * @author Maxence Bernard
 */
public class RenameAction extends SelectedFileAction {

    public RenameAction(MainFrame mainFrame, Hashtable properties) {
        super(mainFrame, properties);
    }

    public void performAction() {
        FileTable activeTable = mainFrame.getActiveTable();
        AbstractFile selectedFile = activeTable.getSelectedFile(false);

        // Trigger in-table editing only if a file other than parent folder '..' is selected
        if(selectedFile!=null) {
            // Trigger in-table renaming
            activeTable.editCurrentFilename();
        }
    }
}
