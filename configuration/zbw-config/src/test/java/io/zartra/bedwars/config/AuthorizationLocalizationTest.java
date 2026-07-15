package io.zartra.bedwars.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.authorization.AuthorizationDecision;
import io.zartra.bedwars.api.authorization.AuthorizationRequest;
import io.zartra.bedwars.api.authorization.AuthorizationSubject;
import io.zartra.bedwars.api.authorization.PermissionNode;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.localization.LocaleId;
import io.zartra.bedwars.api.localization.LocalizationService.Parameter;
import io.zartra.bedwars.api.localization.LocalizationService.Parameters;
import io.zartra.bedwars.api.localization.MessageKey;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.config.authorization.DefaultAuthorizationService;
import io.zartra.bedwars.config.authorization.DefaultAuthorizationService.AliasRegistry;
import io.zartra.bedwars.config.authorization.DefaultAuthorizationService.CanonicalAction;
import io.zartra.bedwars.config.authorization.DefaultAuthorizationService.Grant;
import io.zartra.bedwars.config.authorization.DefaultAuthorizationService.ImmutableGrantSource;
import io.zartra.bedwars.config.authorization.DefaultAuthorizationService.PermissionCatalog;
import io.zartra.bedwars.config.localization.DefaultLocalizationService;
import io.zartra.bedwars.config.localization.DefaultLocalizationService.Catalog;
import io.zartra.bedwars.config.localization.DefaultLocalizationService.CatalogCodec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuthorizationLocalizationTest {
    @Test void authorizationIsCentralExactDefaultDenyAndAudited() {
        final AuthorizationSubject subject = AuthorizationSubject.of(AuthorizationSubject.Kind.PLAYER,
                DefinitionId.of("zartra", "player/test"));
        final PermissionNode action = PermissionCatalog.node("config", CanonicalAction.RELOAD);
        final DefinitionId target = DefinitionId.of("zartra", "config/messages");
        final List<AuthorizationDecision> audit = new ArrayList<AuthorizationDecision>();
        final DefaultAuthorizationService service = new DefaultAuthorizationService(
                ImmutableGrantSource.of(Collections.singletonList(Grant.of(subject, action, target))),
                AliasRegistry.empty(), (caller, node, resource, decision) -> audit.add(decision));
        assertTrue(service.authorize(AuthorizationRequest.of(subject, action, target)).isAllowed());
        assertFalse(service.authorize(AuthorizationRequest.of(subject,
                PermissionCatalog.node("config", CanonicalAction.MANAGE), target)).isAllowed());
        assertFalse(service.authorize(AuthorizationRequest.of(subject, action,
                DefinitionId.of("zartra", "config/security"))).isAllowed());
        assertEquals(3, audit.size());
        assertEquals("zartra:authorization/default_deny", audit.get(2).reason().toString());
    }

    @Test void legacyAliasesAreOneHopExactAndCannotEscalateThroughParents() {
        final AuthorizationSubject subject = AuthorizationSubject.of(AuthorizationSubject.Kind.CONSOLE,
                DefinitionId.of("zartra", "console/local"));
        final PermissionNode canonical = PermissionCatalog.node("private.resource-scarcity",
                CanonicalAction.MANAGE);
        final PermissionNode legacy = PermissionNode.of("bedwars.private.resource-scarcity.manage");
        final DefinitionId target = DefinitionId.of("zartra", "private/resource-scarcity");
        final Map<PermissionNode, PermissionNode> aliases = new HashMap<PermissionNode, PermissionNode>();
        aliases.put(legacy, canonical);
        final DefaultAuthorizationService service = new DefaultAuthorizationService(
                ImmutableGrantSource.of(Collections.singletonList(Grant.of(subject, canonical, target))),
                AliasRegistry.of(aliases), (caller, node, resource, decision) -> { });
        assertTrue(service.authorize(AuthorizationRequest.of(subject, legacy, target)).isAllowed());
        assertFalse(service.authorize(AuthorizationRequest.of(subject,
                PermissionNode.of("zartrabedwars.private"), target)).isAllowed());
        assertEquals(canonical, AliasRegistry.of(aliases).resolve(legacy));
        assertEquals(canonical, AliasRegistry.empty().resolve(canonical));

        final Map<PermissionNode, PermissionNode> chain = new HashMap<PermissionNode, PermissionNode>();
        chain.put(PermissionNode.of("legacy.one"), PermissionNode.of("legacy.two"));
        chain.put(PermissionNode.of("legacy.two"), canonical);
        assertThrows(IllegalArgumentException.class, () -> AliasRegistry.of(chain));
        final Map<PermissionNode, PermissionNode> self = Collections.singletonMap(canonical, canonical);
        assertThrows(IllegalArgumentException.class, () -> AliasRegistry.of(self));
        assertThrows(IllegalArgumentException.class, () -> PermissionNode.of("zartra.*"));
        assertEquals(33, PermissionCatalog.nodes("config").size());
        assertThrows(IllegalArgumentException.class, () -> PermissionCatalog.node("BAD",
                CanonicalAction.VIEW));
    }

    @Test void localizationUsesPlayerServerFallbackAndEscapedTypedParameters() {
        final MessageKey welcome = MessageKey.of("lobby.welcome");
        final MessageKey games = MessageKey.of("profile.games");
        final Catalog english = catalog(LocaleId.parse("en-US"), welcome, "Welcome {player}",
                games, "Games: {count}");
        final Catalog italian = catalog(LocaleId.parse("it-IT"), welcome, "Benvenuto {player}",
                games, "Partite: {count}");
        final Catalog frenchIncomplete = Catalog.of(LocaleId.parse("fr-FR"),
                Collections.singletonMap(welcome, "Bienvenue {player}"));
        final DefaultLocalizationService service = new DefaultLocalizationService(
                LocaleId.parse("en-US"), LocaleId.parse("en-US"),
                Arrays.asList(english, italian, frenchIncomplete));
        final PlayerId player = PlayerId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        Result<io.zartra.bedwars.api.localization.LocalizationService.LocalizedMessage> rendered =
                service.render(welcome, Optional.of(player), Parameters.of(Collections.singletonList(
                        Parameter.text("player", "<red>Alex&"))));
        assertTrue(rendered.isSuccess());
        assertEquals("Welcome \\<red\\>Alex\\&", rendered.requireValue().text());
        assertEquals(LocaleId.parse("en-US"), rendered.requireValue().locale());

        assertTrue(service.switchServerLocale(LocaleId.parse("it-IT")).isSuccess());
        assertTrue(service.switchPlayerLocale(player, LocaleId.parse("it-IT")).isSuccess());
        rendered = service.render(welcome, Optional.of(player), Parameters.of(
                Collections.singletonList(Parameter.text("player", "Alex"))));
        assertEquals("Benvenuto Alex", rendered.requireValue().text());
        assertFalse(service.switchServerLocale(LocaleId.parse("fr-FR")).isSuccess());
        assertFalse(service.completeness(LocaleId.parse("fr-FR")).isComplete());
        assertEquals(Collections.singletonList(games),
                service.completeness(LocaleId.parse("fr-FR")).missingKeys());
        assertFalse(service.completeness(LocaleId.parse("de-DE")).catalogPresent());
    }

    @Test void localizationRejectsMissingKeysAndParameterMismatch() {
        final MessageKey key = MessageKey.of("message.test");
        final Catalog catalog = Catalog.of(LocaleId.parse("en-US"),
                Collections.singletonMap(key, "Value {name}"));
        final DefaultLocalizationService service = new DefaultLocalizationService(
                LocaleId.parse("en-US"), LocaleId.parse("en-US"),
                Collections.singletonList(catalog));
        assertTrue(service.render(key, Optional.<PlayerId>empty(), Parameters.empty()).isFailure());
        assertTrue(service.render(MessageKey.of("message.absent"), Optional.<PlayerId>empty(),
                Parameters.empty()).isFailure());
        assertTrue(service.render(key, Optional.<PlayerId>empty(), Parameters.of(Arrays.asList(
                Parameter.text("name", "Alex"), Parameter.bool("extra", true)))).isFailure());
        assertThrows(IllegalArgumentException.class, () -> Catalog.of(LocaleId.parse("en-US"),
                Collections.singletonMap(key, "Malformed {name")));
        assertThrows(IllegalArgumentException.class, () -> Catalog.of(LocaleId.parse("en-US"),
                Collections.<MessageKey, String>emptyMap()));
    }

    @Test void catalogImportExportIsDeterministicAndRejectsMalformedInput() {
        final MessageKey first = MessageKey.of("message.first");
        final MessageKey second = MessageKey.of("message.second");
        final Catalog original = catalog(LocaleId.parse("en-US"), first, "A=B", second, "Tab\tValue");
        final CatalogCodec codec = new CatalogCodec();
        final String exported = codec.exportCatalog(original);
        assertEquals(exported, codec.exportCatalog(original));
        final Catalog imported = codec.importCatalog(LocaleId.parse("en-US"), exported);
        assertEquals(original.templates(), imported.templates());
        assertThrows(IllegalArgumentException.class, () -> codec.importCatalog(LocaleId.parse("en-US"),
                "message.first=a\nmessage.first=b\n"));
        assertThrows(IllegalArgumentException.class, () -> codec.importCatalog(LocaleId.parse("en-US"),
                "missing-separator\n"));
        assertThrows(IllegalArgumentException.class, () -> codec.importCatalog(LocaleId.parse("en-US"),
                "message.first=bad\\q\n"));
        assertThrows(IllegalArgumentException.class, () -> codec.importCatalog(LocaleId.parse("en-US"),
                "message.first=bad\\\n"));
    }

    private static Catalog catalog(final LocaleId locale, final MessageKey first,
                                   final String firstValue, final MessageKey second,
                                   final String secondValue) {
        final Map<MessageKey, String> values = new HashMap<MessageKey, String>();
        values.put(first, firstValue);
        values.put(second, secondValue);
        return Catalog.of(locale, values);
    }
}
