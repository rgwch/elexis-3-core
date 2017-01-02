package ch.elexis.core.ui.medication.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.State;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;

import ch.elexis.core.ui.medication.handlers.ApplyCustomSortingHandler;
import ch.elexis.data.Artikel;
import ch.elexis.data.Prescription;
import ch.elexis.data.Prescription.EntryType;
import ch.elexis.data.Query;
import ch.rgw.tools.ExHandler;
import ch.rgw.tools.Money;
import ch.rgw.tools.TimeTool;

public class MedicationViewHelper {
	private static final int FILTER_PRESCRIPTION_AFTER_N_DAYS = 30;
	
	public static ViewerSortOrder getSelectedComparator(){
		ICommandService service =
			(ICommandService) PlatformUI.getWorkbench().getService(ICommandService.class);
		Command command = service.getCommand(ApplyCustomSortingHandler.CMD_ID);
		State state = command.getState(ApplyCustomSortingHandler.STATE_ID);
		
		if ((Boolean) state.getValue()) {
			return ViewerSortOrder.getSortOrderPerValue(ViewerSortOrder.MANUAL.val);
		} else {
			return ViewerSortOrder.getSortOrderPerValue(ViewerSortOrder.DEFAULT.val);
		}
	}
	
	public static String calculateDailyCostAsString(List<Prescription> pres){
		String TTCOST = Messages.FixMediDisplay_DailyCost;
		
		double cost = 0.0;
		boolean canCalculate = true;
		
		for (Prescription pr : pres) {
			float num = Prescription.calculateTagesDosis(pr.getDosis());
			try {
				Artikel art = pr.getArtikel();
				if (art != null && art.getATC_code() != null) {
					if (art.getATC_code().toUpperCase().startsWith("J07")) {
						continue;
					}
					if (pr.getEntryType() == EntryType.RECIPE
						|| pr.getEntryType() == EntryType.SELF_DISPENSED) {
						continue;
					}
					int ve = art.guessVE();
					if (ve != 0) {
						Money price = pr.getArtikel().getVKPreis();
						cost += num * price.getAmount() / ve;
					} else {
						canCalculate = false;
					}
				} else {
					canCalculate = false;
				}
			} catch (Exception ex) {
				ExHandler.handle(ex);
				canCalculate = false;
			}
		}
		
		double rounded = Math.round(100.0 * cost) / 100.0;
		if (canCalculate) {
			return TTCOST +" "+Double.toString(rounded);
		} else {
			if (rounded == 0.0) {
				return TTCOST + " ?";
			} else {
				return TTCOST + " >" + Double.toString(rounded);
			}
		}
	}
	
	/**
	 * <pre>
	 * SELECT * FROM PATIENT_ARTIKEL_JOINT 
	 * WHERE deleted='0' AND PatientId='C7dc8b102d96407ed0632' 
	 * AND (
	 * 	(DateFrom >= '20150922' AND RezeptID is not null)
	 * 	OR
	 * 	# FIXED MEDICATION
	 * 	(RezeptID is null AND DateUntil is null)
	 * )
	 * </pre>
	 * 
	 * @param patId
	 * @return
	 */
	public static List<Prescription> loadInputData(boolean loadFullHistory, String patId){
		if (patId == null)
			return Collections.emptyList();
			
		if (loadFullHistory) {
			return loadAllHistorical(patId);
		}
		return loadNonHistorical(patId);
	}
	
	private static List<Prescription> loadNonHistorical(String patId){
		// make sure just now closed are not included
		TimeTool now = new TimeTool();
		now.add(TimeTool.SECOND, 5);
		Query<Prescription> qbe = new Query<Prescription>(Prescription.class);
		qbe.add(Prescription.FLD_PATIENT_ID, Query.EQUALS, patId);
		qbe.startGroup();
		qbe.add(Prescription.FLD_DATE_UNTIL, Query.EQUALS, null);
		qbe.or();
		qbe.add(Prescription.FLD_DATE_UNTIL, Query.GREATER,
			now.toString(TimeTool.TIMESTAMP));
		qbe.endGroup();
		
		List<Prescription> tmpPrescs = qbe.execute();
		
		List<Prescription> result = new ArrayList<Prescription>();
		for (Prescription p : tmpPrescs) {
			if (p.getArtikel() != null && p.getArtikel().getATC_code() != null) {
				if (p.getArtikel().getATC_code().toUpperCase().startsWith("J07")) {
					continue;
				}
				if (p.getEntryType() == EntryType.RECIPE
					|| p.getEntryType() == EntryType.SELF_DISPENSED) {
					continue;
				}
			}
			
			result.add(p);
		}
		return result;
	}
	
	private static List<Prescription> loadAllHistorical(String patId){
		Query<Prescription> qbe = new Query<Prescription>(Prescription.class);
		qbe.add(Prescription.FLD_PATIENT_ID, Query.EQUALS, patId);
		return qbe.execute();
	}
}
