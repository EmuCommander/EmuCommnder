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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import com.mucommander.ui.theme.Theme;
import com.mucommander.ui.theme.ThemeManager;

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
    /** Case-insensitive check box. */
    private JCheckBox isCaseInsensitive;
    /** Regexp check box. */
    private JCheckBox isRegexp;
    /** Modified ago check box - when selected modifiedAgo will be taken into account. */
    private JCheckBox isModifiedAgo;
    /** Modified ago spinner. */
    private JSpinner modifiedAgo;
    /** Run/stop button. */
    private JButton       runStopButton;
    /** Cancel button. */
    private JButton       cancelButton;
    /** Text area used to display the output. */
    private JTextArea     outputTextArea;
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
        YBoxPanel mainPanel = new YBoxPanel();

        mainPanel.add(createPatternInput());
        mainPanel.add(createOptionCheckBoxes());
        mainPanel.addSpace(10);

        JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        labelPanel.add(new JLabel(Translator.get("find_file_dialog.found_files")));
        labelPanel.add(new JLabel(dial = new SpinningDial()));
        mainPanel.add(labelPanel);

        return mainPanel;
    }
    
    private Component createPatternInput(){
    	XBoxPanel panel = new XBoxPanel();
    	JLabel label = new JLabel(Translator.get("find_file_dialog.pattern_input"));
        panel.add(label);
        panel.addSpace(5);
    	panel.add(patternText = new JTextField());
    	label.setLabelFor(patternText);
        patternText.setEnabled(true);
        patternText.setToolTipText(
        	Translator.get(
        		WINDOWS.equals(OsFamily.getCurrent())  
	        	? "find_file_dialog.pattern_input_tooltip_win" 
	        	: "find_file_dialog.pattern_input_tooltip_linux"));
        return panel; 
    }
    
    private Component createOptionCheckBoxes(){
    	XBoxPanel panel = new XBoxPanel();
    	
		panel.add(isCaseInsensitive = new JCheckBox(Translator.get("find_file_dialog.case_insensitive"), true));
		
		panel.addSpace(5);
		
		panel.add(isRegexp = new JCheckBox(Translator.get("find_file_dialog.use_regexp"), false));

		panel.addSpace(5);
		
    	panel.add(isModifiedAgo = new JCheckBox(Translator.get("find_file_dialog.modified_ago"), false));
    	isModifiedAgo.addChangeListener(new ChangeListener() {
    		@Override
    		public void stateChanged(ChangeEvent e) {
    			modifiedAgo.setEnabled(isModifiedAgo.isSelected());
    		}
    	});
    	panel.addSpace(3);
    	panel.add(modifiedAgo = new JSpinner( new SpinnerNumberModel(5, 1, Integer.MAX_VALUE, 1) ));
    	modifiedAgo.setEnabled(false);
    	panel.addSpace(3);
        panel.add(new JLabel(Translator.get("find_file_dialog.modified_ago_unit")));
    	
		return panel;
	}
    
    /**
     * Creates the list of found files area.
     * @return a scroll pane containing the list of found files.
     */
    private JScrollPane createOutputArea() {
        // Creates and initialises the output area.
        outputTextArea = new JTextArea();
        outputTextArea.setLineWrap(true);
        outputTextArea.setCaretPosition(0);
        outputTextArea.setRows(10);
        outputTextArea.setEditable(false);

        // Applies the current theme to the output area.
        outputTextArea.setForeground(ThemeManager.getCurrentColor(Theme.SHELL_FOREGROUND_COLOR));
        outputTextArea.setCaretColor(ThemeManager.getCurrentColor(Theme.SHELL_FOREGROUND_COLOR));
        outputTextArea.setBackground(ThemeManager.getCurrentColor(Theme.SHELL_BACKGROUND_COLOR));
        outputTextArea.setSelectedTextColor(ThemeManager.getCurrentColor(Theme.SHELL_SELECTED_FOREGROUND_COLOR));
        outputTextArea.setSelectionColor(ThemeManager.getCurrentColor(Theme.SHELL_SELECTED_BACKGROUND_COLOR));
        outputTextArea.setFont(ThemeManager.getCurrentFont(Theme.SHELL_FONT));

        // Creates a scroll pane on the output area.
        return new JScrollPane(outputTextArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
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
        contentPane.add(createOutputArea(), BorderLayout.CENTER);
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
        currentProcess = null;
        switchToRunState();
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
	public void processOutput(String output) {addToTextArea(output);}


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
                runSearch(patternText.getText());

            // If we're running a process, kill it.
            } else {
                currentProcess.destroy();
                this.currentProcess = null;
                switchToRunState();
            }
        }

        // Cancel button disposes the dialog and kills the process
        else if(source == cancelButton) {
            if(currentProcess != null)
                currentProcess.destroy();
            dispose();
        }
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
        patternText.requestFocus();

        // Disables the caret in the process output area.
        outputTextArea.getCaret().setVisible(false);

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
    	return text.replace("\"", "").replace("\\", "\\\\");
    }
    
    /**
     * Creates shell command that prints all files that match search criteria.
     * @param pattern file name pattern
     * @return shell command that prints all files that match search criteria
     */
    private String prepareShellCommand(String pattern){
    	pattern = pattern.trim();
    	switch( OsFamily.getCurrent() ){
    	case WINDOWS:
    		return prepareWindowsShellCommand(pattern);
    	default:
    		return prepareBashShellCommand(pattern);
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
    private String prepareWindowsShellCommand(String pattern){
    	if( isRegexp.isSelected() ){
    		//PH TODO
    		throw new RuntimeException("not implemented");
    	} else {
    		return "dir /b/s " + prepareShellText(pattern);
    	}
    }
    
    //PH extract command examples to documentation
    /**
     * Creates Bash shell command that prints all files that match search criteria.
     * @param pattern file name pattern
     * @return shell command that prints all files that match search criteria
     */
    private String prepareBashShellCommand(String pattern){
    	StringBuilder cmd = new StringBuilder("find . -");
    	if( isModifiedAgo.isSelected() ){
    		cmd.append("mmin -").append(modifiedAgo.getValue()).append(" -");
    	}
    	if( isCaseInsensitive.isSelected() ){
    		cmd.append("i");
    	}
    	if( isRegexp.isSelected() ){
    		cmd.append("regex ");
    	} else {
    		cmd.append("name ");
    	}
    	cmd.append("\"").append(prepareShellText(pattern)).append("\"");
    	return cmd.toString();
    }

    /**
     * Starts search.
     * @param pattern file name pattern to search for.
     */
    public void runSearch(String pattern) {
        try {
            // Starts the spinning dial.
            dial.setAnimated(true);

            // Change 'Run' button to 'Stop'
            this.runStopButton.setText(Translator.get("find_file_dialog.stop"));

            // Resets the output list.
            outputTextArea.setText("");
            outputTextArea.setCaretPosition(0);
            outputTextArea.getCaret().setVisible(true);
            outputTextArea.requestFocus();

            // No new pattern can be entered while a process is running.
            patternText.setEnabled(false);
  	      
            //PH FindFileDialog replace ls with something more usefull
            currentProcess = Shell.execute(prepareShellCommand(pattern), mainFrame.getActivePanel().getCurrentFolder(), this);

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
        outputTextArea.append(s);
        outputTextArea.setCaretPosition(outputTextArea.getText().length());
        outputTextArea.getCaret().setVisible(true);
        outputTextArea.repaint();
    }
}
