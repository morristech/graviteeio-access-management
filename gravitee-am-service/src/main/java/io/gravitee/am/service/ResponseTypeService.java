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

import java.util.List;

/**
 * @author Alexandre FARIA (lusoalex at github.com)
 * @author GraviteeSource Team
 */
public interface ResponseTypeService {

    /**
     * Throw InvalidClientMetadataException if null or empty, or contains unknown response types.
     * @param responseTypes Array of response_type to validate.
     */
    boolean isValideResponseType(List<String> responseTypes);

    /**
     * Throw InvalidClientMetadataException if null or contains unknown response types.
     * @param responseType String to response_type validate.
     */
    boolean isValideResponseType(String responseType);

    /**
     * Currently in the UI there's no response type settings on the client.
     * So we will add default response type according to selected grant_type.
     * authorization_code  : add code
     * implicit            : add token
     *
     * @param client Client to analyse.
     * @return Client updated Client
     */
    Client applyDefaultResponseType(Client client);
}