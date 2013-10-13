package wyclipse.ui;

import org.eclipse.core.resources.IBuildConfiguration;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Button;
import org.eclipse.ui.dialogs.PropertyPage;

import wyclipse.core.Activator;
import wyclipse.core.WhileyNature;

public class WhileyCompilerPropertyPage extends PropertyPage {
	
	private final String VERIFICATION_TITLE = "Enable Verification";
	
	private Button verificationEnable;
	
	public WhileyCompilerPropertyPage() {
		super();
		setDescription("Properties for the Whiley Compiler");	
	}

	private void addFirstSection(Composite parent) {
		Composite composite = createDefaultComposite(parent);

		//Label for path field
		Label label = new Label(composite, SWT.NONE);
		label.setText(VERIFICATION_TITLE);

		// Verification enable check bos
		verificationEnable = new Button(composite, SWT.CHECK);	
		
		try {
			String ve =
					((IResource) getElement()).getPersistentProperty(
							WhileyNature.VERIFICATION_PROPERTY);
			if(ve != null) {
				verificationEnable.setSelection(ve.equals("true"));
			} else {
				verificationEnable.setSelection(WhileyNature.VERIFICATION_DEFAULT);
			}
		} catch (CoreException e) {
			verificationEnable.setSelection(WhileyNature.VERIFICATION_DEFAULT);
		}
	}
	
	private void addSeparator(Composite parent) {
		Label separator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
		GridData gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		separator.setLayoutData(gridData);
	}

	private void addSecondSection(Composite parent) {
		
	}
	
	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL);
		data.grabExcessHorizontalSpace = true;
		composite.setLayoutData(data);

		addFirstSection(composite);
		addSeparator(composite);
		addSecondSection(composite);
		return composite;
	}

	private Composite createDefaultComposite(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		composite.setLayout(layout);

		GridData data = new GridData();
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
		composite.setLayoutData(data);

		return composite;
	}
	
	protected void performDefaults() {
		super.performDefaults();
		verificationEnable.setSelection(true);
	}
	
	public boolean performOk() {
		// Store properties persistently.
		try {
			IProject project = (IProject) getElement();
			WhileyNature nature = (WhileyNature) project
					.getNature(Activator.WYCLIPSE_NATURE_ID);
			nature.setVerificationEnable(verificationEnable.getSelection());
		} catch (CoreException e) {
			return false;
		}
		return true;
	}
}