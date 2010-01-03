package com.plexobject.rbac.security;

import java.util.Collection;
import java.util.Map;

import org.apache.commons.validator.GenericValidator;
import org.apache.log4j.Logger;

import com.plexobject.rbac.dao.PermissionDAO;
import com.plexobject.rbac.dao.SecurityErrorDAO;
import com.plexobject.rbac.domain.Permission;
import com.plexobject.rbac.domain.SecurityError;
import com.plexobject.rbac.eval.PredicateEvaluator;
import com.plexobject.rbac.utils.CurrentUserRequest;

public class PermissionManager {
    private static final Logger LOGGER = Logger
            .getLogger(PermissionManager.class);
    private final PredicateEvaluator evaluator;
    private final PermissionDAO permissionDAO;
    private final SecurityErrorDAO securityErrorDAO;

    public PermissionManager(final PermissionDAO permissionDAO,
            final SecurityErrorDAO securityErrorDAO,
            final PredicateEvaluator evaluator) {
        this.permissionDAO = permissionDAO;
        this.securityErrorDAO = securityErrorDAO;
        this.evaluator = evaluator;
    }

    public void check(final String operation,
            final Map<String, String> userContext) throws SecurityException {
        String username = CurrentUserRequest.getUsername();
        if (GenericValidator.isBlankOrNull(username)) {
            throw new SecurityException("Username is not specified", username,
                    operation, userContext);
        }
        Collection<Permission> all = permissionDAO.findAll(null, 20);
        for (Permission permission : all) {
            if (permission.impliesOperation(operation)) {

                if (GenericValidator.isBlankOrNull(permission.getExpression())) {
                    return;
                } else {
                    if (evaluator.evaluate(permission.getExpression(),
                            userContext)) {
                        return;
                    }
                }
            }
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Operation " + operation
                    + " is not permitted by permissions " + all);
        }
        try {
            securityErrorDAO.save(new SecurityError(username, operation,
                    userContext));
        } catch (Exception e) {
            LOGGER.error("Failed to save securit error for username "
                    + username + ", operation " + operation + ", context "
                    + userContext, e);
        }
        throw new SecurityException(username, operation, userContext);
    }
}
