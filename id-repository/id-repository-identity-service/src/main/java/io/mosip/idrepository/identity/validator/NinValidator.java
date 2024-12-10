package io.mosip.idrepository.identity.validator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.mosip.kernel.core.idvalidator.exception.InvalidIDException;

@Component
public class NinValidator {
	@Value("${mosip.registration.util.common.nin.regex:^[a-zA-Z0-9]{14,14}}")
	private String regexPattern;
	public boolean validateNin(Object nin) {
        Pattern pattern = Pattern.compile(regexPattern);
        Matcher matcher = pattern.matcher((CharSequence) nin);
        if(matcher.matches()) {
        	return true;
        }
        else {
        	throw new InvalidIDException("NIN 1","Enter a Valid NIN");
        }
    }
}