/*******************************************************************************
 * Copyright (c) 2005-2009, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 *******************************************************************************/
package ch.elexis.data;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.jdom.Document;
import org.jdom.Element;

import ch.elexis.core.data.activator.CoreHub;
import ch.elexis.data.Prescription.EntryType;
import ch.rgw.tools.StringTool;
import ch.rgw.tools.TimeTool;

/**
 * Ein Rezept besteht aus einem Mandanten, einem Patienten, einem Datum und einer Prescription-Liste
 * Aus kompatibilitätsgründen wird in Moment noch der RpText mitgeschleppt
 * 
 * @author Gerry
 * 
 */
public class Rezept extends PersistentObject {
	public static final String LINES = "Zeilen";
	public static final String LETTER_ID = "BriefID";
	public static final String DATE = "Datum";
	public static final String MANDATOR_ID = "MandantID";
	public static final String PATIENT_ID = "PatientID";
	
	@Override
	protected String getTableName(){
		return "REZEPTE";
	}
	
	static {
		addMapping("REZEPTE", PATIENT_ID, MANDATOR_ID, DATE_COMPOUND, "Text=RpTxt", LETTER_ID,
			"Zeilen=LIST:RezeptID:PATIENT_ARTIKEL_JOINT");
	}
	
	public static Rezept load(final String id){
		return new Rezept(id);
	}
	
	public Rezept(final Patient pat){
		create(null);
		set(new String[] {
			PATIENT_ID, MANDATOR_ID, DATE
		}, pat.getId(), CoreHub.actMandant.getId(), new TimeTool().toString(TimeTool.DATE_GER));
	}
	
	public Patient getPatient(){
		return Patient.load(get(PATIENT_ID));
	}
	
	public Mandant getMandant(){
		Mandant mret = Mandant.load(get(MANDATOR_ID));
		return mret;
	}
	
	public String getDate(){
		return get(DATE);
	}
	
	public String getText(){
		return get("Text");
	}
	
	protected Rezept(){}
	
	protected Rezept(final String id){
		super(id);
	}
	
	/**
	 * Den "Brief" liefern. Dieser existiert, wenn das Rezept mindestens einmal gedruckt wurde und
	 * ist die Print-Repräsentation mit etwaigen manuellen Änderungen
	 * 
	 * @return der Brief oder null, wenn keiner existiert.
	 */
	public Brief getBrief(){
		Brief brief = Brief.load(get(LETTER_ID));
		if (brief.exists()) {
			return brief;
		}
		return null;
	}
	
	public void setBrief(final Brief brief){
		if (brief == null) {
			log.error("Null Brief gesetzt bei setBrief");
		} else {
			set(LETTER_ID, brief.getId());
		}
	}
	
	@Override
	public String getLabel(){
		Mandant m = getMandant();
		if (m == null) {
			return getDate() + " (unbekannt)";
		}
		return getDate() + " " + m.getLabel();
	}
	
	/**
	 * Get all prescriptions of the recipe. <br />
	 * <br />
	 * Since 3.1 the {@link Prescription} will be in the same order they were added
	 * {@link Rezept#addPrescription(Prescription)}.
	 * 
	 * @return all prescriptions with a matching recipe id entry
	 */
	public List<Prescription> getLines(){
		List<String> list = getList(LINES, false);
		List<Prescription> ret = new ArrayList<Prescription>(list.size());
		for (String s : list) {
			ret.add(Prescription.load(s));
		}
		return ret.stream()
			.sorted((p1, p2) -> Integer.compare(p1.getRecipeOrder(), p2.getRecipeOrder()))
			.collect(Collectors.toList());
	}
	
	/**
	 * Remove the prescription from the recipe. The prescription will be deleted.
	 * 
	 * @param p
	 */
	public void removePrescription(final Prescription p){
		p.remove();
	}
	
	/**
	 * Add a prescription to the recipe. The Prescription will be marked as recipe entry, and will
	 * be stopped immediately. The prescription will be deleted if it is removed from the recipe. It
	 * should not be altered afterwards. <br />
	 * <br />
	 * Since 3.1 the {@link Prescription} will be added at the end of the list of the
	 * {@link Rezept}, and the same order will be applied on {@link Rezept#getLines()}.
	 * 
	 * @param p
	 */
	public void addPrescription(final Prescription p){
		p.setEndDate(null);
		p.setEntryType(EntryType.RECIPE);
		p.set(Prescription.FLD_REZEPT_ID, getId());
		p.setRecipeOrder(getNextOrder());
	}
	
	private int getNextOrder(){
		List<Prescription> prescriptions = getLines();
		if (prescriptions != null && !prescriptions.isEmpty()) {
			return prescriptions.stream().mapToInt(p -> p.getRecipeOrder()).max().orElse(0) + 1;
		}
		return 0;
	}
	
	@Override
	public boolean delete(){
		Brief brief = getBrief();
		if (brief != null) {
			brief.delete();
		}
		return super.delete();
	}
	
	public Document toXML(){
		List<Prescription> lines = getLines();
		Document ret = new Document();
		Element root = new Element("Rezept");
		root.setAttribute(DATE, getDate());
		root.setAttribute("Patient", getPatient().getLabel());
		root.setAttribute("Aussteller", getMandant().getLabel());
		ret.setRootElement(root);
		for (Prescription l : lines) {
			Element item = new Element("Item");
			item.setAttribute("Verordnung", l.getDosis());
			item.setAttribute("Bemerkung", l.getBemerkung());
			item.addContent(l.getLabel());
			root.addContent(item);
		}
		return ret;
	}
		
}
