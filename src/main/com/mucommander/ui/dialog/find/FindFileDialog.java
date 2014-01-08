/*
 * This file is part of muCommander, http://www.mucommander.com
 * Copyright (C) 2002-2012 Maxence Bernard
 *
 * muCommander is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * muCommander is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mucommander.ui.dialog.find;

import static com.mucommander.commons.runtime.OsFamily.WINDOWS;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mucommander.commons.file.AbstractFile;
import com.mucommander.commons.file.FileFactory;
import com.mucommander.commons.runtime.OsFamily;
import com.mucommander.process.AbstractProcess;
import com.mucommander.process.ProcessListener;
import com.mucommander.shell.Shell;
import com.mucommander.text.Translator;
import com.mucommander.ui.action.ActionProperties;
import com.mucommander.ui.action.impl.FindFileAction;
import com.mucommander.ui.dialog.DialogToolkit;
import com.mucommander.ui.dialog.FocusDialog;
import com.mucommander.ui.icon.SpinningDial;
import com.mucommander.ui.layout.XBoxPanel;
import com.mucommander.ui.layout.YBoxPanel;
import com.mucommander.ui.main.MainFrame;

/**
 * Dialog used to search for files.
 * <p>
 * Creates and displays a new dialog allowing the user to input a file name pattern
 * and search in current directory and subdirectories for matching files.
 * </p>
 * @author pawel.halicz
 */
//PH FindFileDialog
public class FindFileDialog extends FocusDialog implements ActionListener, ProcessListener {
	private static final Logger LOGGER = LoggerFactory.getLogger(FindFileDialog.class);
	
    // - UI components -------------------------------------------------------------------
    // -----------------------------------------------------------------------------------
    /** Main frame this dialog depends on. */
    private MainFrame mainFrame;
    /** Input field for filename pattern. */
    private JTextField patternText;
    /** Input field for content text. */
    private JTextField contentText;
    /** Regexp check box. */
    private JCheckBox isRegexp;
    /** Run/stop button. */
    private JButton       runStopButton;
    /** Cancel button. */
    private JButton       cancelButton;
    /** List with found files. */
    private JList foundFilesList;
    /** Found files list data model. */
    private DefaultListModel foundFilesListModel;
    /** Last fragment of process output - not followed by new list so it might be beginning of next line. */ 
    private String foundFilesLastFragment = null;
    /** When first result is displayed in list switch focus to this list - but only once for each search so user can change it. */
    private boolean foundFilesLastFocusSet = false;
    
    /** Used to let the user known that the command is still running. */
    private SpinningDial  dial;

    /** Process currently running, <code>null</code> if none. */
    private AbstractProcess currentProcess;
    
    // - Misc. class variables -----------------------------------------------------------
    // -----------------------------------------------------------------------------------
    /** Minimum dimensions for the dialog. */
    private final static Dimension MINIMUM_DIALOG_DIMENSION = new Dimension(600, 400);



    // - Initialisation ------------------------------------------------------------------
    // -----------------------------------------------------------------------------------
    private YBoxPanel createInputArea() {
        YBoxPanel panel = new YBoxPanel();
        
    	XBoxPanel p = new XBoxPanel();
        p.add(new JLabel(Translator.get("find_file_dialog.pattern_input")));
        p.addSpace(5);
        p.add(isRegexp = new JCheckBox(Translator.get("find_file_dialog.use_regexp"), false));
        panel.add(p);
        
    	panel.add(patternText = new JTextField());
        patternText.setEnabled(true);
        patternText.setToolTipText(
        	Translator.get(
        		WINDOWS.equals(OsFamily.getCurrent())  
	        	? "find_file_dialog.pattern_input_tooltip_win" 
	        	: "find_file_dialog.pattern_input_tooltip_linux"));
        
        panel.add(new JLabel(Translator.get("find_file_dialog.content_input")));

        panel.add(contentText = new JTextField());
    	contentText.setEnabled(true);                
        
    	XBoxPanel p2 = new XBoxPanel();
        p2.add(new JLabel(Translator.get("find_file_dialog.found_files")));
        p2.add(new JLabel(dial = new SpinningDial()));
        panel.add(p2);
        
        return panel;
    }
    
   
    /**
     * Creates the list of found files area.
     * @return a scroll pane containing the list of found files.
     */
    private JPanel createOutputList() {
        YBoxPanel panel = new YBoxPanel();
    	
        foundFilesListModel = new DefaultListModel();
        foundFilesList = new JList(foundFilesListModel);
        foundFilesList.setLayoutOrientation(JList.VERTICAL);
        foundFilesList.setVisibleRowCount(-1);
        foundFilesList.setFocusable(true);
        foundFilesList.setToolTipText(Translator.get("find_file_dialog.ouput_list_tooltip"));

        foundFilesList.addMouseListener(new MouseAdapter(){
        	@Override
        	public void mouseClicked(MouseEvent e) {
        		if( e.getClickCount() == 2 ){
        			navigateToSelectedFile();
        		}
        	}
        });
        foundFilesList.getInputMap().put(KeyStroke.getKeyStroke("SPACE"), "space pressed");
        foundFilesList.getActionMap().put("space pressed", new AbstractAction(){
        	@Override
        	public void actionPerformed(ActionEvent e) {
        		navigateToSelectedFile();
        	}
        });

        // Creates a scroll pane on the output area.
        JScrollPane listScroller = new JScrollPane(foundFilesList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        panel.add(listScroller);
        
        return panel;
    }
    
    private void navigateToSelectedFile(){
    	String selectedFile = (String)foundFilesList.getSelectedValue();
    	if( selectedFile == null ){
    		return;
    	}
    	selectedFile = selectedFile.trim();
		if( selectedFile.startsWith(".")){
			selectedFile = mainFrame.getActivePanel().getCurrentFolder().getAbsolutePath()
					+ selectedFile.substring(2);
		}
		
		AbstractFile selectedAF = FileFactory.getFile(selectedFile);
		AbstractFile selectedAFFolder = selectedAF.getParent();
		if( selectedAF.isDirectory() ){
			selectedAFFolder = selectedAF;
			selectedAF = null;
		}
		
		mainFrame.getActivePanel().tryChangeCurrentFolder(selectedAFFolder, selectedAF, true);
		FindFileDialog.this.close();

    }
   /**
     * Creates a panel containing the dialog's buttons.
     * @return a panel containing the dialog's buttons.
     */
    private XBoxPanel createButtonsArea() {
        // Buttons panel
        XBoxPanel buttonsPanel;

        buttonsPanel = new XBoxPanel();
        // 'Run / stop' and 'Cancel' buttons.
        buttonsPanel.add(DialogToolkit.createOKCancelPanel(
                runStopButton = new JButton(Translator.get("find_file_dialog.run")),
                cancelButton  = new JButton(Translator.get("cancel")),
                getRootPane(),
                this));

        return buttonsPanel;
    }

    /**
     * Creates and displays a new FindFileDialog.
     * @param mainFrame the main frame this dialog is attached to.
     */
    public FindFileDialog(MainFrame mainFrame) {
        super(mainFrame, ActionProperties.getActionLabel(FindFileAction.Descriptor.ACTION_ID), mainFrame);
        this.mainFrame = mainFrame;
		
        // Initializes the dialog's UI.
        Container contentPane = getContentPane();
        contentPane.add(createInputArea(), BorderLayout.NORTH);
        contentPane.add(createOutputList(), BorderLayout.CENTER);
        contentPane.add(createButtonsArea(), BorderLayout.SOUTH);

        // Sets default items.
        setInitialFocusComponent(patternText);
        getRootPane().setDefaultButton(runStopButton);

        // Makes sure that any running process will be killed when the dialog is closed.
        addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    if(currentProcess!=null) {
                        currentProcess.destroy();
                    }
                }
            });

        // Sets the dialog's minimum size.
        setMinimumSize(MINIMUM_DIALOG_DIMENSION);
    }



    // - ProcessListener code ------------------------------------------------------------
    // -----------------------------------------------------------------------------------
    /**
     * Notifies the FindFileDialog that the current process has died.
     * @param retValue process' return code (not used).
     */	
    @Override
	public void processDied(int retValue) {
        LOGGER.debug("process exit, return value= "+retValue);
    	//don't touch UI from different thread - use event thread instead 
    	javax.swing.SwingUtilities.invokeLater( new Runnable() {
    		@Override
			public void run() {
    			currentProcess = null;
    			switchToRunState();
    		}
    	 });
    }	

    /**
     * Ignored.
     */
    @Override
	public void processOutput(byte[] buffer, int offset, int length) {}

    /**
     * Notifies the dialog that the process has output some text.
     * @param output contains the process' output.
     */
    @Override
	public void processOutput(final String output) {
    	//don't touch UI from different thread - use event thread instead 
    	javax.swing.SwingUtilities.invokeLater( new Runnable() {
    		@Override
			public void run() {
    			addToTextArea(output);
    		}
    	 });
    }


    // - ActionListener code -------------------------------------------------------------
    // -----------------------------------------------------------------------------------
    /**
     * Notifies the dialog that an action has been performed.
     * @param e describes the action that occured.
     */
    @Override
	public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        // 'Run / stop' button has been pressed.
        if(source == runStopButton) {

            // If we're not running a process, start a new one.
            if(currentProcess == null){
                runSearch(patternText.getText().trim(), contentText.getText().trim());

            // If we're running a process, kill it.
            } else {
                currentProcess.destroy();
                this.currentProcess = null;
                switchToRunState();
            }
        }

        // Cancel button disposes the dialog and kills the process
        else if(source == cancelButton) {
            close();
        }
    }

    private void close(){
    	if(currentProcess != null)
            currentProcess.destroy();
        dispose();
    }

    // - Misc. ---------------------------------------------------------------------------
    // -----------------------------------------------------------------------------------
    /**
     * Switches the UI back to 'Run command' state.
     */
    private void switchToRunState() {
        // Stops the spinning dial.
        dial.setAnimated(false);

        // Change 'Stop' button to 'Run'
        this.runStopButton.setText(Translator.get("find_file_dialog.run"));

        // Make command field active again
        this.patternText.setEnabled(true);

        // Repaint this dialog
        repaint();
    }	

    /**
     * Removes any " and duplicates all backslashes so text can be safely 
     * used in shell when put between "...".
     * @param text
     * @return text prepared for use in shell command
     */
    private String prepareShellText(String text){
    	return text.replace("\"", "");
    	//on Windows following this makes impossible to escape characters in regexp
    	//.replace("\\", "\\\\");
    }
    
    /**
     * Creates shell command that prints all files that match search criteria.
     * @param pattern file name pattern
     * @return shell command that prints all files that match search criteria
     */
    private String prepareShellCommand(String pattern, String content){
    	switch( OsFamily.getCurrent() ){
    	case WINDOWS:
    		return prepareWindowsShellCommand(pattern, content);
    	default:
    		return prepareBashShellCommand(pattern, content);
    	}
    }
    
	/**
     * Creates Windows shell command that prints all files that match search criteria.
     * 
     * Recursive search with filename pattern:
     *   dir /b/s "<file name pattern>"
     *   
     * Recursive search with case-insensitive Regexp match on filename:
     *   dir /b/s | findstr /I /R <file name regexp>
     *   Note: findstr regexp is limited to: . * ^ $ [class] [^class] [x-y]
     * 
     * Filename and content search command:
     * 	<file name search command> | findstr /F:/ /M /C:"<data pattern>"
     * 
     * Filename and case-insensitive content search command:
     * 	<file name search command> | findstr /F:/ /M /I /C:"<data pattern>"
     * 
     * Filename and case-insensitive content regexp search command:
     * 	<file name search command> | findstr /F:/ /M /I /R "<data regexp>"
     *  Note: findstr regexp is limited to: . * ^ $ [class] [^class] [x-y]
     *   
     * @param pattern file name pattern
     * @return shell command that prints all files that match search criteria
     */
    private String prepareWindowsShellCommand(String pattern, String content){
    	String cmd;
    	if( isRegexp.isSelected() ){
    		cmd = "dir /b/s | findstr /I /R \"" + prepareShellText(pattern) + "\"";
    	} else {
    		cmd = "dir /b/s \"" + prepareShellText(pattern) + "\"";
    	}
    	if( content != null && content.length() > 0 ){
    		cmd = cmd + " | findstr /F:/ /M /I /C:\"" + prepareShellText(content) + "\""; 
    	}
    	return cmd;
    }
    
    //PH extract command examples to documentation
    /**
     * Creates Bash shell command that prints all files that match search criteria.
     *
     * Exmaple commands:
     * find . -iname "[pattern]" -exec grep -l "[content]" {} \;
     * find . -iregexp "[pattern]" -exec grep -l "[content]" {} \;
     *
     * @param pattern file name pattern
     * @return shell command that prints all files that match search criteria
     */
    private String prepareBashShellCommand(String pattern, String content){
    	StringBuilder cmd = new StringBuilder("find . -i");
    	if( isRegexp.isSelected() ){
    		cmd.append("regex ");
    	} else {
    		cmd.append("name ");
    	}
    	cmd.append("\"").append(prepareShellText(pattern)).append("\"");

        if( content.length() > 0 ){
            cmd.append(" -exec grep -l \"").append(prepareShellText(content)).append("\" {} \\;");
        }
    	return cmd.toString();
    }

    /**
     * Starts search.
     * @param pattern file name pattern to search for.
     */
    public void runSearch(String pattern, String content) {
        try {
            // Starts the spinning dial.
            dial.setAnimated(true);

            // Change 'Run' button to 'Stop'
            this.runStopButton.setText(Translator.get("find_file_dialog.stop"));

            // Resets the output list.
            foundFilesListModel.clear();
            foundFilesLastFragment = null;
            foundFilesLastFocusSet = false;
            

            // No new pattern can be entered while a process is running.
            patternText.setEnabled(false);

            currentProcess = Shell.execute(prepareShellCommand(pattern, content), mainFrame.getActivePanel().getCurrentFolder(), this);

            // Repaints the dialog.
            repaint();
        }
        catch(Exception e) {
            // Notifies the user that an error occured and resets to normal state.
            addToTextArea(Translator.get("generic_error"));
            switchToRunState();
        }
    }

    /**
     * Appends the specified string to the output area.
     * @param s string to append to the output area.
     */
    private void addToTextArea(String s) {
    	if( foundFilesLastFragment != null ){
    		s = foundFilesLastFragment + s;
    	}
    	int prev=0, pos=-1;
    	while((pos = s.indexOf("\n", prev)) != -1 ){
    		foundFilesListModel.addElement(s.substring(prev, pos));
    		prev = pos+1;
    		
    		//move focus to list and select first item
    		if( !foundFilesLastFocusSet ){
    			foundFilesLastFocusSet = true;
    			foundFilesList.requestFocus();
    			if( foundFilesList.getSelectedIndex() == -1 ){
    				foundFilesList.setSelectedIndex(0);
    			}
    		}
    	}
    	if( prev < s.length() ){
    		foundFilesLastFragment = s.substring(prev);
    	} else {
    		foundFilesLastFragment = null;
    	}
        foundFilesList.repaint();
    }
}
