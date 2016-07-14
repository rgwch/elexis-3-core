/*******************************************************************************
 * Copyright (c) 2005-2016, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 *    
 *******************************************************************************/

package ch.elexis.core.ui.dialogs;

import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import ch.elexis.core.data.activator.CoreHub;
import ch.elexis.core.data.util.Extensions;
import ch.elexis.core.ui.ILoginNews;
import ch.elexis.core.ui.UiDesk;
import ch.elexis.core.ui.constants.ExtensionPointConstantsUi;
import ch.elexis.core.ui.util.SWTHelper;
import ch.elexis.core.ui.wizards.DBConnectWizard;
import ch.elexis.data.Anwender;
import ch.elexis.data.Query;
import ch.rgw.tools.ExHandler;

public class LoginDialog extends TitleAreaDialog {
	Text usr, pwd;
	boolean hasUsers;
	ButtonEnabler be = new ButtonEnabler();
	
	public LoginDialog(Shell parentShell){
		super(parentShell);
		
		Query<Anwender> qbe = new Query<Anwender>(Anwender.class);
		List<Anwender> list = qbe.execute();
		hasUsers = (list.size() > 1);
	}
	
	/**
	 * Well, sure, that's not very elegant. We allow the user to switch databases at login, using
	 * the new switch database wizard, but since login does not work at this point, we just exit. 
	 * But on next launch, zthe selected database connection will be active.
	 */
	public void requestDatabaseConnectionConfiguration(){
		WizardDialog wd = new WizardDialog(UiDesk.getTopShell(), new DBConnectWizard());
		wd.create();
		SWTHelper.center(wd.getShell());
		wd.open();
		CoreHub.localCfg.flush();
		this.cancelPressed();
	}

	@Override
	protected Control createDialogArea(Composite parent){
		Composite ret = new Composite(parent, SWT.NONE);
		ret.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		ret.setLayout(new GridLayout(3, false));
		Label lblDatabase=new Label(ret, SWT.WRAP);
		lblDatabase.setLayoutData(SWTHelper.getFillGridData(1,true,1,false));
		lblDatabase.setText(Messages.LoginDialog_database);
		Text tfDatabase=new Text(ret,SWT.BORDER|SWT.READ_ONLY);
		tfDatabase.setText(Anwender.getConnection().getConnectString());
		tfDatabase.setLayoutData(SWTHelper.getFillGridData(1,true,1,false));
		Button chDatabase=new Button(ret,SWT.PUSH);
		chDatabase.setLayoutData(SWTHelper.getFillGridData(1, false, 1, false));
		chDatabase.setText("Change");
		chDatabase.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				requestDatabaseConnectionConfiguration();
			}
			
		});
		tfDatabase.setEnabled(false);

		Label lu = new Label(ret, SWT.NONE);
		lu.setText(Messages.LoginDialog_0);
		usr = new Text(ret, SWT.BORDER);
		usr.setLayoutData(SWTHelper.getFillGridData(2, true, 1, false));
		new Label(ret, SWT.NONE).setText(Messages.LoginDialog_1);
		pwd = new Text(ret, SWT.BORDER | SWT.PASSWORD);
		pwd.setLayoutData(SWTHelper.getFillGridData(2, true, 1, false));
		if (hasUsers == false) {
			usr.setText("Administrator"); //$NON-NLS-1$
			pwd.setText("admin"); //$NON-NLS-1$
		}
		
		@SuppressWarnings("unchecked")
		List<ILoginNews> newsModules =
			Extensions.getClasses(ExtensionPointConstantsUi.LOGIN_NEWS, Messages.LoginDialog_3);
		
		if (newsModules.size() > 0) {
			Composite cNews = new Composite(ret, SWT.NONE);
			cNews.setLayoutData(SWTHelper.getFillGridData(2, true, 1, true));
			cNews.setLayout(new GridLayout());
			for (ILoginNews lm : newsModules) {
				try {
					Composite comp = lm.getComposite(cNews);
					comp.setLayoutData(SWTHelper.getFillGridData());
				} catch (Exception ex) {
					// Note: This is NOT a fatal error. It just means, that the Newsmodule could not
					// load. Maybe we are offline.
					ExHandler.handle(ex);
					
				}
			}
			
		}
		usr.setFocus();
		return ret;
	}
	
	@Override
	protected void okPressed(){
		if (Anwender.login(usr.getText(), pwd.getText()) == true) {
			super.okPressed();
		} else {
			setMessage(Messages.LoginDialog_4, IMessageProvider.ERROR);
			// getButton(IDialogConstants.OK_ID).setEnabled(false);
		}
	}
	
	@Override
	protected void cancelPressed(){
		CoreHub.actUser = null;
		CoreHub.actMandant = null;
		super.cancelPressed();
	}
	
	@Override
	public void create(){
		super.create();
		getButton(IDialogConstants.OK_ID).setText(Messages.LoginDialog_login);
		getButton(IDialogConstants.CANCEL_ID).setText(Messages.LoginDialog_terminate);
		// getButton(IDialogConstants.OK_ID).setEnabled(false);
		
	}
	
	class ButtonEnabler implements ModifyListener {
		
		@Override
		public void modifyText(ModifyEvent e){
			if (usr.getText().length() == 0 || pwd.getText().length() == 0) {
				// getButton(IDialogConstants.OK_ID).setEnabled(false);
			} else {
				// getButton(IDialogConstants.OK_ID).setEnabled(true);
			}
			
		}
		
	}
	
}
