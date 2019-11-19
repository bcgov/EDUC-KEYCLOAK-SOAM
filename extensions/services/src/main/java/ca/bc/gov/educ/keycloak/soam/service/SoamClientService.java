package ca.bc.gov.educ.keycloak.soam.service;

import org.jboss.logging.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@ComponentScan("ca.bc.gov.educ.keycloak.soam")
public class SoamClientService {
	private static Logger logger = Logger.getLogger(SoamClientService.class);
	
    @Value("${soamAPI.url}")
    private String soamApiURL;

    private RestTemplate restTemplate = new RestTemplate();

    public String login() {
    	logger.info("Soam API URL: " + soamApiURL);
        return restTemplate.getForObject(soamApiURL, String.class);
    }

}