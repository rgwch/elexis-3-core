package ch.elexis.core.ui.medication.handlers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.elexis.core.data.events.ElexisEventDispatcher;
import ch.elexis.core.ui.medication.views.MedicationTableViewerItem;
import ch.elexis.core.ui.views.RezeptBlatt;
import ch.elexis.data.Patient;
import ch.elexis.data.Prescription;

public class PrintTakingsListHandler extends AbstractHandler {
	
	public static final String COMMAND_ID = "ch.elexis.core.ui.medication.PrintTakingsList";
	
	private static Logger log = LoggerFactory.getLogger(PrintTakingsListHandler.class);
	
	@SuppressWarnings("unchecked")
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException{
		Patient patient = ElexisEventDispatcher.getSelectedPatient();
		if (patient == null)
			return null;
		
		/*
		List<Prescription> prescRecipes = new ArrayList<Prescription>();
		
		ISelection selection =
			HandlerUtil.getActiveWorkbenchWindow(event).getActivePage().getSelection();
		if (selection != null && !selection.isEmpty()) {
			IStructuredSelection strucSelection = (IStructuredSelection) selection;
			if (strucSelection.getFirstElement() instanceof MedicationTableViewerItem) {
				List<MedicationTableViewerItem> mtvItems = strucSelection.toList();
				for (MedicationTableViewerItem mtvItem : mtvItems) {
					Prescription p = mtvItem.getPrescription();
					if (p != null) {
						prescRecipes.add(p);
					}
				}
			} else if (strucSelection.getFirstElement() instanceof Prescription) {
				prescRecipes.addAll(strucSelection.toList());
			}
		} else {
			prescRecipes = Arrays.asList(patient.getFixmedikation());
		}
		*/
		List<Prescription> prescRecipes = Arrays.asList(patient.getFixmedikation());
		
		RezeptBlatt rpb;
		try {
			rpb =
				(RezeptBlatt) HandlerUtil.getActiveWorkbenchWindow(event).getActivePage()
					.showView(RezeptBlatt.ID);
			rpb.createEinnahmeliste(patient,
				prescRecipes.toArray(new Prescription[prescRecipes.size()]));
		} catch (PartInitException e) {
			log.error("Error outputting recipe", e);
		}
		
		return null;
	}
	
}
