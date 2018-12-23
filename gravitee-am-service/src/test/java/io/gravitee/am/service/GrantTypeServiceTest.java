/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.am.service;

import io.gravitee.am.model.Client;
import io.gravitee.am.service.impl.GrantTypeServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * @author Alexandre FARIA (lusoalex at github.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class GrantTypeServiceTest {

    @InjectMocks
    private GrantTypeService grantTypeService = new GrantTypeServiceImpl();

    @Test
    public void isValidGrantType_authorization_code_implicit_refresh_token() {
        boolean isValid = this.grantTypeService.isValideGrantType(Arrays.asList("authorization_code", "implicit", "refresh_token"));
        assertTrue("Were expecting to be true",isValid);
    }

    @Test
    public void isValidGrantType_unknown_response_type() {
        boolean isValid = this.grantTypeService.isValideGrantType(Arrays.asList("unknown"));
        assertFalse("Were expecting to be false",isValid);
    }

    @Test
    public void isValidGrantType_empty_response_type() {
        boolean isValid = this.grantTypeService.isValideGrantType(Arrays.asList());
        assertFalse("Were expecting to be false",isValid);
    }

    @Test
    public void completeGrantTypeCorrespondance_missingCodeGrantType() {
        Client client = new Client();
        client.setResponseTypes(Arrays.asList("code"));
        client.setAuthorizedGrantTypes(Arrays.asList());

        client = this.grantTypeService.completeGrantTypeCorrespondance(client);
        assertTrue("was expecting code grant type",client.getAuthorizedGrantTypes().contains("authorization_code"));
    }

    @Test
    public void completeGrantTypeCorrespondance_missingImplicitGrantType() {
        Client client = new Client();
        client.setResponseTypes(Arrays.asList("id_token"));
        client.setAuthorizedGrantTypes(Arrays.asList("authorization_code"));

        client = this.grantTypeService.completeGrantTypeCorrespondance(client);
        assertTrue("was expecting code grant type",client.getAuthorizedGrantTypes().contains("implicit"));
        assertFalse("was expecting code grant type",client.getAuthorizedGrantTypes().contains("authorization_code"));
    }

    @Test
    public void completeGrantTypeCorrespondance_removeImplicitGrantType() {
        Client client = new Client();
        client.setResponseTypes(Arrays.asList("code"));
        client.setAuthorizedGrantTypes(Arrays.asList("implicit"));

        client = this.grantTypeService.completeGrantTypeCorrespondance(client);
        assertFalse("was expecting code grant type",client.getAuthorizedGrantTypes().contains("implicit"));
        assertTrue("was expecting code grant type",client.getAuthorizedGrantTypes().contains("authorization_code"));
    }

    @Test
    public void completeGrantTypeCorrespondance_caseNoResponseType() {
        Client client = new Client();
        client.setResponseTypes(Arrays.asList());
        client.setAuthorizedGrantTypes(Arrays.asList("client_credentials"));

        client = this.grantTypeService.completeGrantTypeCorrespondance(client);
        assertTrue("was expecting code grant type",client.getResponseTypes().isEmpty());
        assertTrue("was expecting code grant type",client.getAuthorizedGrantTypes().contains("client_credentials"));
    }

    @Test
    public void completeGrantTypeCorrespondance_caseAllEmpty() {
        Client client = new Client();
        client.setResponseTypes(Arrays.asList());
        client.setAuthorizedGrantTypes(Arrays.asList());

        client = this.grantTypeService.completeGrantTypeCorrespondance(client);
        assertTrue("was expecting code grant type",client.getResponseTypes().contains("code"));
        assertTrue("was expecting code grant type",client.getAuthorizedGrantTypes().contains("authorization_code"));
    }
}
