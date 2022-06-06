package com.mykaarma.kcommunications.utils;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.mykaarma.kcommunications.model.api.UrlShortenRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DealerRestHelper {

    @Value("${kcommunications_basic_auth_user}")
    private String username;

    @Value("${kcommunications_basic_auth_pass}")
    String password;

    @Value("${dealer_app_url}")
    private String dealerUrl;

    private static final String PATH_URL_SHORTENER = "rest03/url-shortener/shorten";

    public HttpHeaders createHeadersWithBasicAuth(){
        return new HttpHeaders(){
            {
                String auth = username + ":" + password;
                byte[] encodedAuth = org.apache.commons.codec.binary.Base64.encodeBase64(auth.getBytes(StandardCharsets.US_ASCII) );
                String authHeader = "Basic " + new String( encodedAuth );
                set( "Authorization", authHeader );
            }
        };
    }
    public String[] getShortUrlFromKaarmaDealer(String dealerUuid, String departmentUuid, List<String> longUrls) {
        try {
            String url = dealerUrl + PATH_URL_SHORTENER;
            url = UriComponentsBuilder.fromHttpUrl(url).build().encode().toUriString();
            log.info("in getShortUrlFromKaarmaDealer for department_uuid={} long_urls={}", departmentUuid, longUrls);
            HttpHeaders headers = createHeadersWithBasicAuth();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("Accept", MediaType.APPLICATION_JSON_VALUE);
            UrlShortenRequest urlShortenRequest = new UrlShortenRequest(longUrls, dealerUuid, departmentUuid,
                "short_domain_url");
            HttpEntity<UrlShortenRequest> entity = new HttpEntity<>(urlShortenRequest, headers);
            ClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(
                HttpClients.createDefault());

            RestTemplate restTemplate = new RestTemplate(requestFactory);
            String[] response = restTemplate.postForObject(url, entity, String[].class);
            if (response != null && response.length > 0) {
                log.info("in getShortUrlFromKaarmaDealer received short_urls={} for department_uuid={} long_urls={}", response, departmentUuid, longUrls);
                return response;
            } else {
                log.error("error in getShortUrlFromKaarmaDealer for department_uuid={} long_urls={}", departmentUuid, longUrls);
                return null;
            }
        } catch (RestClientException e) {
            log.info("error in getShortUrlFromKaarmaDealer for department_uuid={} long_urls={}", departmentUuid, longUrls, e);
            return null;
        }
    }
}
