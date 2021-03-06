// Copyright (c) 2011, David J. Pearce (djp@ecs.vuw.ac.nz)
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//    * Redistributions of source code must retain the above copyright
//      notice, this list of conditions and the following disclaimer.
//    * Redistributions in binary form must reproduce the above copyright
//      notice, this list of conditions and the following disclaimer in the
//      documentation and/or other materials provided with the distribution.
//    * Neither the name of the <organization> nor the
//      names of its contributors may be used to endorse or promote products
//      derived from this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL DAVID J. PEARCE BE LIABLE FOR ANY
// DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package wyclipse.ui.wizards;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;

import wyclipse.ui.util.WyclipseUI;

/**
 * Implements the first page of the new Whiley project wizard. The main
 * responsibilities of this page are:
 * <ol>
 * <li>Determine the project, and ensure this does not already exist.</li>
 * <li>Determine the project location, using the default location as the initial
 * value.</li>
 * </ol>
 * 
 * @author David J. Pearce
 * 
 */
public class NewWhileyProjectPageOne extends WizardNewProjectCreationPage {
	private Composite container;
	private Combo wreCombo;
	private Combo wycsCombo;
	
	/**
	 * Constructor for SampleNewWizardPage.
	 * 
	 * @param pageName
	 */
	public NewWhileyProjectPageOne() {
		super("Create a Whiley Project");
		setTitle("Create a Whiley Project");
		setDescription("Enter a project name.");
	}

	/**
	 * @see IDialogPage#createControl(Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		// First, setup outer container
		this.container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		container.setLayout(layout);
		
		// Second add default controls which are managed by super class, and fit
		// them into our three column format.
		super.createControl(container);		
		
		// at this point, we can use container to add more widgets onto the main
		// page. For now, I don't do anything. 
		Group group = WyclipseUI.createGroup(container, "Whiley Environment", SWT.SHADOW_ETCHED_IN, 1, 2);
		WyclipseUI.createLabel(group, "Standard Library:", 1);
		wreCombo = WyclipseUI.createCombo(group, 1, "Default WyRT");
		wreCombo.setText("Default WyRT");
		WyclipseUI.createLabel(group, "Verifier:", 1);
		wycsCombo = WyclipseUI.createCombo(group, 1, "Default WyCS");
		wycsCombo.setText("Default WyCS");
		
		setControl(container);
	}	
	
	public String getWhileyRuntime() {
		return wreCombo.getText();
	}
	
	@Override
	public void dispose() {
		super.dispose();
		container.dispose();
		wreCombo.dispose();
		wycsCombo.dispose();
	}
}