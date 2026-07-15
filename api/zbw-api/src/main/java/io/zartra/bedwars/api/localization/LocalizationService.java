package io.zartra.bedwars.api.localization;

import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.result.Result;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Thread-safe localization port with server, per-player and fallback language selection.
 *
 * <p>Parameters must be scalar strings, numbers or booleans. Missing keys and malformed parameter
 * sets return typed failures. Renderers never treat a parameter as trusted formatting markup.</p>
 */
public interface LocalizationService {
    /** @return rendered neutral text using per-player, server and fallback resolution */
    Result<LocalizedMessage> render(MessageKey key, Optional<PlayerId> player,
                                    Parameters parameters);
    /** @return the selected server locale after an atomic complete-catalog switch */
    Result<LocaleId> switchServerLocale(LocaleId locale);
    /** @return the selected player locale after an atomic complete-catalog switch */
    Result<LocaleId> switchPlayerLocale(PlayerId player, LocaleId locale);

    /** Immutable neutral rendering result consumed by version-specific text adapters. */
    final class LocalizedMessage {
        private final LocaleId locale;
        private final MessageKey key;
        private final String text;
        private LocalizedMessage(final LocaleId locale, final MessageKey key, final String text) {
            this.locale = java.util.Objects.requireNonNull(locale, "locale");
            this.key = java.util.Objects.requireNonNull(key, "key");
            this.text = java.util.Objects.requireNonNull(text, "text");
        }
        /** @return immutable rendered message */
        public static LocalizedMessage of(final LocaleId locale, final MessageKey key,
                                          final String text) {
            return new LocalizedMessage(locale, key, text);
        }
        /** @return locale actually selected */ public LocaleId locale() { return locale; }
        /** @return stable message key */ public MessageKey key() { return key; }
        /** @return neutral rendered text for a platform adapter */ public String text() { return text; }
    }

    /** Immutable named scalar parameters; formatting markup is never accepted as a value kind. */
    final class Parameters {
        private final Map<String, Parameter> values;
        private Parameters(final Collection<Parameter> parameters) {
            final Map<String, Parameter> collected = new LinkedHashMap<String, Parameter>();
            for (Parameter parameter : parameters) {
                final Parameter checked = java.util.Objects.requireNonNull(parameter, "parameter");
                if (collected.put(checked.name(), checked) != null) {
                    throw new IllegalArgumentException("Duplicate localization parameter");
                }
            }
            values = Collections.unmodifiableMap(collected);
        }
        /** @return empty parameter set */ public static Parameters empty() {
            return new Parameters(Collections.<Parameter>emptyList());
        }
        /** @return immutable unique parameter set */ public static Parameters of(
                final Collection<Parameter> parameters) {
            return new Parameters(new ArrayList<Parameter>(
                    java.util.Objects.requireNonNull(parameters, "parameters")));
        }
        /** @return parameter names */ public java.util.Set<String> names() { return values.keySet(); }
        /** @return parameter by name */ public Optional<Parameter> find(final String name) {
            return Optional.ofNullable(values.get(java.util.Objects.requireNonNull(name, "name")));
        }
    }

    /** Immutable typed scalar localization parameter. */
    final class Parameter {
        private final String name;
        private final Kind kind;
        private final String value;
        private Parameter(final String name, final Kind kind, final String value) {
            if (name == null || !name.matches("[a-z][a-z0-9_.-]{0,63}")) {
                throw new IllegalArgumentException("Invalid localization parameter name");
            }
            this.name = name;
            this.kind = java.util.Objects.requireNonNull(kind, "kind");
            this.value = java.util.Objects.requireNonNull(value, "value");
        }
        /** @return plain-text parameter */ public static Parameter text(final String name,
                                                                          final String value) {
            return new Parameter(name, Kind.TEXT, value);
        }
        /** @return decimal parameter with canonical representation */
        public static Parameter number(final String name, final BigDecimal value) {
            return new Parameter(name, Kind.NUMBER,
                    java.util.Objects.requireNonNull(value, "value").stripTrailingZeros().toPlainString());
        }
        /** @return boolean parameter */ public static Parameter bool(final String name,
                                                                       final boolean value) {
            return new Parameter(name, Kind.BOOLEAN, Boolean.toString(value));
        }
        /** @return stable name */ public String name() { return name; }
        /** @return scalar kind */ public Kind kind() { return kind; }
        /** @return canonical scalar value without trusted markup */ public String value() { return value; }

        /** Supported scalar parameter kinds. */
        public enum Kind {
            /** Untrusted plain text. */ TEXT,
            /** Canonical decimal number. */ NUMBER,
            /** Canonical boolean. */ BOOLEAN
        }
    }
}
