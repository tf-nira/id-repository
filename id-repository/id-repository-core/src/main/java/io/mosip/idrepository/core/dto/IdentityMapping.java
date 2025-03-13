package io.mosip.idrepository.core.dto;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class IdentityMapping {
	private Identity identity;
	private MetaInfo metaInfo;
	private Audits audits;
	private Documents documents;

	@Data
	@NoArgsConstructor
	public static class Identity {
		@JsonProperty("IDSchemaVersion")
		private IDSchemaVersion iDSchemaVersion;
		private Name name;
		private Gender gender;
		private LocationHierarchyForProfiling locationHierarchyForProfiling;
		private Dob dob;
		private Age age;
		private PreferredLanguages preferredLanguage;
		private IntroducerRID introducerRID;
		private IntroducerUIN introducerUIN;
		private IntroducerVID introducerVID;
		private IntroducerName introducerName;
		private Phone phone;
		private Email email;
		private Uin uin;
		private IndividualBiometrics individualBiometrics;
		private IntroducerBiometrics introducerBiometrics;
		private IndividualAuthBiometrics individualAuthBiometrics;
		private OfficerBiometricFileName officerBiometricFileName;
		private SupervisorBiometricFileName supervisorBiometricFileName;
		@JsonProperty("residenceStatus")
		private ResidenceStatus residenceStatus;
		private FullAddress fullAddress;
		@JsonProperty("selectedHandles")
		private SelectedHandles selectedHandles;
		private UserService userService;
		private UserServiceType userServiceType;
		@JsonProperty("applicantOriginPlace")
		private ApplicantOriginPlace applicantOriginPlace;
		@JsonProperty("applicantBirthPlace")
		private ApplicantBirthPlace applicantBirthPlace;
		@JsonProperty("fatherResidence")
		private FatherResidence fatherResidence;
		@JsonProperty("motherResidence")
		private MotherResidence motherResidence;
		@JsonProperty("fatherOrigin")
		private FatherOrigin fatherOrigin;
		@JsonProperty("motherOrigin")
		private MotherOrigin motherOrigin;
		@JsonProperty("applicantPlaceOfResidence")
		private ApplicantPlaceOfResidence applicantPlaceOfResidence;
		@JsonProperty("applicantPlaceOfResidenceDistrict")
		private ApplicantPlaceOfResidenceDistrict applicantPlaceOfResidenceDistrict;
		@JsonProperty("applicantPlaceOfResidenceCounty")
		private ApplicantPlaceOfResidenceCounty applicantPlaceOfResidenceCounty;
		@JsonProperty("applicantPlaceOfResidenceSubCounty")
		private ApplicantPlaceOfResidenceSubCounty applicantPlaceOfResidenceSubCounty;
		@JsonProperty("applicantPlaceOfResidenceParish")
		private ApplicantPlaceOfResidenceParish applicantPlaceOfResidenceParish;
		@JsonProperty("applicantPlaceOfResidenceVillage")
		private ApplicantPlaceOfResidenceVillage applicantPlaceOfResidenceVillage;
		@JsonProperty("applicantPlaceOfResidenceStreet")
		private ApplicantPlaceOfResidenceStreet applicantPlaceOfResidenceStreet;
		@JsonProperty("applicantPlaceOfResidenceYearsLived")
		private ApplicantPlaceOfResidenceYearsLived applicantPlaceOfResidenceYearsLived;
		@JsonProperty("applicantPlaceOfResidenceDistrictOfPrevRes")
		private ApplicantPlaceOfResidenceDistrictOfPrevRes applicantPlaceOfResidenceDistrictOfPrevRes;
		@JsonProperty("applicantPlaceOfResidenceHouseNo")
		private ApplicantPlaceOfResidenceHouseNo applicantPlaceOfResidenceHouseNo;
		@JsonProperty("applicantPlaceOfResidencePostalAddress")
		private ApplicantPlaceOfResidencePostalAddress applicantPlaceOfResidencePostalAddress;
		@JsonProperty("appResCountryUGA")
		private AppResCountryUGA appResCountryUGA;
		@JsonProperty("applicantForeignResidenceCountry")
		private ApplicantForeignResidenceCountry applicantForeignResidenceCountry;
		@JsonProperty("applicantForeignResidenceAddress")
		private ApplicantForeignResidenceAddress applicantForeignResidenceAddress;
		@JsonProperty("applicantForeignBirthCountry")
		private ApplicantForeignBirthCountry applicantForeignBirthCountry;
		@JsonProperty("applicantForeignBirthAddress")
		private ApplicantForeignBirthAddress applicantForeignBirthAddress;
		@JsonProperty("applicantPlaceOfBirthDistrict")
		private ApplicantPlaceOfBirthDistrict applicantPlaceOfBirthDistrict;
		@JsonProperty("appBirCountryUGA")
		private AppBirCountryUGA appBirCountryUGA;
		@JsonProperty("applicantPlaceOfBirthCounty")
		private ApplicantPlaceOfBirthCounty applicantPlaceOfBirthCounty;
		@JsonProperty("applicantPlaceOfBirthSubCounty")
		private ApplicantPlaceOfBirthSubCounty applicantPlaceOfBirthSubCounty;
		@JsonProperty("applicantPlaceOfBirthParish")
		private ApplicantPlaceOfBirthParish applicantPlaceOfBirthParish;
		@JsonProperty("applicantPlaceOfBirthVillage")
		private ApplicantPlaceOfBirthVillage applicantPlaceOfBirthVillage;
		@JsonProperty("applicantPlaceOfBirthCity")
		private ApplicantPlaceOfBirthCity applicantPlaceOfBirthCity;
		@JsonProperty("applicantPlaceOfBirthHealthFacility")
		private ApplicantPlaceOfBirthHealthFacility applicantPlaceOfBirthHealthFacility;
		@JsonProperty("applicantPlaceOfOriginDistrict")
		private ApplicantPlaceOfOriginDistrict applicantPlaceOfOriginDistrict;
		@JsonProperty("appOriCountryUGA")
		private AppOriCountryUGA appOriCountryUGA;
		@JsonProperty("applicantPlaceOfOriginCounty")
		private ApplicantPlaceOfOriginCounty applicantPlaceOfOriginCounty;
		@JsonProperty("applicantPlaceOfOriginSubCounty")
		private ApplicantPlaceOfOriginSubCounty applicantPlaceOfOriginSubCounty;
		@JsonProperty("applicantPlaceOfOriginParish")
		private ApplicantPlaceOfOriginParish applicantPlaceOfOriginParish;
		@JsonProperty("applicantPlaceOfOriginVillage")
		private ApplicantPlaceOfOriginVillage applicantPlaceOfOriginVillage;
		@JsonProperty("applicantPlaceOfOriginIndigenousCommunityTribe")
		private ApplicantPlaceOfOriginIndigenousCommunityTribe applicantPlaceOfOriginIndigenousCommunityTribe;
		@JsonProperty("applicantPlaceOfOriginClan")
		private ApplicantPlaceOfOriginClan applicantPlaceOfOriginClan;
		@JsonProperty("applicantForeignOriginCountry")
		private ApplicantForeignOriginCountry applicantForeignOriginCountry;
		@JsonProperty("applicantForeignOriginAddress")
		private ApplicantForeignOriginAddress applicantForeignOriginAddress;
		@JsonProperty("fatherForeignResidenceCountry")
		private FatherForeignResidenceCountry fatherForeignResidenceCountry;
		@JsonProperty("fatherForeignResidenceAddress")
		private FatherForeignResidenceAddress fatherForeignResidenceAddress;
		@JsonProperty("fatherPostalAddress")
		private FatherPostalAddress fatherPostalAddress;
		@JsonProperty("fatResCountryUGA")
		private FatResCountryUGA fatResCountryUGA;
		@JsonProperty("fatherPlaceOfResidenceDistrict")
		private FatherPlaceOfResidenceDistrict fatherPlaceOfResidenceDistrict;
		@JsonProperty("fatherPlaceOfResidenceCounty")
		private FatherPlaceOfResidenceCounty fatherPlaceOfResidenceCounty;
		@JsonProperty("fatherPlaceOfResidenceSubCounty")
		private FatherPlaceOfResidenceSubCounty fatherPlaceOfResidenceSubCounty;
		@JsonProperty("fatherPlaceOfResidenceParish")
		private FatherPlaceOfResidenceParish fatherPlaceOfResidenceParish;
		@JsonProperty("fatherPlaceOfResidenceVillage")
		private FatherPlaceOfResidenceVillage fatherPlaceOfResidenceVillage;
		@JsonProperty("fatherPlaceOfResidenceStreet")
		private FatherPlaceOfResidenceStreet fatherPlaceOfResidenceStreet;
		@JsonProperty("fatherPlaceOfResidenceHouseNo")
		private FatherPlaceOfResidenceHouseNo fatherPlaceOfResidenceHouseNo;
		@JsonProperty("fatherForeignOriginCountry")
		private FatherForeignOriginCountry fatherForeignOriginCountry;
		@JsonProperty("fatherForeignOriginAddress")
		private FatherForeignOriginAddress fatherForeignOriginAddress;
		@JsonProperty("fatOriCountryUGA")
		private FatOriCountryUGA fatOriCountryUGA;
		@JsonProperty("fatherPlaceOfOriginDistrict")
		private FatherPlaceOfOriginDistrict fatherPlaceOfOriginDistrict;
		@JsonProperty("fatherPlaceOfOriginCounty")
		private FatherPlaceOfOriginCounty fatherPlaceOfOriginCounty;
		@JsonProperty("fatherPlaceOfOriginSubCounty")
		private FatherPlaceOfOriginSubCounty fatherPlaceOfOriginSubCounty;
		@JsonProperty("fatherPlaceOfOriginParish")
		private FatherPlaceOfOriginParish fatherPlaceOfOriginParish;
		@JsonProperty("fatherPlaceOfOriginVillage")
		private FatherPlaceOfOriginVillage fatherPlaceOfOriginVillage;
		@JsonProperty("motherForeignResidenceCountry")
		private MotherForeignResidenceCountry motherForeignResidenceCountry;

		@JsonProperty("motherForeignResidenceAddress")
		private MotherForeignResidenceAddress motherForeignResidenceAddress;
		@JsonProperty("motherPostalAddress")
		private MotherPostalAddress motherPostalAddress;

		@JsonProperty("motResCountryUGA")
		private MotResCountryUGA motResCountryUGA;
		@JsonProperty("motherPlaceOfResidenceDistrict")
		private MotherPlaceOfResidenceDistrict motherPlaceOfResidenceDistrict;

		@JsonProperty("motherPlaceOfResidenceCounty")
		private MotherPlaceOfResidenceCounty motherPlaceOfResidenceCounty;
		@JsonProperty("motherPlaceOfResidenceSubCounty")
		private MotherPlaceOfResidenceSubCounty motherPlaceOfResidenceSubCounty;

		@JsonProperty("motherPlaceOfResidenceParish")
		private MotherPlaceOfResidenceParish motherPlaceOfResidenceParish;
		@JsonProperty("motherPlaceOfResidenceVillage")
		private MotherPlaceOfResidenceVillage motherPlaceOfResidenceVillage;

		@JsonProperty("motherPlaceOfResidenceStreet")
		private MotherPlaceOfResidenceStreet motherPlaceOfResidenceStreet;
		@JsonProperty("motherPlaceOfResidenceHouseNo")
		private MotherPlaceOfResidenceHouseNo motherPlaceOfResidenceHouseNo;
		@JsonProperty("motOriCountryUGA")
		private MotOriCountryUGA motOriCountryUGA;
		@JsonProperty("motherForeignOriginCountry")
		private MotherForeignOriginCountry motherForeignOriginCountry;
		@JsonProperty("motherForeignOriginAddress")
		private MotherForeignOriginAddress motherForeignOriginAddress;
		@JsonProperty("motherPlaceOfOriginDistrict")
		private MotherPlaceOfOriginDistrict motherPlaceOfOriginDistrict;
		@JsonProperty("motherPlaceOfOriginCounty")
		private MotherPlaceOfOriginCounty motherPlaceOfOriginCounty;
		@JsonProperty("motherPlaceOfOriginSubCounty")
		private MotherPlaceOfOriginSubCounty motherPlaceOfOriginSubCounty;
		@JsonProperty("motherPlaceOfOriginParish")
		private MotherPlaceOfOriginParish motherPlaceOfOriginParish;
		@JsonProperty("motherPlaceOfOriginVillage")
		private MotherPlaceOfOriginVillage motherPlaceOfOriginVillage;
		@JsonProperty("motherIndigenousCommunityTribe")
		private MotherIndigenousCommunityTribe motherIndigenousCommunityTribe;
		@JsonProperty("motherIndigenousCommunityClan")
		private MotherIndigenousCommunityClan motherIndigenousCommunityClan;
		@JsonProperty("fatherIndigenousCommunityTribe")
		private FatherIndigenousCommunityTribe fatherIndigenousCommunityTribe;
		@JsonProperty("fatherIndigenousCommunityClan")
		private FatherIndigenousCommunityClan fatherIndigenousCommunityClan;

	}

	@Data
	@NoArgsConstructor
	public static class PreferredLanguages {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class LocationHierarchyForProfiling {
		private String value;

		public List<String> getValueList() {
			return Arrays.asList(Objects.nonNull(value) ? value.split(",") : new String[] { "" }).stream()
					.map(StringUtils::trim).filter(StringUtils::isNotBlank).collect(Collectors.toList());
		}
	}

	@Data
	@NoArgsConstructor
	public static class MetaInfo {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class Audits {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class Poa {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class Poi {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class Por {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class Pob {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class Poe {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class Documents {
		private Poa poa;
		private Poi poi;
		private Por por;
		private Pob pob;
		private Poe poe;

		public List<String> getValueList() {
			return List.of(poa.getValue(), poi.getValue(), por.getValue(), pob.getValue(), poe.getValue());
		}
	}

	@Data
	@NoArgsConstructor
	public static class IDSchemaVersion {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class Name {
		private String value;		
		public List<String> getValueList() {
			return Arrays.asList(Objects.nonNull(value) ? value.split(",") : new String[] { "" }).stream()
					.map(StringUtils::trim).filter(StringUtils::isNotBlank).collect(Collectors.toList());
		}
		
	}

	@Data
	@NoArgsConstructor
	public static class Gender {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class Dob {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class Age {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class IntroducerRID {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class IntroducerUIN {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class IntroducerVID {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class IntroducerName {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class Phone {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class Email {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class Uin {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class IndividualBiometrics {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class IntroducerBiometrics {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class IndividualAuthBiometrics {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class OfficerBiometricFileName {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class SupervisorBiometricFileName {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class ResidenceStatus {
		private String value;
	}
	
	@Data
	@NoArgsConstructor
	public static class FullAddress {
		private String value;
		
		public List<String> getValueList() {
			return Arrays.asList(Objects.nonNull(value) ? value.split(",") : new String[] { "" }).stream()
					.map(StringUtils::trim).filter(StringUtils::isNotBlank).collect(Collectors.toList());
		}
	}

	@Data
	@NoArgsConstructor
	public static class SelectedHandles {
		private String value;
	}
	
	@Data
	@NoArgsConstructor
	public static class UserService {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class UserServiceType{
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class ApplicantOriginPlace {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class ApplicantBirthPlace {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class FatherResidence {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class MotherResidence {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class FatherOrigin {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class MotherOrigin {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class ApplicantPlaceOfResidence {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class ApplicantPlaceOfResidenceDistrict {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class ApplicantPlaceOfResidenceCounty {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class ApplicantPlaceOfResidenceSubCounty {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class ApplicantPlaceOfResidenceParish {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class ApplicantPlaceOfResidenceVillage {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class ApplicantPlaceOfResidenceStreet {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class ApplicantPlaceOfResidenceYearsLived {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class ApplicantPlaceOfResidenceDistrictOfPrevRes {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class ApplicantPlaceOfResidenceHouseNo {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class ApplicantPlaceOfResidencePostalAddress {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class AppResCountryUGA {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class ApplicantForeignResidenceCountry {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class ApplicantForeignResidenceAddress {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class ApplicantForeignBirthCountry {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class ApplicantForeignBirthAddress {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class ApplicantPlaceOfBirthDistrict {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class AppBirCountryUGA {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class ApplicantPlaceOfBirthCounty {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class ApplicantPlaceOfBirthSubCounty {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class ApplicantPlaceOfBirthParish {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class ApplicantPlaceOfBirthVillage {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class ApplicantPlaceOfBirthCity {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class ApplicantPlaceOfBirthHealthFacility {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class ApplicantPlaceOfOriginDistrict {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class AppOriCountryUGA {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class ApplicantPlaceOfOriginCounty {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class ApplicantPlaceOfOriginSubCounty {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class ApplicantPlaceOfOriginParish {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class ApplicantPlaceOfOriginVillage {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class ApplicantPlaceOfOriginIndigenousCommunityTribe {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class ApplicantPlaceOfOriginClan {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class ApplicantForeignOriginCountry {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class ApplicantForeignOriginAddress {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class FatherForeignResidenceCountry {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class FatherForeignResidenceAddress {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class FatherPostalAddress {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class FatResCountryUGA {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class FatherPlaceOfResidenceCounty {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class FatherPlaceOfResidenceDistrict {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class FatherPlaceOfResidenceSubCounty {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class FatherPlaceOfResidenceParish {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class FatherPlaceOfResidenceVillage {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class FatherPlaceOfResidenceStreet {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class FatherPlaceOfResidenceHouseNo {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class FatherForeignOriginCountry {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class FatherForeignOriginAddress {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class FatOriCountryUGA {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class FatherPlaceOfOriginDistrict {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class FatherPlaceOfOriginCounty {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class FatherPlaceOfOriginSubCounty {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class FatherPlaceOfOriginParish {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class FatherPlaceOfOriginVillage {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class MotherForeignResidenceCountry {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class MotherForeignResidenceAddress {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class MotherPostalAddress {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class MotResCountryUGA {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class MotherPlaceOfResidenceDistrict {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class MotherPlaceOfResidenceCounty {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class MotherPlaceOfResidenceSubCounty {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class MotherPlaceOfResidenceStreet {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class MotherPlaceOfResidenceHouseNo {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class MotherPlaceOfResidenceParish {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class MotherPlaceOfResidenceVillage {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class MotherForeignOriginCountry {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class MotherForeignOriginAddress {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class MotherPlaceOfOriginCounty {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class MotherPlaceOfOriginDistrict {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class MotherPlaceOfOriginSubCounty {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class MotherPlaceOfOriginParish {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class MotherPlaceOfOriginVillage {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class MotherIndigenousCommunityTribe {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class MotherIndigenousCommunityClan {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class FatherIndigenousCommunityTribe {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class FatherIndigenousCommunityClan {
		private String value;
	}

	@Data
	@NoArgsConstructor
	public static class MotOriCountryUGA {
		private String value;
	}

}