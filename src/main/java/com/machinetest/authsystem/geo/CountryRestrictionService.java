package com.machinetest.authsystem.geo;

import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.machinetest.authsystem.config.AppProperties;
import com.machinetest.authsystem.exception.ApiException;
import com.machinetest.authsystem.exception.ErrorCode;

@Service
public class CountryRestrictionService {

    private static final Logger log = LoggerFactory.getLogger(CountryRestrictionService.class);

    private final GeoLocationService geoLocationService;
    private final Set<String> blockedCountryCodes;
    private final boolean failOpen;

    public CountryRestrictionService(GeoLocationService geoLocationService, AppProperties appProperties) {
        this.geoLocationService = geoLocationService;
        this.blockedCountryCodes = appProperties.geo().blockedCountryCodes();
        this.failOpen = appProperties.geo().failOpen();
    }

    public Optional<Country> assertSignupAllowed(String clientIp) {
        Optional<Country> country = geoLocationService.resolveCountry(clientIp);
        if (country.isEmpty()) {
            if (failOpen) {
                log.warn("Country could not be resolved, allowing sign-up because fail-open is enabled");
                return Optional.empty();
            }
            throw new ApiException(ErrorCode.COUNTRY_UNRESOLVED);
        }
        if (blockedCountryCodes.contains(country.get().code())) {
            log.info("Sign-up blocked for country {}", country.get().code());
            throw new ApiException(ErrorCode.COUNTRY_RESTRICTED);
        }
        return country;
    }
}
