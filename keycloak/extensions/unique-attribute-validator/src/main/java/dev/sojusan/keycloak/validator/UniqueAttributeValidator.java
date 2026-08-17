package dev.sojusan.keycloak.validator;

import java.util.List;
import java.util.Locale;

import jakarta.persistence.EntityManager;
import jakarta.ws.rs.core.Response;

import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.userprofile.UserProfileAttributeValidationContext;
import org.keycloak.validate.SimpleValidator;
import org.keycloak.validate.ValidationContext;
import org.keycloak.validate.ValidationError;
import org.keycloak.validate.ValidatorConfig;

/**
 * Rejects a User Profile attribute value if another user in the same realm already has the
 * same value for that attribute, compared case-insensitively.
 * <p>
 * Attach it to an attribute in the realm's declarative User Profile config via
 * {@code "validations": {"unique-attribute": {}}}.
 * <p>
 * {@link org.keycloak.models.UserProvider#searchForUserByUserAttributeStream} only matches
 * exact values, so this queries the USER_ATTRIBUTE table directly to compare case-insensitively.
 */
public class UniqueAttributeValidator implements SimpleValidator {

    public static final String ID = "unique-attribute";
    public static final String MESSAGE_ATTRIBUTE_NOT_UNIQUE = "error.attributeNotUnique";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ValidationContext validate(Object input, String inputHint, ValidationContext context, ValidatorConfig config) {
        @SuppressWarnings("unchecked")
        List<String> values = (List<String>) input;

        if (values == null || values.isEmpty()) {
            return context;
        }

        String value = values.get(0);

        if (value == null || value.trim().isEmpty()) {
            return context;
        }

        KeycloakSession session = context.getSession();
        RealmModel realm = session.getContext().getRealm();
        UserModel user = UserProfileAttributeValidationContext.from(context).getAttributeContext().getUser();

        if (!isUnique(session, realm, inputHint, value, user)) {
            context.addError(new ValidationError(ID, inputHint, MESSAGE_ATTRIBUTE_NOT_UNIQUE)
                    .setStatusCode(Response.Status.CONFLICT));
        }

        return context;
    }

    private boolean isUnique(KeycloakSession session, RealmModel realm, String attributeName, String value, UserModel currentUser) {
        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();

        Long duplicates = em.createQuery(
                        "select count(a) from UserAttributeEntity a "
                                + "where a.name = :attributeName "
                                + "and lower(coalesce(a.value, a.longValue)) = :value "
                                + "and a.user.realmId = :realmId "
                                + "and (:userId is null or a.user.id <> :userId)",
                        Long.class)
                .setParameter("attributeName", attributeName)
                .setParameter("value", value.toLowerCase(Locale.ROOT))
                .setParameter("realmId", realm.getId())
                .setParameter("userId", currentUser != null ? currentUser.getId() : null)
                .getSingleResult();

        return duplicates == 0;
    }
}
