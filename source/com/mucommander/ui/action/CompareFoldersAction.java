package com.mucommander.ui.action;

import com.mucommander.file.AbstractFile;
import com.mucommander.ui.MainFrame;
import com.mucommander.ui.table.FileTable;
import com.mucommander.ui.table.FileTableModel;

import java.util.Hashtable;

/**
 * This action compares the content of the 2 MainFrame's file tables and marks the files that are different.
 *
 * @author Maxence Bernard
 */
public class CompareFoldersAction extends MucoAction {

    public CompareFoldersAction(MainFrame mainFrame, Hashtable properties) {
        super(mainFrame, properties);
    }

    public void performAction() {
        FileTable table1 = mainFrame.getFolderPanel1().getFileTable();
        FileTable table2 = mainFrame.getFolderPanel2().getFileTable();

        FileTableModel tableModel1 = table1.getFileTableModel();
        FileTableModel tableModel2 = table2.getFileTableModel();

        int nbFiles1 = tableModel1.getFileCount();
        int nbFiles2 = tableModel2.getFileCount();
        int fileIndex;
        String tempFileName;
        AbstractFile tempFile;
        for(int i=0; i<nbFiles1; i++) {
            tempFile = tableModel1.getFileAt(i);
            if(tempFile.isDirectory())
                continue;

            tempFileName = tempFile.getName();
            fileIndex = -1;
            for(int j=0; j<nbFiles2; j++)
                if (tableModel2.getFileAt(j).getName().equals(tempFileName)) {
                    fileIndex = j;
                    break;
                }
            if (fileIndex==-1 || tableModel2.getFileAt(fileIndex).getDate()<tempFile.getDate()) {
                tableModel1.setFileMarked(tempFile, true);
                table1.repaint();
            }
        }

        for(int i=0; i<nbFiles2; i++) {
            tempFile = tableModel2.getFileAt(i);
            if(tempFile.isDirectory())
                continue;

            tempFileName = tempFile.getName();
            fileIndex = -1;
            for(int j=0; j<nbFiles1; j++)
                if (tableModel1.getFileAt(j).getName().equals(tempFileName)) {
                    fileIndex = j;
                    break;
                }
            if (fileIndex==-1 || tableModel1.getFileAt(fileIndex).getDate()<tempFile.getDate()) {
                tableModel2.setFileMarked(tempFile, true);
                table2.repaint();
            }
        }

        // Notify registered listeners that currently marked files have changed on the file tables
        table1.fireMarkedFilesChangedEvent();
        table2.fireMarkedFilesChangedEvent();
    }
}
