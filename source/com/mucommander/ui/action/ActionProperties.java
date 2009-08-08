/*
 * This file is part of muCommander, http://www.mucommander.com
 * Copyright (C) 2002-2009 Maxence Bernard
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

package com.mucommander.ui.action;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.KeyStroke;

/**
 * TODO: document
 * 
 * @author Arik Hadas
 */
public class ActionProperties {
	
	/* map action id -> action descriptor */
	private static Hashtable actionDescriptors = new Hashtable();
	
	private static HashSet actionCategories = new HashSet();
	
	private static HashMap defaultPrimaryActionKeymap = new HashMap();
	private static HashMap defaultAlternateActionKeymap = new HashMap();
	private static AcceleratorMap defaultAcceleratorMap = new AcceleratorMap();
	
	public static void addActionDescriptor(ActionDescriptor actionDescriptor) {
		String actionId = actionDescriptor.getId();
		actionDescriptors.put(actionId, actionDescriptor);
		
		ActionCategory category = actionDescriptor.getCategory();
		if (category != null)
			actionCategories.add(category);
		
		// keymaps:
		KeyStroke defaultActionKeyStroke = actionDescriptor.getDefaultKeyStroke();
		if (defaultActionKeyStroke != null) {
			defaultPrimaryActionKeymap.put(actionId, defaultActionKeyStroke);
			defaultAcceleratorMap.putAccelerator(defaultActionKeyStroke, actionId);
		}
		
		KeyStroke defaultActionAlternativeKeyStroke = actionDescriptor.getDefaultAltKeyStroke();
		if (defaultActionAlternativeKeyStroke != null) {
			defaultAlternateActionKeymap.put(actionId, defaultActionAlternativeKeyStroke);
			defaultAcceleratorMap.putAlternativeAccelerator(defaultActionAlternativeKeyStroke, actionId);
		}
	}
	
	public static ActionDescriptor getActionDescriptor(String actionId) {
		return (ActionDescriptor) actionDescriptors.get(actionId);
	}
	
	public static ActionCategory getActionCategory(String actionId) {
		return getActionDescriptor(actionId).getCategory();
	}
	
	public static KeyStroke getDefaultAccelerator(String actionID) {
		return (KeyStroke) defaultPrimaryActionKeymap.get(actionID);
	}
	
	public static KeyStroke getDefaultAlternativeAccelerator(String actionID) {
		return (KeyStroke) defaultAlternateActionKeymap.get(actionID);
	}
	
	static String getDefaultActionForKeyStroke(KeyStroke keyStroke) {
		return defaultAcceleratorMap.getActionId(keyStroke);
	}
	
	static int getDefaultAcceleratorType(KeyStroke keyStroke) {
		return defaultAcceleratorMap.getAcceleratorType(keyStroke);
	}
	
	public static String getActionLabel(String actionId) {
		return getActionDescriptor(actionId).getLabel();
	}
	
	public static String getActionLabelKey(String actionId) {
		return getActionDescriptor(actionId).getLabelKey();
	}
	
	public static ImageIcon getActionIcon(String actionId) {
		return getActionDescriptor(actionId).getIcon();
	}
	
	public static String getActionTooltip(String actionId) {
		return getActionDescriptor(actionId).getTooltip();
	}
	
	public static Set getActionCategories() {
		return actionCategories;
	}
}
