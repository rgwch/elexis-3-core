/*******************************************************************************
 * Copyright (c) 2006-2010, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 * 
 *******************************************************************************/

package ch.elexis.core.ui.views.codesystems;

import java.util.HashMap;
import java.util.List;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IWorkbenchActionConstants;

import ch.elexis.core.data.events.ElexisEvent;
import ch.elexis.core.data.events.ElexisEventDispatcher;
import ch.elexis.core.data.events.ElexisEventListenerImpl;
import ch.elexis.core.model.ICodeElement;
import ch.elexis.core.model.IPersistentObject;
import ch.elexis.core.ui.actions.ToggleVerrechenbarFavoriteAction;
import ch.elexis.core.ui.commands.ExportiereBloeckeCommand;
import ch.elexis.core.ui.icons.Images;
import ch.elexis.core.ui.selectors.FieldDescriptor;
import ch.elexis.core.ui.selectors.SelectorPanel;
import ch.elexis.core.ui.util.viewers.CommonViewer;
import ch.elexis.core.ui.util.viewers.DefaultLabelProvider;
import ch.elexis.core.ui.util.viewers.SelectorPanelProvider;
import ch.elexis.core.ui.util.viewers.SimpleWidgetProvider;
import ch.elexis.core.ui.util.viewers.ViewerConfigurer;
import ch.elexis.data.Leistungsblock;
import ch.elexis.data.PersistentObject;
import ch.elexis.data.Query;
import ch.elexis.data.VerrechenbarFavorites;
import ch.elexis.data.VerrechenbarFavorites.Favorite;
import ch.rgw.tools.IFilter;
import ch.rgw.tools.Tree;

public class BlockSelector extends CodeSelectorFactory {
	private IAction deleteAction, createAction, exportAction;
	private CommonViewer cv;
	private MenuManager mgr;
	static SelectorPanelProvider slp;
	int eventType = SWT.KeyDown;
	
	ToggleVerrechenbarFavoriteAction tvfa = new ToggleVerrechenbarFavoriteAction();
	ISelectionChangedListener selChangeListener = new ISelectionChangedListener() {
		@Override
		public void selectionChanged(SelectionChangedEvent event){
			TreeViewer tv = (TreeViewer) event.getSource();
			StructuredSelection ss = (StructuredSelection) tv.getSelection();
			tvfa.updateSelection(ss.isEmpty() ? null : ss.getFirstElement());
		}
	};
	
	@Override
	public ViewerConfigurer createViewerConfigurer(CommonViewer cv){
		this.cv = cv;
		cv.setSelectionChangedListener(selChangeListener);
		makeActions();
		mgr = new MenuManager();
		mgr.setRemoveAllWhenShown(true);
		mgr.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		mgr.addMenuListener(new IMenuListener() {
			
			public void menuAboutToShow(IMenuManager manager){
				manager.add(tvfa);
				manager.add(deleteAction);
			}
		});

		cv.setContextMenu(mgr);
		
		FieldDescriptor<?>[] lbName = new FieldDescriptor<?>[] {
			new FieldDescriptor<Leistungsblock>(Leistungsblock.FLD_NAME)
		};
		
		// add keyListener to search field
		Listener keyListener = new Listener() {
			@Override
			public void handleEvent(Event event){
				if (event.type == eventType) {
					if (event.keyCode == SWT.CR || event.keyCode == SWT.KEYPAD_CR) {
						slp.fireChangedEvent();
					}
				}
			}
		};
		for (FieldDescriptor<?> lbn : lbName) {
			lbn.setAssignedListener(eventType, keyListener);
		}
		
		slp = new SelectorPanelProvider(lbName, true);
		slp.addActions(createAction, exportAction);
		ViewerConfigurer vc =
			new ViewerConfigurer(new BlockContentProvider(cv), new DefaultLabelProvider() {
				@Override
				public Image getImage(Object element){
					if(element instanceof Leistungsblock) {
						Favorite fav = VerrechenbarFavorites.isFavorite((IPersistentObject) element);
						if(fav!=null) return Images.IMG_STAR.getImage();
					}
					return null;
				}
			}, slp,
				new ViewerConfigurer.DefaultButtonProvider(), new SimpleWidgetProvider(
					SimpleWidgetProvider.TYPE_TREE, SWT.NONE, null));
		return vc;
	}
	
	@Override
	public Class<? extends PersistentObject> getElementClass(){
		return Leistungsblock.class;
	}
	
	@Override
	public void dispose(){
		
	}
	
	private void makeActions(){
		deleteAction = new Action("Block löschen") {
			@Override
			public void run(){
				Object o = cv.getSelection()[0];
				if (o instanceof Leistungsblock) {
					((Leistungsblock) o).delete();
					cv.notify(CommonViewer.Message.update);
				}
			}
		};
		createAction = new Action("neu erstellen") {
			{
				setImageDescriptor(Images.IMG_NEW.getImageDescriptor());
				setToolTipText("Neuen Block erstellen");
			}
			
			@Override
			public void run(){
				String[] v = cv.getConfigurer().getControlFieldProvider().getValues();
				if (v != null && v.length > 0 && v[0] != null && v[0].length() > 0) {
					new Leistungsblock(v[0], ElexisEventDispatcher.getSelectedMandator());
					cv.notify(CommonViewer.Message.update_keeplabels);
				}
			}
		};
		exportAction = new Action("Blöcke exportieren") {
			{
				setImageDescriptor(Images.IMG_EXPORT.getImageDescriptor());
				setToolTipText("Exportiert alle Blöcke in eine XML-Datei");
			}
			
			@Override
			public void run(){
				// Handler.execute(null, ExportiereBloeckeCommand.ID, null);
				try {
					new ExportiereBloeckeCommand().execute(null);
				} catch (ExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
	}
	
	public static class BlockContentProvider implements
			ViewerConfigurer.ICommonViewerContentProvider, ITreeContentProvider {
		CommonViewer cv;
		ViewerFilter filter;
		private final ElexisEventListenerImpl eeli_lb = new ElexisEventListenerImpl(
			Leistungsblock.class) {
			
			public void catchElexisEvent(ElexisEvent ev){
				cv.notify(CommonViewer.Message.update);
			}
		};
		
		BlockContentProvider(CommonViewer c){
			cv = c;
		}
		
		public void startListening(){
			cv.getConfigurer().getControlFieldProvider().addChangeListener(this);
			ElexisEventDispatcher.getInstance().addListeners(eeli_lb);
			
		}
		
		public void stopListening(){
			cv.getConfigurer().getControlFieldProvider().removeChangeListener(this);
			ElexisEventDispatcher.getInstance().removeListeners(eeli_lb);
		}
		
		public Object[] getElements(Object inputElement){
			Query<Leistungsblock> qbe = new Query<Leistungsblock>(Leistungsblock.class);
			qbe.orderBy(false, new String[] {
				"Name"
			});
			return qbe.execute().toArray();
		}
		
		public void dispose(){
			stopListening();
		}
		
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput){}
		
		/** Vom ControlFieldProvider */
		public void changed(HashMap<String, String> vals){
			TreeViewer tv = (TreeViewer) cv.getViewerWidget();
			if (filter != null) {
				tv.removeFilter(filter);
				filter = null;
			}
			cv.notify(CommonViewer.Message.update);
			if (cv.getConfigurer().getControlFieldProvider().isEmpty()) {
				cv.notify(CommonViewer.Message.empty);
			} else {
				cv.notify(CommonViewer.Message.notempty);
				filter = new BlockFilter(slp.getPanel());
				tv.addFilter(filter);
				
			}
			
		}
		
		/** Vom ControlFieldProvider */
		public void reorder(String field){
			
		}
		
		/** Vom ControlFieldProvider */
		public void selected(){
			// nothing to do
		}
		
		public Object[] getChildren(Object parentElement){
			if (parentElement instanceof Leistungsblock) {
				Leistungsblock lb = (Leistungsblock) parentElement;
				return lb.getElements().toArray();
				
			}
			return new Object[0];
		}
		
		public Object getParent(Object element){
			return null;
		}
		
		public boolean hasChildren(Object element){
			if (element instanceof Leistungsblock) {
				return !(((Leistungsblock) element).isEmpty());
			}
			return false;
		}
		
		@Override
		public void init(){
			// TODO Auto-generated method stub
			
		}
		
	};
	
	static class BlockFilter extends ViewerFilter implements IFilter {
		SelectorPanel slp;
		
		public BlockFilter(SelectorPanel panel){
			slp = panel;
		}
		
		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element){
			return select(element);
		}
		
		public boolean select(Object element){
			PersistentObject po = null;
			if (element instanceof Tree) {
				po = (PersistentObject) ((Tree) element).contents;
			} else if (element instanceof PersistentObject) {
				po = (PersistentObject) element;
			} else {
				return false;
			}
			HashMap<String, String> vals = slp.getValues();
			if (po.isMatching(vals, PersistentObject.MATCH_START, true)) {
				return true;
			} else {
				if (element instanceof Tree) {
					Tree p = ((Tree) element).getParent();
					if (p == null) {
						return false;
					}
					return select(p);
				} else if (element instanceof Leistungsblock) {
					Leistungsblock lb = (Leistungsblock) element;
					List<ICodeElement> elements = lb.getElements();
					String value = vals.get("Name");
					for (ICodeElement ice : elements) {
						if (ice.getText().startsWith(value)) {
							return true;
						}
					}
					return false;
				}
			}
			return false;
		}
	}
	
	@Override
	public String getCodeSystemName(){
		return "Block";
	}
	
	@Override
	public ISelectionProvider getSelectionProvider(){
		return cv.getViewerWidget();
	}
	
	@Override
	public MenuManager getMenuManager(){
		return mgr;
	}
}
