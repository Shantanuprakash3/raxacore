package org.raxa.module.raxacore.impl;

/**
 * Copyright 2012, Raxa
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Location;
import org.openmrs.Order;
import org.openmrs.User;
import org.openmrs.Provider;
import org.openmrs.Person;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.raxa.module.raxacore.PatientList;
import org.raxa.module.raxacore.PatientListService;
import org.raxa.module.raxacore.db.PatientListDAO;

/*
 * Implements PatientListService.java Note the PatientList query must be in the
 * form of:
 * "?encounterType=<uuid>&startDate=2012-05-07&endDate=2012-05-08&inlist=<uuidForList>&notinlist=<uuidForList>"
 */
public class PatientListServiceImpl implements PatientListService {
	
	private PatientListDAO dao;
	
	private Log log = LogFactory.getLog(this.getClass());
	
	/**
	 * @see org.raxa.module.raxacore.PatientListService#setPatientListDAO
	 */
	@Override
	public void setPatientListDAO(PatientListDAO dao) {
		this.dao = dao;
	}
	
	/**
	 * @see org.raxa.module.raxacore.PatientListService#savePatientList
	 */
	@Override
	public PatientList savePatientList(PatientList patientList) {
		return dao.savePatientList(patientList);
	}
	
	/**
	 * @see org.raxa.module.raxacore.PatientListService#getPatientList(Integer)
	 */
	@Override
	public PatientList getPatientList(Integer id) {
		return dao.getPatientList(id);
	}
	
	/**
	 * @see
	 * org.raxa.module.raxacore.PatientListService#getPatientListByName(String)
	 */
	@Override
	public List<PatientList> getPatientListByName(String name) {
		return dao.getPatientListByName(name);
	}
	
	/**
	 * @see
	 * org.raxa.module.raxacore.PatientListService#getPatientListByUuid(String)
	 */
	@Override
	public PatientList getPatientListByUuid(String uuid) {
		return dao.getPatientListByUuid(uuid);
	}
	
	/**
	 * @see
	 * org.raxa.module.raxacore.PatientListService#getPatientListByEncounterType
	 */
	@Override
	public List<PatientList> getPatientListByEncounterType(EncounterType encounterType) {
		return dao.getPatientListByEncounterType(encounterType);
	}
	
	/**
	 * @see org.raxa.module.raxacore.PatientListService#getAllPatientList
	 */
	@Override
	public List<PatientList> getAllPatientList(boolean includeRetired) {
		return dao.getAllPatientList(includeRetired);
	}
	
	/**
	 * Parses a string into a date
	 *
	 * @param str String to be parsed (must be iso format)
	 * @return Date
	 */
	private Date getDateFromString(String str) {
		
		String[] supportedFormats = { "yyyy-MM-dd'T'HH:mm:ss.SSSZ", "yyyy-MM-dd'T'HH:mm:ss.SSS", "yyyy-MM-dd'T'HH:mm:ssZ",
		        "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd" };
		for (int i = 0; i < supportedFormats.length; i++) {
			try {
				Date date = new SimpleDateFormat(supportedFormats[i]).parse(str);
				return date;
			}
			catch (Exception ex) {
				//log.error(ex.getMessage() + " Error parsing string " + str + " into Date");
			}
		}
		log.error("Date string is malformed");
		return null;
	}
	
	/**
	 * @see
	 * org.raxa.module.raxacore.PatientListService#getEncountersInPatientList
	 */
	@Override
	public List<Encounter> getEncountersInPatientList(PatientList patientList) {
		if (patientList.getSearchQuery() == null) {
			return new ArrayList<Encounter>();
		}
		String query = patientList.getSearchQuery();
		if (query.indexOf("?") == 0) {
			query = query.substring(1);
		}
		//"?encounterType=<uuid>&startDate=2012-05-07&endDate=2012-05-08&inlist=<uuidForList>&notinlist=<uuidForList>"
		EncounterType encType = null;
		Date startDate = null;
		Date endDate = null;
		Provider provid = null;
		Patient patient = null;
		String uuid = null;
		//the return value can only choose encounters from this list (if not null)
		List<Encounter> inListEncounters = null;
		//the return value can not contain any patients from this list
		List<Patient> notInListPatients = new ArrayList<Patient>();
		List<Encounter> notInListEncounters = new ArrayList<Encounter>();
		Location location = null;
		String containsOrderType = "";
		String[] queryFields = query.split("&");
		//if we have an encountertype in our search query, set it
		for (int i = 0; i < queryFields.length; i++) {
			if (queryFields[i].indexOf("encounterType=") != -1) {
				encType = Context.getEncounterService().getEncounterTypeByUuid(queryFields[i].substring(14));
			} else if (queryFields[i].indexOf("location=") != -1) {
				location = Context.getLocationService().getLocationByUuid(queryFields[i].substring(9));
			} else if (queryFields[i].indexOf("startDate=") != -1) {
				startDate = getDateFromString(queryFields[i].substring(10));
			} else if (queryFields[i].indexOf("endDate=") != -1) {
				endDate = getDateFromString(queryFields[i].substring(8));
			} else if (queryFields[i].indexOf("inList=") != -1) {
				inListEncounters = new ArrayList<Encounter>();
				//could be multiple lists here, add all that are in
				String[] inListUuids = queryFields[i].substring(7).split(",");
				for (int j = 0; j < inListUuids.length; j++) {
					inListEncounters.addAll(getEncountersInPatientList(getPatientListByUuid(inListUuids[j])));
				}
			} else if (queryFields[i].indexOf("notInList=") != -1) {
				String[] notInListUuids = queryFields[i].substring(10).split(",");
				for (int k = 0; k < notInListUuids.length; k++) {
					notInListPatients.addAll(getPatientsInPatientList(getPatientListByUuid(notInListUuids[k])));
					notInListEncounters.addAll(getEncountersInPatientList(getPatientListByUuid(notInListUuids[k])));
				}
			} else if (queryFields[i].indexOf("provider=") != -1) {
				uuid = queryFields[i].substring(9);
				provid = Context.getProviderService().getProviderByUuid(uuid);
			} else if (queryFields[i].indexOf("patient=") != -1) {
				uuid = queryFields[i].substring(8);
				patient = Context.getPatientService().getPatientByUuid(uuid);
			} else if (queryFields[i].indexOf("containsOrderType=") != -1) {
				containsOrderType = queryFields[i].substring(18);
			}
		}
		List<EncounterType> encTypes = new ArrayList<EncounterType>();
		List<Provider> provids = new ArrayList<Provider>();
		List<Encounter> encs = new ArrayList<Encounter>();
		encTypes.add(encType);
		provids.add(provid);
		//if we give inList, start with that list and remove encounters
		if (inListEncounters != null) {
			encs = inListEncounters;
			Iterator<Encounter> iter = encs.iterator();
			while (iter.hasNext()) {
				Encounter currEnc = iter.next();
				if ((startDate != null && currEnc.getEncounterDatetime().before(startDate))
				        || (endDate != null && currEnc.getEncounterDatetime().after(endDate))
				        || (encType != null && !currEnc.getEncounterType().equals(encType))
				        || (patient != null && !currEnc.getPatient().equals(patient))) {
					iter.remove();
				} else if (provid != null) {
					Iterator<Set<Provider>> providerIter = currEnc.getProvidersByRoles().values().iterator();
					boolean hasProvider = false;
					while (providerIter.hasNext() && !hasProvider) {
						Set<Provider> providerEncounters = providerIter.next();
						if (providerEncounters.contains(provid)) {
							hasProvider = true;
						}
					}
					if (!hasProvider) {
						iter.remove();
					}
				}
			}
		}
		//otherwise, make an entirely new list
		else {
			if (uuid != null) {
				encs = Context.getEncounterService().getEncounters(patient, location, startDate, endDate, null, encTypes,
				    provids, null, null, Boolean.FALSE);
			} else {
				encs = Context.getEncounterService().getEncounters(patient, location, startDate, endDate, null, encTypes,
				    null, null, null, Boolean.FALSE);
			}
		}
		//refactor this to hash map so double loop is not required
		if (notInListPatients != null) {
			Iterator<Encounter> iter = encs.iterator();
			Iterator<Encounter> iter2;
			//if patient is in notInListPatients, remove the encounter
			while (iter.hasNext()) {
				Encounter currEnc = iter.next();
				//if patient already has encounter, check the dates to see if he should be removed
				if (notInListPatients.contains(currEnc.getPatient())) {
					iter2 = notInListEncounters.iterator();
					boolean removed = false;
					while (iter2.hasNext() && !removed) {
						Encounter currEnc2 = iter2.next();
						if (currEnc2.getPatient().equals(currEnc.getPatient())
						        && currEnc2.getEncounterDatetime().after(currEnc.getEncounterDatetime())) {
							iter.remove();
							removed = true;
						}
					}
				}
			}
		}
		if (containsOrderType.equals("drugOrder")) {
			boolean shouldRemove;
			Iterator<Encounter> iter = encs.iterator();
			while (iter.hasNext()) {
				shouldRemove = true;
				Encounter currEnc = iter.next();
				Iterator<Order> orderIter = currEnc.getOrders().iterator();
				while (orderIter.hasNext()) {
					Order o = orderIter.next();
					if (o.isDrugOrder()) {
						shouldRemove = false;
						break;
					}
				}
				if (shouldRemove) {
					iter.remove();
				}
			}
		}
		return encs;
	}
	
	/**
	 * @see org.raxa.module.raxacore.PatientListService#getPatientsInList
	 */
	@Override
	public List<Patient> getPatientsInPatientList(PatientList patientList) {
		List<Encounter> encounters = getEncountersInPatientList(patientList);
		List<Patient> patients = new ArrayList<Patient>();
		for (int j = 0; j < encounters.size(); j++) {
			//just in case we have an encounter for the same type with the same patient, check if already exists
			if (!patients.contains(encounters.get(j).getPatient())) {
				patients.add(encounters.get(j).getPatient());
			}
		}
		return patients;
	}
	
	/**
	 * @see org.raxa.module.raxacore.PatientListService#updatePatientList
	 */
	@Override
	public PatientList updatePatientList(PatientList patientList) {
		return dao.updatePatientList(patientList);
	}
	
	/**
	 * @see org.raxa.module.raxacore.PatientListService#deletePatientList
	 */
	@Override
	public void deletePatientList(PatientList patientList) {
		dao.deletePatientList(patientList);
	}
	
	@Override
	public void onStartup() {
		log.info("Starting patient list service");
	}
	
	@Override
	public void onShutdown() {
		log.info("Stopping patient list service");
	}
}
