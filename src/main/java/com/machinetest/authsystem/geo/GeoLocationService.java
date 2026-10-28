package com.machinetest.authsystem.geo;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.machinetest.authsystem.config.AppProperties;

@Service
public class GeoLocationService {

    private static final Logger log = LoggerFactory.getLogger(GeoLocationService.class);
    private static final Pattern IP_LITERAL = Pattern.compile("^[0-9a-fA-F:.]{2,45}$");
    private static final String QUERY = "?fields=status,message,country,countryCode";

    private final RestClient geoRestClient;
    private final String lookupUrl;

    public GeoLocationService(RestClient geoRestClient, AppProperties appProperties) {
        this.geoRestClient = geoRestClient;
        this.lookupUrl = appProperties.geo().lookupUrl();
    }

    public Optional<Country> resolveCountry(String clientIp) {
        String target = isPublicIp(clientIp) ? clientIp : "";
        try {
            IpApiResponse response = geoRestClient.get()
                    .uri(lookupUrl + target + QUERY)
                    .retrieve()
                    .body(IpApiResponse.class);
            if (response == null || !"success".equalsIgnoreCase(response.status())
                    || !StringUtils.hasText(response.countryCode())) {
                log.warn("Country lookup returned no result for the client address");
                return Optional.empty();
            }
            return Optional.of(new Country(response.countryCode().toUpperCase(), response.country()));
        } catch (RestClientException e) {
            log.warn("Country lookup failed", e);
            return Optional.empty();
        }
    }

    static boolean isPublicIp(String ip) {
        if (!StringUtils.hasText(ip) || !IP_LITERAL.matcher(ip).matches()) {
            return false;
        }
        try {
            InetAddress address = InetAddress.getByName(ip);
            if (address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress()
                    || address.isAnyLocalAddress() || address.isMulticastAddress()) {
                return false;
            }
            byte[] bytes = address.getAddress();
            return !(bytes.length == 16 && (bytes[0] & 0xFE) == 0xFC);
        } catch (UnknownHostException e) {
            return false;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record IpApiResponse(String status, String message, String country, String countryCode) {
    }
}
