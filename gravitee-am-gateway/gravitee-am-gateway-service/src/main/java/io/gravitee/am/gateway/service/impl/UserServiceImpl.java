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
package io.gravitee.am.gateway.service.impl;

import io.gravitee.am.common.oauth2.Parameters;
import io.gravitee.am.gateway.service.UserService;
import io.gravitee.am.model.Domain;
import io.gravitee.am.model.User;
import io.gravitee.am.repository.management.api.GroupRepository;
import io.gravitee.am.repository.management.api.UserRepository;
import io.gravitee.am.service.exception.UserNotFoundException;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class UserServiceImpl implements UserService {

    private final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    private static final String GROUP_MAPPING_ATTRIBUTE = "_RESERVED_AM_GROUP_MAPPING_";
    private static final String SOURCE_FIELD = "source";

    @Autowired
    private Domain domain;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Override
    public Single<User> findOrCreate(io.gravitee.am.identityprovider.api.User user) {
        String source = (String) user.getAdditionalInformation().get("source");
        return userRepository.findByDomainAndUsernameAndSource(domain.getId(), user.getUsername(), source)
                .switchIfEmpty(Maybe.error(new UserNotFoundException(user.getUsername())))
                .flatMapSingle(existingUser -> enhanceUserWithGroupRoles(existingUser, user))
                .flatMap(existingUser -> {
                    logger.debug("Updating user: username[%s]", user.getUsername());
                    // set external id
                    existingUser.setExternalId(user.getId());
                    existingUser.setLoggedAt(new Date());
                    existingUser.setLoginsCount(existingUser.getLoginsCount() + 1);
                    // set roles
                    if (existingUser.getRoles() == null) {
                        existingUser.setRoles(user.getRoles());
                    } else if (user.getRoles() != null) {
                        existingUser.getRoles().addAll(user.getRoles());
                    }
                    Map<String, Object> additionalInformation = user.getAdditionalInformation();
                    extractAdditionalInformation(existingUser, additionalInformation);
                    return userRepository.update(existingUser);
                })
                .onErrorResumeNext(ex -> {
                    if (ex instanceof UserNotFoundException) {
                        logger.debug("Creating a new user: username[%s]", user.getUsername());
                        final User newUser = new User();
                        // set external id
                        newUser.setExternalId(user.getId());
                        newUser.setUsername(user.getUsername());
                        newUser.setDomain(domain.getId());
                        newUser.setCreatedAt(new Date());
                        newUser.setLoggedAt(new Date());
                        newUser.setLoginsCount(1L);
                        newUser.setRoles(user.getRoles());

                        Map<String, Object> additionalInformation = user.getAdditionalInformation();
                        extractAdditionalInformation(newUser, additionalInformation);
                        return userRepository.create(newUser);
                    }
                    return Single.error(ex);
                });
    }

    @Override
    public Maybe<User> findById(String id) {
        return userRepository.findById(id);
    }

    private Single<User> enhanceUserWithGroupRoles(User user, io.gravitee.am.identityprovider.api.User idpUser) {
        if (idpUser.getAdditionalInformation() != null && idpUser.getAdditionalInformation().containsKey(GROUP_MAPPING_ATTRIBUTE)) {
            Map<String, List<String>> groupMapping = (Map<String, List<String>>) idpUser.getAdditionalInformation().get(GROUP_MAPPING_ATTRIBUTE);
            // for each group if current user is member of one of these groups add corresponding role to the user
            return Observable.fromIterable(groupMapping.entrySet())
                    .flatMapSingle(entry -> groupRepository.findByIdIn(entry.getValue())
                            .map(groups -> groups
                                    .stream()
                                    .filter(group -> group.getMembers().contains(user.getId()))
                                    .findFirst())
                            .map(optionalGroup -> optionalGroup.isPresent() ? Optional.of(entry.getKey()) : Optional.<String>empty()))
                    .toList()
                    .map(optionals -> {
                        List<String> roles = optionals.stream().filter(Optional::isPresent).map(opt -> opt.get()).collect(Collectors.toList());
                        user.setRoles(roles);
                        return user;
                    });
        } else {
            return Single.just(user);
        }
    }

    private void extractAdditionalInformation(User user, Map<String, Object> additionalInformation) {
        if (additionalInformation != null) {
            Map<String, Object> extraInformation = new HashMap<>(additionalInformation);
            user.setSource((String) extraInformation.remove(SOURCE_FIELD));
            user.setClient((String) extraInformation.remove(Parameters.CLIENT_ID.value()));
            extraInformation.remove(GROUP_MAPPING_ATTRIBUTE);
            user.setAdditionalInformation(extraInformation);
        }
    }
}
