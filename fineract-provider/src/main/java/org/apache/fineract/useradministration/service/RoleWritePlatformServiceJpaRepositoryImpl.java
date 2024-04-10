/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.useradministration.service;

import com.google.gson.Gson;
import jakarta.persistence.PersistenceException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.exception.ErrorHandler;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.useradministration.command.PermissionsCommand;
import org.apache.fineract.useradministration.domain.AppUser;
import org.apache.fineract.useradministration.domain.Permission;
import org.apache.fineract.useradministration.domain.PermissionRepository;
import org.apache.fineract.useradministration.domain.Role;
import org.apache.fineract.useradministration.domain.RoleRepository;
import org.apache.fineract.useradministration.exception.PermissionNotFoundException;
import org.apache.fineract.useradministration.exception.RoleAssociatedException;
import org.apache.fineract.useradministration.exception.RoleNotFoundException;
import org.apache.fineract.useradministration.serialization.PermissionsCommandFromApiJsonDeserializer;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
public class RoleWritePlatformServiceJpaRepositoryImpl implements RoleWritePlatformService {

    private final PlatformSecurityContext context;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RoleDataValidator roleCommandFromApiJsonDeserializer;
    private final PermissionsCommandFromApiJsonDeserializer permissionsFromApiJsonDeserializer;

    @Transactional
    @Override
    public CommandProcessingResult createRole(final JsonCommand command) {
        try {
            final AppUser authenticatedUser = this.context.authenticatedUser();
            final String usuarioCreacionNombre = authenticatedUser.getUsername();
            this.roleCommandFromApiJsonDeserializer.validateForCreate(command.json());
            final Role entity = Role.fromJson(command);
            final String rolNombre = entity.getName();
            this.roleRepository.saveAndFlush(entity);
            final String rolId = String.valueOf(entity.getId());
            final Gson gson = new Gson();
            final String json = gson.toJson(entity);
            return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withEntityId(entity.getId())
                    .withRegistroPosterior(json).withUsuarioCreacionNombre(usuarioCreacionNombre).withRolNombre(rolNombre).withRolId(rolId)
                    .build();
        } catch (final JpaSystemException | DataIntegrityViolationException dve) {
            handleDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return new CommandProcessingResultBuilder() //
                    .withCommandId(command.commandId()) //
                    .build();
        } catch (final PersistenceException dve) {
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            handleDataIntegrityIssues(command, throwable, dve);
            return new CommandProcessingResultBuilder() //
                    .withCommandId(command.commandId()) //
                    .build();
        }
    }

    /*
     * Guaranteed to throw an exception no matter what the data integrity issue is.
     */
    private void handleDataIntegrityIssues(final JsonCommand command, final Throwable realCause, final Exception dve) {
        if (realCause.getMessage().contains("unq_name")) {
            final String name = command.stringValueOfParameterNamed("name");
            throw new PlatformDataIntegrityException("error.msg.role.duplicate.name", "Role with name `" + name + "` already exists",
                    "name", name);
        }

        log.error("Error occured.", dve);
        throw ErrorHandler.getMappable(dve, "error.msg.role.unknown.data.integrity.issue", "Unknown data integrity issue with resource.");
    }

    @Caching(evict = { @CacheEvict(value = "users", allEntries = true), @CacheEvict(value = "usersByUsername", allEntries = true) })
    @Transactional
    @Override
    public CommandProcessingResult updateRole(final Long roleId, final JsonCommand command) {
        try {
            final AppUser authenticatedUser = this.context.authenticatedUser();
            final String usuarioCreacionNombre = authenticatedUser.getUsername();
            this.roleCommandFromApiJsonDeserializer.validateForUpdate(command.json());
            final Role role = this.roleRepository.findById(roleId).orElseThrow(() -> new RoleNotFoundException(roleId));
            final Gson gson = new Gson();
            final String registroAnteriorJson = gson.toJson(role);
            final Map<String, Object> changes = role.update(command);
            if (!changes.isEmpty()) {
                this.roleRepository.saveAndFlush(role);
            }
            final String rolNombre = role.getName();
            final String rolId = String.valueOf(role.getId());
            final String registroPosterior = gson.toJson(role);
            return new CommandProcessingResultBuilder() //
                    .withCommandId(command.commandId()) //
                    .withEntityId(roleId) //
                    .with(changes) //
                    .withRegistroAnterior(registroAnteriorJson).withRegistroPosterior(registroPosterior)
                    .withUsuarioCreacionNombre(usuarioCreacionNombre).withRolNombre(rolNombre).withRolId(rolId).build();
        } catch (final JpaSystemException | DataIntegrityViolationException dve) {
            handleDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return new CommandProcessingResultBuilder() //
                    .withCommandId(command.commandId()) //
                    .build();
        } catch (final PersistenceException dve) {
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            handleDataIntegrityIssues(command, throwable, dve);
            return new CommandProcessingResultBuilder() //
                    .withCommandId(command.commandId()) //
                    .build();
        }
    }

    @Caching(evict = { @CacheEvict(value = "users", allEntries = true), @CacheEvict(value = "usersByUsername", allEntries = true) })
    @Transactional
    @Override
    public CommandProcessingResult updateRolePermissions(final Long roleId, final JsonCommand command) {
        final AppUser authenticatedUser = this.context.authenticatedUser();
        final String usuarioCreacionNombre = authenticatedUser.getUsername();
        final Role role = this.roleRepository.findById(roleId).orElseThrow(() -> new RoleNotFoundException(roleId));
        final Gson gson = new Gson();
        final String registroAnteriorJson = gson.toJson(role);
        final Collection<Permission> allPermissions = this.permissionRepository.findAll();
        final PermissionsCommand permissionsCommand = this.permissionsFromApiJsonDeserializer.commandFromApiJson(command.json());
        final Map<String, Boolean> commandPermissions = permissionsCommand.getPermissions();
        final Map<String, Object> changes = new HashMap<>();
        final Map<String, Boolean> changedPermissions = new HashMap<>();
        for (Map.Entry<String, Boolean> entry : commandPermissions.entrySet()) {
            final boolean isSelected = entry.getValue();

            final Permission permission = findPermissionByCode(allPermissions, entry.getKey());
            final boolean changed = role.updatePermission(permission, isSelected);
            if (changed) {
                changedPermissions.put(entry.getKey(), isSelected);
            }
        }
        if (!changedPermissions.isEmpty()) {
            changes.put("permissions", changedPermissions);
            this.roleRepository.saveAndFlush(role);
        }
        final String rolNombre = role.getName();
        final String rolId = String.valueOf(role.getId());
        final String registroPosterior = gson.toJson(role);
        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(roleId) //
                .with(changes) //
                .withRegistroAnterior(registroAnteriorJson).withRegistroPosterior(registroPosterior)
                .withUsuarioCreacionNombre(usuarioCreacionNombre).withRolNombre(rolNombre).withRolId(rolId).build();
    }

    private Permission findPermissionByCode(final Collection<Permission> allPermissions, final String permissionCode) {

        if (allPermissions != null) {
            for (final Permission permission : allPermissions) {
                if (permission.hasCode(permissionCode)) {
                    return permission;
                }
            }
        }
        throw new PermissionNotFoundException(permissionCode);
    }

    /**
     * Method for Delete Role
     */
    @Transactional
    @Override
    public CommandProcessingResult deleteRole(Long roleId) {
        final AppUser authenticatedUser = this.context.authenticatedUser();
        final String usuarioCreacionNombre = authenticatedUser.getUsername();
        try {
            /**
             * Checking the role present in DB or not using role_id
             */
            final Role role = this.roleRepository.findById(roleId).orElseThrow(() -> new RoleNotFoundException(roleId));
            final String rolNombre = role.getName();
            final String rolId = String.valueOf(role.getId());
            final Gson gson = new Gson();
            final String registroAnteriorJson = gson.toJson(role);
            /**
             * Roles associated with users can't be deleted
             */
            final Integer count = this.roleRepository.getCountOfRolesAssociatedWithUsers(roleId);
            if (count > 0) {
                throw new RoleAssociatedException("error.msg.role.associated.with.users.deleted", roleId);
            }

            this.roleRepository.delete(role);
            return new CommandProcessingResultBuilder().withEntityId(roleId).withRegistroAnterior(registroAnteriorJson)
                    .withUsuarioCreacionNombre(usuarioCreacionNombre).withRolNombre(rolNombre).withRolId(rolId).build();
        } catch (final JpaSystemException | DataIntegrityViolationException e) {
            throw ErrorHandler.getMappable(e, "error.msg.unknown.data.integrity.issue",
                    "Unknown data integrity issue with resource: " + e.getMostSpecificCause());
        }
    }

    /**
     * Method for disabling the role
     */
    @Transactional
    @Override
    public CommandProcessingResult disableRole(Long roleId) {
        final AppUser authenticatedUser = this.context.authenticatedUser();
        final String usuarioCreacionNombre = authenticatedUser.getUsername();
        try {
            /**
             * Checking the role present in DB or not using role_id
             */
            final Role role = this.roleRepository.findById(roleId).orElseThrow(() -> new RoleNotFoundException(roleId));
            final String rolNombre = role.getName();
            final String rolId = String.valueOf(role.getId());
            // if(role.isDisabled()){throw new RoleNotFoundException(roleId);}
            final Gson gson = new Gson();
            final String registroAnteriorJson = gson.toJson(role);
            /**
             * Roles associated with users can't be disable
             */
            final Integer count = this.roleRepository.getCountOfRolesAssociatedWithUsers(roleId);
            if (count > 0) {
                throw new RoleAssociatedException("error.msg.role.associated.with.users.disabled", roleId);
            }

            /**
             * Disabling the role
             */
            role.disableRole();
            final String registroPosterior = gson.toJson(role);
            this.roleRepository.saveAndFlush(role);
            return new CommandProcessingResultBuilder().withEntityId(roleId).withRegistroAnterior(registroAnteriorJson)
                    .withRegistroPosterior(registroPosterior).withUsuarioCreacionNombre(usuarioCreacionNombre).withRolNombre(rolNombre)
                    .withRolId(rolId).build();

        } catch (final JpaSystemException | DataIntegrityViolationException e) {
            throw ErrorHandler.getMappable(e, "error.msg.unknown.data.integrity.issue",
                    "Unknown data integrity issue with resource: " + e.getMostSpecificCause());
        }
    }

    /**
     * Method for Enabling the role
     */
    @Transactional
    @Override
    public CommandProcessingResult enableRole(Long roleId) {
        final AppUser authenticatedUser = this.context.authenticatedUser();
        final String usuarioCreacionNombre = authenticatedUser.getUsername();
        try {
            /**
             * Checking the role present in DB or not using role_id
             */
            final Role role = this.roleRepository.findById(roleId).orElseThrow(() -> new RoleNotFoundException(roleId));
            final String rolNombre = role.getName();
            final String rolId = String.valueOf(role.getId());
            // if(!role.isEnabled()){throw new RoleNotFoundException(roleId);}
            final Gson gson = new Gson();
            final String registroAnteriorJson = gson.toJson(role);
            role.enableRole();
            this.roleRepository.saveAndFlush(role);
            final String registroPosterior = gson.toJson(role);
            return new CommandProcessingResultBuilder().withEntityId(roleId).withRegistroAnterior(registroAnteriorJson)
                    .withRegistroPosterior(registroPosterior).withUsuarioCreacionNombre(usuarioCreacionNombre).withRolNombre(rolNombre)
                    .withRolId(rolId).build();

        } catch (final JpaSystemException | DataIntegrityViolationException e) {
            throw ErrorHandler.getMappable(e, "error.msg.unknown.data.integrity.issue",
                    "Unknown data integrity issue with resource: " + e.getMostSpecificCause());
        }
    }
}
