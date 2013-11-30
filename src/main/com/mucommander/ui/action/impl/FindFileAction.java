package com.mucommander.ui.action.impl;

import java.awt.event.KeyEvent;
import java.util.Map;

import javax.swing.KeyStroke;

import com.mucommander.ui.action.AbstractActionDescriptor;
import com.mucommander.ui.action.ActionCategories;
import com.mucommander.ui.action.ActionCategory;
import com.mucommander.ui.action.ActionDescriptor;
import com.mucommander.ui.action.ActionFactory;
import com.mucommander.ui.action.MuAction;
import com.mucommander.ui.dialog.find.FindFileDialog;
import com.mucommander.ui.main.MainFrame;

/**
 * Opens FindFileDialog
 * 
 * @author pawel.halicz
 *
 */
//PH FindFileAction
public class FindFileAction extends MuAction {

	public FindFileAction(MainFrame mainFrame, Map<String, Object> properties) {
		super(mainFrame, properties);
	}

	@Override
	public void performAction() {
		new FindFileDialog(mainFrame).showDialog();
	}

	@Override
	public ActionDescriptor getDescriptor() {
		return new Descriptor();
	}
	
    public static class Factory implements ActionFactory {

		public MuAction createAction(MainFrame mainFrame, Map<String,Object> properties) {
			return new FindFileAction(mainFrame, properties);
		}
    }
    
    public static class Descriptor extends AbstractActionDescriptor {
    	public static final String ACTION_ID = "FindFile";
    	
		public String getId() { return ACTION_ID; }

		public ActionCategory getCategory() { return ActionCategories.MISC; }

		public KeyStroke getDefaultAltKeyStroke() { return null; }

		public KeyStroke getDefaultKeyStroke() { return KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK); }
    }
}
