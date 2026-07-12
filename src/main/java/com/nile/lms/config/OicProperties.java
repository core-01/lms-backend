package com.nile.lms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "oic")
public class OicProperties {
    private String registerUrl;

    public String getRegisterUrl() { return registerUrl; }
    public void setRegisterUrl(String registerUrl) { this.registerUrl = registerUrl; }
}