package com.machinetest.authsystem.geo;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.machinetest.authsystem.config.AppProperties;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class ClientIpResolver {

    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";

    private final boolean trustForwardedHeader;

    public ClientIpResolver(AppProperties appProperties) {
        this.trustForwardedHeader = appProperties.geo().trustForwardedHeader();
    }

    public String resolve(HttpServletRequest request) {
        if (trustForwardedHeader) {
            String forwardedFor = request.getHeader(FORWARDED_FOR_HEADER);
            if (StringUtils.hasText(forwardedFor)) {
                return forwardedFor.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
