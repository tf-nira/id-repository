package io.mosip.idrepository.identity.helper;

import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.SPOUSE_DETAILS_NOT_FOUND;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import io.mosip.idrepository.core.dto.RequestDTO;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.identity.constant.MappingJsonConstants;
import io.mosip.idrepository.identity.constant.SpouseNumberMapping;
import io.mosip.kernel.core.logger.spi.Logger;

@Component
public class SpouseDetailHelper {
	Logger mosipLogger = IdRepoLogger.getLogger(IdRepoServiceHelper.class);

	private static final String SPOUSE_DETAIL_HELPER = "SpouseDetailHelper";

	@Autowired
	private IdRepoServiceHelper idRepoServiceHelper;

	public void addSpouseDetails(DocumentContext inputData, DocumentContext dbData) throws IOException {

		mosipLogger.info("Before addSpouseDetails - dbData: " + dbData.jsonString());
		mosipLogger.info("Before addSpouseDetails - inputData: " + inputData.jsonString());
		String numberOfOtherSpousesInput = getStringData(
				idRepoServiceHelper.getMappingJsonValue("numberOfOtherSpouses"), inputData, null, false);
		String numberOfOtherSpousesDB = getStringData(
				idRepoServiceHelper.getMappingJsonValue("numberOfOtherSpouses"), dbData, null, false);
		int numberOfOtherSpousesDBnumber = 0;
		if (numberOfOtherSpousesDB != null) {
			numberOfOtherSpousesDBnumber = Integer.parseInt(numberOfOtherSpousesDB);
		}
		if(numberOfOtherSpousesInput!=null) {
			int numberOfOtherSpousesRequest = Integer.parseInt(numberOfOtherSpousesInput);
			for (int i = 1; i <= numberOfOtherSpousesRequest; i++) {
				String dateOfMarriageInput = getStringData(idRepoServiceHelper.getMappingJsonValue("spouse" + getNumber(i) + "DateOfMarriage"), inputData, null, false);
				String givenNameInputValue = getSpouseGivenName(idRepoServiceHelper.getMappingJsonValue("spouse" + getNumber(i) + "GivenName"), inputData);

				if (dateOfMarriageInput != null && givenNameInputValue != null) {
					for (int j = 1; j <= 4; j++) {
						List givenNameCheck = getSimpleType(
								idRepoServiceHelper.getMappingJsonValue("spouse" + getNumber(j) + "GivenName"), dbData, null, false);

						String givenNameCheckValue = getSpouseGivenName(idRepoServiceHelper.getMappingJsonValue("spouse" + getNumber(j) + "GivenName"), dbData);
						String dateOfMarriage = getStringData(idRepoServiceHelper.getMappingJsonValue("spouse" + getNumber(j) + "DateOfMarriage"), dbData, null, false);

						if (dateOfMarriageInput.equalsIgnoreCase(dateOfMarriage) && givenNameInputValue.equalsIgnoreCase(givenNameCheckValue)) {
							removeSpouseDetails(inputData, getNumber(i));
							break;
						}

						if (givenNameCheck == null) {
							Map<String, Object> inputDataMap = getSpouseDetails(inputData, getNumber(i));
							addSpouseDetailsToDb(dbData, getNumber(j), inputDataMap, getNumber(i));
							removeSpouseDetails(inputData, getNumber(i));
							numberOfOtherSpousesDBnumber++;
							break;
						}
					}
				}
				removeSpouseDetails(inputData, getNumber(i));
			}
			String numberOfOtherSpousesValue = Integer.toString(numberOfOtherSpousesDBnumber);
			dbData.put("$", idRepoServiceHelper.getMappingJsonValue("numberOfOtherSpouses"), numberOfOtherSpousesValue);
			inputData.put("$", idRepoServiceHelper.getMappingJsonValue("numberOfOtherSpouses"), numberOfOtherSpousesValue);

			List fieldnameList = getSimpleType("maritalStatus", dbData, null,false);
			if (fieldnameList != null) {
				for (Object obj : fieldnameList) {
					if (obj instanceof Map) {
						Map<String, Object> map = (Map<String, Object>) obj;
						map.put("value", "Married");
					}
				}
			}

			dbData.put("$", idRepoServiceHelper.getMappingJsonValue("maritalStatus"), fieldnameList);
			inputData.put("$", idRepoServiceHelper.getMappingJsonValue("maritalStatus"), fieldnameList);

			inputData.delete(JsonPath.compile("$." + idRepoServiceHelper.getMappingJsonValue("addSpouse")));
		}
		mosipLogger.info("After addSpouseDetails - dbData: " + dbData.jsonString());
		mosipLogger.info("After addSpouseDetails - inputData: " + inputData.jsonString());
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
		getStringData(idRepoServiceHelper.getMappingJsonValue("spouse" + number + "DateOfMarriage"),
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

	public String getStringData(String fieldname, DocumentContext dbData, Map<String, Object> fieldMap, boolean add) {
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
		addToData(dbData, idRepoServiceHelper.getMappingJsonValue("spouse" + dbNumber + "DateOfMarriage"), fieldMap,
				idRepoServiceHelper.getMappingJsonValue("spouse" + inputNumber + "DateOfMarriage"));
	}


	private void addToData(DocumentContext data, String dbFieldName, Map<String, Object> fieldMap,
			String inputfieldName) {
		if(fieldMap.get(inputfieldName)!=null) {
			data.put("$", dbFieldName, fieldMap.get(inputfieldName));
		}

	}

	public String getNumber(int spousecode) {
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

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void updateSpouseDetails(RequestDTO requestDTO, DocumentContext inputData, DocumentContext dbData)
			throws IdRepoAppException, IOException {

		mosipLogger.info("Before updateSpouseDetails - dbData: " + dbData.jsonString());
		mosipLogger.info("Before updateSpouseDetails - inputData: " + inputData.jsonString());
		String removeSpouseDateOfMarriage = null;
		String removeSpouseGivenNameValue = null;
		boolean removed = false;
		List removeSpouseDateOfMarriagePath = inputData
				.read("." + idRepoServiceHelper.getMappingJsonValue(MappingJsonConstants.REMOVESPOUSEDATEOFMARRIRAGE));
		if (removeSpouseDateOfMarriagePath != null && !removeSpouseDateOfMarriagePath.isEmpty()) {
			removeSpouseDateOfMarriage = (String) removeSpouseDateOfMarriagePath.get(0);
		}
		List removeSpouseGivenNamePath = (List) inputData
				.read("." + idRepoServiceHelper.getMappingJsonValue(MappingJsonConstants.REMOVESPOUSEGIVENNAME));
		if ((removeSpouseGivenNamePath != null && !removeSpouseGivenNamePath.isEmpty())) {
			List removeSpouseGivenName = (List) removeSpouseGivenNamePath.get(0);
			Map<String, String> removeSpouseGivenNameMap = (Map<String, String>) removeSpouseGivenName.get(0);
			removeSpouseGivenNameValue = removeSpouseGivenNameMap.get("value");
		}
		if (removeSpouseDateOfMarriage != null && removeSpouseGivenNameValue != null) {
			for (int i = 1; i <= 4; i++) {
				if (!removed) {
					String spouseDateOfMarriage = getStringData(
							idRepoServiceHelper
									.getMappingJsonValue("spouse" + getNumber(i) + "DateOfMarriage"),
							dbData, null, false);
					String spouseGivenName = getSpouseGivenName(
							idRepoServiceHelper
									.getMappingJsonValue("spouse" + getNumber(i) + "GivenName"),
							dbData);
					if (spouseDateOfMarriage != null && spouseGivenName != null) {
						if (spouseDateOfMarriage.equalsIgnoreCase(removeSpouseDateOfMarriage)
								&& spouseGivenName.equalsIgnoreCase(removeSpouseGivenNameValue)) {
							removeSpouseDetails(dbData, getNumber(i));
							removeSpouseDetails(inputData, getNumber(i));
							removed = true;
							break;
						}
					}
				}
			}
			if (removed) {
				dbData.delete(JsonPath.compile("$."
						+ idRepoServiceHelper.getMappingJsonValue(MappingJsonConstants.REMOVESPOUSEDATEOFMARRIRAGE)));
				dbData.delete(JsonPath.compile(
						"$." + idRepoServiceHelper.getMappingJsonValue(MappingJsonConstants.REMOVESPOUSEGIVENNAME)));
				dbData.delete(JsonPath.compile(
						"$." + idRepoServiceHelper.getMappingJsonValue(MappingJsonConstants.REMOVESPOUSESURNAME)));
				inputData.delete(JsonPath.compile("$."
						+ idRepoServiceHelper.getMappingJsonValue(MappingJsonConstants.REMOVESPOUSEDATEOFMARRIRAGE)));
				inputData.delete(JsonPath.compile(
						"$." + idRepoServiceHelper.getMappingJsonValue(MappingJsonConstants.REMOVESPOUSEGIVENNAME)));
				inputData.delete(JsonPath.compile(
						"$." + idRepoServiceHelper.getMappingJsonValue(MappingJsonConstants.REMOVESPOUSESURNAME)));
				List numberOfOtherSpousesPath = dbData
						.read("." + idRepoServiceHelper.getMappingJsonValue(MappingJsonConstants.NUMBEROFOTHERSPOUSES));
				if (numberOfOtherSpousesPath != null && !numberOfOtherSpousesPath.isEmpty()) {
					String numberOfOtherSpousesstr = (String) numberOfOtherSpousesPath.get(0);
					int numberOfOtherSpouses = Integer.parseInt(numberOfOtherSpousesstr);
					if (numberOfOtherSpouses != 0) {
						numberOfOtherSpouses = numberOfOtherSpouses - 1;
						if (numberOfOtherSpouses != 0) {
							String numberOfOtherSpousesValue = Integer.toString(numberOfOtherSpouses);
							dbData.put("$",
									idRepoServiceHelper.getMappingJsonValue(MappingJsonConstants.NUMBEROFOTHERSPOUSES),
									numberOfOtherSpousesValue);
							inputData.put("$",
									idRepoServiceHelper.getMappingJsonValue(MappingJsonConstants.NUMBEROFOTHERSPOUSES),
									numberOfOtherSpousesValue);
						} else {
							dbData.delete(JsonPath.compile("$." + idRepoServiceHelper
									.getMappingJsonValue(MappingJsonConstants.NUMBEROFOTHERSPOUSES)));
							inputData.delete(JsonPath.compile("$." + idRepoServiceHelper
									.getMappingJsonValue(MappingJsonConstants.NUMBEROFOTHERSPOUSES)));

							List fieldnameList = getSimpleType("maritalStatus", dbData, null,false);
							if (fieldnameList != null) {
								for (Object obj : fieldnameList) {
									if (obj instanceof Map) {
										Map<String, Object> map = (Map<String, Object>) obj;
										map.put("value", "Single");
									}
								}
							}
							dbData.put("$", idRepoServiceHelper.getMappingJsonValue("maritalStatus"), fieldnameList);
							inputData.put("$", idRepoServiceHelper.getMappingJsonValue("maritalStatus"), fieldnameList);
						}

					}
				}

			}
			if (removed == false) {
				throw new IdRepoAppException(SPOUSE_DETAILS_NOT_FOUND.getErrorCode(),
						SPOUSE_DETAILS_NOT_FOUND.getErrorMessage());
			}
		}
		dbData.delete(JsonPath.compile("$." + idRepoServiceHelper.getMappingJsonValue("removeSpouse")));
		inputData.delete(JsonPath.compile("$." + idRepoServiceHelper.getMappingJsonValue("removeSpouse")));

		mosipLogger.info("After updateSpouseDetails - dbData: " + dbData.jsonString());
		mosipLogger.info("After updateSpouseDetails - inputData: " + inputData.jsonString());
	}

	private void removeSpouseDetails(DocumentContext dbData, String spouseNumber) throws IOException {
		dbData.delete(JsonPath
				.compile("$." + idRepoServiceHelper.getMappingJsonValue("spouse" + spouseNumber + "DateOfMarriage")));
		dbData.delete(JsonPath
				.compile("$." + idRepoServiceHelper.getMappingJsonValue("spouse" + spouseNumber + "GivenName")));
		dbData.delete(
				JsonPath.compile("$." + idRepoServiceHelper.getMappingJsonValue("spouse" + spouseNumber + "Surname")));
		dbData.delete(JsonPath
				.compile("$." + idRepoServiceHelper.getMappingJsonValue("spouse" + spouseNumber + "OtherNames")));
		dbData.delete(JsonPath
				.compile("$." + idRepoServiceHelper.getMappingJsonValue("spouse" + spouseNumber + "MaidenName")));
		dbData.delete(JsonPath
				.compile("$." + idRepoServiceHelper.getMappingJsonValue("spouse" + spouseNumber + "PreviousName")));
		dbData.delete(
				JsonPath.compile("$." + idRepoServiceHelper.getMappingJsonValue("spouse" + spouseNumber + "NIN")));
		dbData.delete(JsonPath
				.compile("$." + idRepoServiceHelper.getMappingJsonValue("spouse" + spouseNumber + "CitizenshipType")));
		dbData.delete(JsonPath
				.compile("$." + idRepoServiceHelper.getMappingJsonValue("spouse" + spouseNumber + "PlaceOfMarriage")));
		dbData.delete(JsonPath
				.compile("$." + idRepoServiceHelper.getMappingJsonValue("spouse" + spouseNumber + "TypeOfMarriage")));
		dbData.delete(JsonPath.compile(
				"$." + idRepoServiceHelper.getMappingJsonValue("spouse" + spouseNumber + "MarriageCertificateNumber")));
	}

	private String getSpouseGivenName(String spouseGivenNameLabel, DocumentContext dbData) {
		String spouseGivenName = null;

		if (dbData.read("." + spouseGivenNameLabel) != null) {
			List spouseGivenNamePath = (List) dbData.read("." + spouseGivenNameLabel);
			if (!spouseGivenNamePath.isEmpty()) {
				List spouseGivenNameList = (List) spouseGivenNamePath.get(0);
				if (spouseGivenNameList != null && !spouseGivenNameList.isEmpty()) {
					Map<String, String> spouseGivenNameMap = (Map<String, String>) spouseGivenNameList.get(0);
					spouseGivenName = spouseGivenNameMap.get("value");
				}
			}
		}

		return spouseGivenName;
	}
}
