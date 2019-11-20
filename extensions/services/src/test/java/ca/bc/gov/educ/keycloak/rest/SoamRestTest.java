package ca.bc.gov.educ.keycloak.rest;

import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ca.bc.gov.educ.keycloak.soam.mapper.SoamProtocolMapper;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {SoamProtocolMapper.class})
public class SoamRestTest {

    @Autowired
    SoamProtocolMapper clientService;

    @Before
    public void setup() {
       
    }

    @Test
    public void testOwners() {
        String pen =  clientService.getPen();
        assertNotNull(pen);

    }
}