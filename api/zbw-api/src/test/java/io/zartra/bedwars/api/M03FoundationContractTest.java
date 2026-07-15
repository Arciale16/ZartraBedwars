package io.zartra.bedwars.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.authorization.AuthorizationDecision;
import io.zartra.bedwars.api.authorization.AuthorizationRequest;
import io.zartra.bedwars.api.authorization.AuthorizationSubject;
import io.zartra.bedwars.api.authorization.PermissionNode;
import io.zartra.bedwars.api.configuration.ConfigurationKey;
import io.zartra.bedwars.api.configuration.ConfigurationVersion;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.localization.LocaleId;
import io.zartra.bedwars.api.localization.LocalizationService.Parameter;
import io.zartra.bedwars.api.localization.LocalizationService.Parameters;
import io.zartra.bedwars.api.localization.MessageKey;
import io.zartra.bedwars.api.secret.SecretRef;
import java.math.BigDecimal;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class M03FoundationContractTest {
    @Test void configurationIdentitiesAreImmutableComparableAndValidated() {
        assertEquals(ConfigurationKey.of("messages.default-locale"),
                ConfigurationKey.of("messages.default-locale"));
        assertEquals(ConfigurationKey.of("a.b").hashCode(), ConfigurationKey.of("a.b").hashCode());
        assertTrue(ConfigurationKey.of("a.b").compareTo(ConfigurationKey.of("a.c")) < 0);
        assertEquals(ConfigurationVersion.of(3), ConfigurationVersion.of(3));
        assertTrue(ConfigurationVersion.of(2).compareTo(ConfigurationVersion.of(3)) < 0);
        assertThrows(IllegalArgumentException.class, () -> ConfigurationKey.of("A"));
        assertThrows(IllegalArgumentException.class, () -> ConfigurationVersion.of(0));
        assertThrows(NullPointerException.class, () -> ConfigurationVersion.of(1).compareTo(null));
    }

    @Test void authorizationContractsCarryExactTypedIdentity() {
        final AuthorizationSubject subject = AuthorizationSubject.of(AuthorizationSubject.Kind.SERVICE,
                DefinitionId.of("zartra", "service/test"));
        final PermissionNode node = PermissionNode.of("zartrabedwars.config.reload");
        final DefinitionId target = DefinitionId.of("zartra", "config/messages");
        final AuthorizationRequest request = AuthorizationRequest.of(subject, node, target);
        assertEquals(subject, request.subject());
        assertEquals(node, request.action());
        assertEquals(target, request.target());
        assertEquals(AuthorizationDecision.allow(target), AuthorizationDecision.allow(target));
        assertNotEquals(AuthorizationDecision.allow(target), AuthorizationDecision.deny(target));
        assertFalse(AuthorizationDecision.deny(target).isAllowed());
        assertThrows(IllegalArgumentException.class, () -> PermissionNode.of("*"));
        assertThrows(NullPointerException.class,
                () -> AuthorizationRequest.of(null, node, target));
    }

    @Test void localeMessageAndParametersUseCanonicalForms() {
        assertEquals("it-IT", LocaleId.parse("IT_it").toString());
        assertEquals(LocaleId.parse("en-us"), LocaleId.parse("en_US"));
        assertTrue(MessageKey.of("game.start").compareTo(MessageKey.of("game.stop")) < 0);
        final Parameters parameters = Parameters.of(Arrays.asList(
                Parameter.text("player", "Alex"),
                Parameter.number("count", new BigDecimal("2.00")),
                Parameter.bool("ready", true)));
        assertEquals("Alex", parameters.find("player").get().value());
        assertEquals("2", parameters.find("count").get().value());
        assertEquals(Parameter.Kind.BOOLEAN, parameters.find("ready").get().kind());
        assertTrue(Parameters.empty().names().isEmpty());
        assertThrows(IllegalArgumentException.class,
                () -> Parameters.of(Arrays.asList(Parameter.text("same", "a"),
                        Parameter.text("same", "b"))));
        assertThrows(IllegalArgumentException.class, () -> LocaleId.parse("not-a-locale-value"));
        assertThrows(IllegalArgumentException.class, () -> MessageKey.of("Bad"));
    }

    @Test void secretReferencesContainOnlyValidatedLocatorMetadata() {
        final SecretRef environment = SecretRef.parse("environment:ZBW_DISCORD_KEY");
        assertEquals(SecretRef.Source.ENVIRONMENT, environment.source());
        assertEquals("ZBW_DISCORD_KEY", environment.key());
        assertEquals(environment, SecretRef.parse(environment.toString()));
        assertEquals(SecretRef.parse("provider:vault/discord").hashCode(),
                SecretRef.parse("provider:vault/discord").hashCode());
        assertThrows(IllegalArgumentException.class, () -> SecretRef.parse("token"));
        assertThrows(IllegalArgumentException.class, () -> SecretRef.parse("unknown:key"));
        assertThrows(IllegalArgumentException.class, () -> SecretRef.parse(null));
    }
}
