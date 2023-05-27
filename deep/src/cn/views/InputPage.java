package cn.njust.cy.views;

import java.util.LinkedHashMap;

import java.util.Map;
import java.util.regex.Pattern;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import cn.njust.cy.actions.InversionRefactoring;
import cn.njust.cy.actions.MoveRefactoring;

public class InputPage  extends UserInputWizardPage {
	private Refactoring refactoring;
	private Map<Text, String> textMap;
	private Map<Text, String> defaultNamingMap;
	public InputPage(Refactoring refactoring) {
		super("Extracted Class Name");
		this.refactoring = refactoring;
		this.textMap = new LinkedHashMap<Text, String>();
		this.defaultNamingMap = new LinkedHashMap<Text, String>();
	}
	@Override
	public void createControl(Composite arg0) {
		// TODO Auto-generated method stub
		Composite result= new Composite(arg0, SWT.NONE);
		setControl(result);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		result.setLayout(layout);
		
		Label extractedClassNameLabel = new Label(result, SWT.NONE);
		extractedClassNameLabel.setText("New Class Name");
		extractedClassNameLabel.setFont(new Font(null, new FontData("Segoe UI", 9, SWT.NORMAL)));
		
		Text extractedClassNameField = new Text(result, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
		extractedClassNameField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		if(refactoring instanceof InversionRefactoring) {
			InversionRefactoring inversionRefactoring = (InversionRefactoring)refactoring;
			extractedClassNameField.setText(inversionRefactoring.getNewFileName());
			
			textMap.put(extractedClassNameField, inversionRefactoring.getNewFileName());
			defaultNamingMap.put(extractedClassNameField, inversionRefactoring.getNewFileName());
		}
		if(refactoring instanceof MoveRefactoring) {
			MoveRefactoring moveRefactoring = (MoveRefactoring)refactoring;
			extractedClassNameField.setText(moveRefactoring.getNewFileName());
			
			textMap.put(extractedClassNameField, moveRefactoring.getNewFileName());
			defaultNamingMap.put(extractedClassNameField, moveRefactoring.getNewFileName());
		}
		
		final Button restoreButton = new Button(result, SWT.PUSH);
		restoreButton.setText("Restore Defaults");
		
		for(Text field : textMap.keySet()) {
			field.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					handleInputChanged();
				}
			});
		}

		restoreButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				for(Text field : defaultNamingMap.keySet()) {
					field.setText(defaultNamingMap.get(field));
				}
			}
		});
		
	}
	
	private void handleInputChanged() {
		String classNamePattern = "[a-zA-Z_][a-zA-Z0-9_]*";
		for(Text text : textMap.keySet()) {
			if(!Pattern.matches(classNamePattern, text.getText())) {
				setPageComplete(false);
				String message = "Type name \"" + text.getText() + "\" is not valid";
				setMessage(message, ERROR);
				return;
			}
//			else if(parentPackageClassNames.contains(text.getText())) {
//				setPageComplete(false);
//				String message = "A Type named \"" + text.getText() + "\" already exists in package " + parentPackage.getElementName();
//				setMessage(message, ERROR);
//				return;
//			}
//			else if(javaLangClassNames.contains(text.getText())) {
//				setPageComplete(false);
//				String message = "Type \"" + text.getText() + "\" already exists in package java.lang";
//				setMessage(message, ERROR);
//				return;
//			}
			else {
				if(refactoring instanceof InversionRefactoring) {
					InversionRefactoring inversionRefactoring = (InversionRefactoring)refactoring;
					inversionRefactoring.setNewFileName(text.getText());
				}
				if(refactoring instanceof MoveRefactoring) {
					MoveRefactoring moveRefactoring = (MoveRefactoring)refactoring;
					moveRefactoring.setNewFileName(text.getText());
				}
			}
		}
		setPageComplete(true);
		setMessage("", NONE);
	}
}
