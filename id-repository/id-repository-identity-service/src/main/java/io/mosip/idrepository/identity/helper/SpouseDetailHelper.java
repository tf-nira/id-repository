package io.mosip.idrepository.identity.helper;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.jayway.jsonpath.DocumentContext;

import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.identity.constant.SpouseNumberMapping;
import io.mosip.kernel.core.logger.spi.Logger;

@Component
public class SpouseDetailHelper {
	Logger mosipLogger = IdRepoLogger.getLogger(IdRepoServiceHelper.class);

	private static final String SPOUSE_DETAIL_HELPER = "SpouseDetailHelper";

	@Autowired
	private IdRepoServiceHelper idRepoServiceHelper;

	public void addSpouseDetails(DocumentContext inputData, DocumentContext dbData) throws IOException {
		// TODO verify logic and call from one place and test it through updateIdentity
		String numberOfOtherSpousesInput = getStringData(
				idRepoServiceHelper.getMappingJsonValue("numberOfOtherSpouses"), dbData, null, false);
		String numberOfOtherSpousesDB = getStringData(
				idRepoServiceHelper.getMappingJsonValue("numberOfOtherSpouses"), inputData, null, false);
		int numberOfOtherSpousesDBnumber = 0;
		if (numberOfOtherSpousesDB != null) {
			numberOfOtherSpousesDBnumber = Integer.parseInt(numberOfOtherSpousesDB);
		}
		if(numberOfOtherSpousesInput!=null) {
			int numberOfOtherSpousesRequest = Integer.parseInt(numberOfOtherSpousesInput);
			for (int i = 1; i <= numberOfOtherSpousesRequest; i++) {
				for (int j = 1; j <= 4; j++) {
					List givenNameCheck = getSimpleType(
							idRepoServiceHelper.getMappingJsonValue("spouse" + getNumber(j) + "GivenName"),
							dbData,
							null, false);
					if (givenNameCheck == null) {
						Map<String, Object> inputDataMap = getSpouseDetails(inputData, getNumber(i));
						addSpouseDetailsToDb(dbData, getNumber(j), inputDataMap, getNumber(i));
						numberOfOtherSpousesDBnumber++;
						break;
					}
		       }

			}
			String numberOfOtherSpousesValue = Integer.toString(numberOfOtherSpousesDBnumber);
			dbData.put("$", idRepoServiceHelper.getMappingJsonValue("numberOfOtherSpouses"), numberOfOtherSpousesValue);

		}

	}


	private Map<String, Object> getSpouseDetails(DocumentContext data, String number) throws IOException {
		Map<String, Object> fieldMap = new HashMap<String, Object>();

		getSimpleType(
				idRepoServiceHelper.getMappingJsonValue("spouse" + number + "GivenName"), data, fieldMap, true);
        getSimpleType(
				idRepoServiceHelper.getMappingJsonValue("spouse" + number + "Surname"), data, fieldMap, true);
	    getSimpleType(
				idRepoServiceHelper.getMappingJsonValue("spouse" + number + "OtherNames"), data, fieldMap, true);
		getSimpleType(
				idRepoServiceHelper.getMappingJsonValue("spouse" + number + "PreviousName"), data, fieldMap,
				true);
		getSimpleType(
				idRepoServiceHelper.getMappingJsonValue("spouse" + number + "MaidenName"), data, fieldMap, true);
		getSimpleType(
				idRepoServiceHelper.getMappingJsonValue("spouse" + number + "CitizenshipType"), data, fieldMap,
				true);
		getSimpleType(
				idRepoServiceHelper.getMappingJsonValue("spouse" + number + "PlaceOfMarriage"), data, fieldMap,
				true);

		getSimpleType(
				idRepoServiceHelper.getMappingJsonValue("spouse" + number + "TypeOfMarriage"), data, fieldMap,
				true);
		getStringData(
				idRepoServiceHelper.getMappingJsonValue("spouse" + number + "MarriageCertificateNumber"),
					data, fieldMap,
				true);
		getStringData(idRepoServiceHelper.getMappingJsonValue("spouse" + number + "NIN"), data, fieldMap,
				true);
		getStringData(idRepoServiceHelper.getMappingJsonValue("spouse" + number + "spouseDateOfMarriage"),
					data, fieldMap,
				true);

		return fieldMap;
	}

	private List getSimpleType(String fieldname, DocumentContext dbData, Map<String, Object> fieldMap, boolean add) {
		List fieldnameList = null;
		if (dbData.read("." + fieldname) != null) {
			List fieldnamePath = (List) dbData.read("." + fieldname);
			if (!fieldnamePath.isEmpty()) {
				 fieldnameList = (List) fieldnamePath.get(0);
			}
		}
		if (add) {
			if (fieldnameList != null) {
			fieldMap.put(fieldname, fieldnameList);
			}
		}
		return fieldnameList;
	}

	private String getStringData(String fieldname, DocumentContext dbData, Map<String, Object> fieldMap, boolean add) {
		String fieldValue = null;
		if (dbData.read("." + fieldname) != null) {
			List fieldnamePath = (List) dbData.read("." + fieldname);
			if (!fieldnamePath.isEmpty()) {
				fieldValue = (String) fieldnamePath.get(0);
			}

		}

		if (add) {
			if (fieldValue != null) {
			fieldMap.put(fieldname, fieldValue);
			}
		}
		return fieldValue;
	}

	private void addSpouseDetailsToDb(DocumentContext dbData, String dbNumber, Map<String, Object> fieldMap,
			String inputNumber)
			throws IOException {
		// Example Spouse two details of input need to add to spouse 3 three details

		addToData(dbData, idRepoServiceHelper.getMappingJsonValue("spouse" + dbNumber + "GivenName"), fieldMap,
				idRepoServiceHelper.getMappingJsonValue("spouse" + inputNumber + "GivenName"));
		addToData(dbData, idRepoServiceHelper.getMappingJsonValue("spouse" + dbNumber + "Surname"), fieldMap,
				idRepoServiceHelper.getMappingJsonValue("spouse" + inputNumber + "Surname"));
		addToData(dbData, idRepoServiceHelper.getMappingJsonValue("spouse" + dbNumber + "OtherNames"), fieldMap,
				idRepoServiceHelper.getMappingJsonValue("spouse" + inputNumber + "OtherNames"));
		addToData(dbData, idRepoServiceHelper.getMappingJsonValue("spouse" + dbNumber + "PreviousName"), fieldMap,
				idRepoServiceHelper.getMappingJsonValue("spouse" + inputNumber + "PreviousName"));
		addToData(dbData, idRepoServiceHelper.getMappingJsonValue("spouse" + dbNumber + "MaidenName"), fieldMap,
				idRepoServiceHelper.getMappingJsonValue("spouse" + inputNumber + "MaidenName"));
		addToData(dbData, idRepoServiceHelper.getMappingJsonValue("spouse" + dbNumber + "CitizenshipType"), fieldMap,
				idRepoServiceHelper.getMappingJsonValue("spouse" + inputNumber + "CitizenshipType"));
		addToData(dbData, idRepoServiceHelper.getMappingJsonValue("spouse" + dbNumber + "PlaceOfMarriage"), fieldMap,
				idRepoServiceHelper.getMappingJsonValue("spouse" + inputNumber + "PlaceOfMarriage"));
		addToData(dbData, idRepoServiceHelper.getMappingJsonValue("spouse" + dbNumber + "TypeOfMarriage"), fieldMap,
				idRepoServiceHelper.getMappingJsonValue("spouse" + inputNumber + "TypeOfMarriage"));
		addToData(dbData, idRepoServiceHelper.getMappingJsonValue("spouse" + dbNumber + "MarriageCertificateNumber"),
				fieldMap,
				idRepoServiceHelper.getMappingJsonValue("spouse" + inputNumber + "MarriageCertificateNumber"));
		addToData(dbData, idRepoServiceHelper.getMappingJsonValue("spouse" + dbNumber + "NIN"), fieldMap,
				idRepoServiceHelper.getMappingJsonValue("spouse" + inputNumber + "NIN"));
		addToData(dbData, idRepoServiceHelper.getMappingJsonValue("spouse" + dbNumber + "spouseDateOfMarriage"),
				fieldMap, idRepoServiceHelper.getMappingJsonValue("spouse" + inputNumber + "spouseDateOfMarriage"));
	}


	private void addToData(DocumentContext data, String dbFieldName, Map<String, Object> fieldMap,
			String inputfieldName) {
		if(fieldMap.get(inputfieldName)!=null) {
			data.put("$", dbFieldName, fieldMap.get(inputfieldName));
		}

	}

	private String getNumber(int spousecode) {
		String spouseNumber;
		if (spousecode == SpouseNumberMapping.ONE.getspouseCode()) {
			spouseNumber = SpouseNumberMapping.ONE.getSpouseNumber();
		} else if (spousecode == SpouseNumberMapping.TWO.getspouseCode()) {
			spouseNumber = SpouseNumberMapping.TWO.getSpouseNumber();
		}
		else if (spousecode == SpouseNumberMapping.THREE.getspouseCode()) {
			spouseNumber = SpouseNumberMapping.THREE.getSpouseNumber();
		} else if (spousecode == SpouseNumberMapping.FOUR.getspouseCode()) {
			spouseNumber = SpouseNumberMapping.FOUR.getSpouseNumber();
		} else {
			spouseNumber = "";
		}
		return spouseNumber;

	}

}
