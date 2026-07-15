package io.zartra.bedwars.config.localization;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.localization.LocaleId;
import io.zartra.bedwars.api.localization.LocalizationService;
import io.zartra.bedwars.api.localization.MessageKey;
import io.zartra.bedwars.api.result.ApiError;
import io.zartra.bedwars.api.result.Result;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Thread-safe server/per-player localization service with complete fallback catalogs. */
public final class DefaultLocalizationService implements LocalizationService {
    private static final Pattern PARAMETER = Pattern.compile("\\{([a-z][a-z0-9_.-]{0,63})\\}");
    private final LocaleId fallback;
    private final Map<LocaleId, Catalog> catalogs;
    private final Map<PlayerId, LocaleId> playerLocales = new LinkedHashMap<PlayerId, LocaleId>();
    private LocaleId serverLocale;

    /**
     * Creates a service with immutable catalogs.
     *
     * @param fallback complete final fallback locale
     * @param serverLocale initial server locale, complete against fallback
     * @param catalogs unique locale catalogs
     */
    public DefaultLocalizationService(final LocaleId fallback, final LocaleId serverLocale,
                                      final Collection<Catalog> catalogs) {
        this.fallback = Objects.requireNonNull(fallback, "fallback");
        final Map<LocaleId, Catalog> collected = new TreeMap<LocaleId, Catalog>();
        for (Catalog catalog : Objects.requireNonNull(catalogs, "catalogs")) {
            final Catalog checked = Objects.requireNonNull(catalog, "catalog");
            if (collected.put(checked.locale(), checked) != null) {
                throw new IllegalArgumentException("Duplicate locale catalog");
            }
        }
        if (!collected.containsKey(fallback)) {
            throw new IllegalArgumentException("Fallback catalog is required");
        }
        this.catalogs = Collections.unmodifiableMap(collected);
        ensureComplete(Objects.requireNonNull(serverLocale, "serverLocale"));
        this.serverLocale = serverLocale;
    }

    @Override
    public synchronized Result<LocalizedMessage> render(final MessageKey key,
                                                        final Optional<PlayerId> player,
                                                        final Parameters parameters) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(parameters, "parameters");
        LocaleId selected = serverLocale;
        if (player.isPresent() && playerLocales.containsKey(player.get())) {
            selected = playerLocales.get(player.get());
        }
        String template = catalogs.get(selected).template(key).orElse(null);
        LocaleId renderedLocale = selected;
        if (template == null) {
            template = catalogs.get(fallback).template(key).orElse(null);
            renderedLocale = fallback;
        }
        if (template == null) {
            return Result.failure(error("localization/missing_key", "localization.missing-key"));
        }
        final Set<String> required = parameterNames(template);
        if (!required.equals(parameters.names())) {
            return Result.failure(error("localization/parameter_mismatch",
                    "localization.parameter-mismatch"));
        }
        final Matcher matcher = PARAMETER.matcher(template);
        final StringBuffer rendered = new StringBuffer();
        while (matcher.find()) {
            final String replacement = escape(parameters.find(matcher.group(1)).get().value());
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(rendered);
        return Result.success(LocalizedMessage.of(renderedLocale, key, rendered.toString()));
    }

    @Override
    public synchronized Result<LocaleId> switchServerLocale(final LocaleId locale) {
        try {
            ensureComplete(Objects.requireNonNull(locale, "locale"));
            serverLocale = locale;
            return Result.success(locale);
        } catch (IllegalArgumentException exception) {
            return Result.failure(error("localization/incomplete_catalog",
                    "localization.incomplete-catalog"));
        }
    }

    @Override
    public synchronized Result<LocaleId> switchPlayerLocale(final PlayerId player,
                                                            final LocaleId locale) {
        Objects.requireNonNull(player, "player");
        try {
            ensureComplete(Objects.requireNonNull(locale, "locale"));
            playerLocales.put(player, locale);
            return Result.success(locale);
        } catch (IllegalArgumentException exception) {
            return Result.failure(error("localization/incomplete_catalog",
                    "localization.incomplete-catalog"));
        }
    }

    /** @return completeness report for a catalog against the final fallback */
    public CompletenessReport completeness(final LocaleId locale) {
        final Catalog candidate = catalogs.get(Objects.requireNonNull(locale, "locale"));
        if (candidate == null) {
            return CompletenessReport.missingCatalog(locale);
        }
        return CompletenessReport.compare(candidate, catalogs.get(fallback));
    }

    private void ensureComplete(final LocaleId locale) {
        if (!completeness(locale).isComplete()) {
            throw new IllegalArgumentException("Locale catalog is incomplete");
        }
    }
    private static Set<String> parameterNames(final String template) {
        final Set<String> result = new TreeSet<String>();
        final Matcher matcher = PARAMETER.matcher(template);
        while (matcher.find()) { result.add(matcher.group(1)); }
        return result;
    }
    private static String escape(final String value) {
        return value.replace("\\", "\\\\").replace("<", "\\<").replace(">", "\\>")
                .replace("&", "\\&");
    }
    private static ApiError error(final String code, final String messageKey) {
        return ApiError.of(DefinitionId.of("zartra", code), messageKey,
                ApiError.RetryDisposition.PERMANENT);
    }

    /** Immutable validated locale catalog. */
    public static final class Catalog {
        private final LocaleId locale;
        private final Map<MessageKey, String> templates;
        private Catalog(final LocaleId locale, final Map<MessageKey, String> templates) {
            this.locale = Objects.requireNonNull(locale, "locale");
            final Map<MessageKey, String> copy = new TreeMap<MessageKey, String>();
            for (Map.Entry<MessageKey, String> entry
                    : Objects.requireNonNull(templates, "templates").entrySet()) {
                final MessageKey key = Objects.requireNonNull(entry.getKey(), "key");
                final String template = Objects.requireNonNull(entry.getValue(), "template");
                validateTemplate(template);
                if (copy.put(key, template) != null) {
                    throw new IllegalArgumentException("Duplicate message key");
                }
            }
            if (copy.isEmpty()) { throw new IllegalArgumentException("Catalog must contain messages"); }
            this.templates = Collections.unmodifiableMap(copy);
        }
        /** @return validated immutable catalog */
        public static Catalog of(final LocaleId locale, final Map<MessageKey, String> templates) {
            return new Catalog(locale, templates);
        }
        /** @return locale */ public LocaleId locale() { return locale; }
        /** @return template by stable key */ public Optional<String> template(final MessageKey key) {
            return Optional.ofNullable(templates.get(Objects.requireNonNull(key, "key")));
        }
        /** @return deterministic message keys */ public Set<MessageKey> keys() { return templates.keySet(); }
        /** @return deterministic immutable templates */ public Map<MessageKey, String> templates() { return templates; }
        private static void validateTemplate(final String template) {
            if (template.isEmpty() || template.indexOf('\n') >= 0 || template.indexOf('\r') >= 0) {
                throw new IllegalArgumentException("Template must be one non-empty logical line");
            }
            final String stripped = PARAMETER.matcher(template).replaceAll("");
            if (stripped.indexOf('{') >= 0 || stripped.indexOf('}') >= 0) {
                throw new IllegalArgumentException("Template contains malformed parameter syntax");
            }
        }
    }

    /** Immutable catalog completeness result. */
    public static final class CompletenessReport {
        private final LocaleId locale;
        private final boolean catalogPresent;
        private final List<MessageKey> missing;
        private final List<MessageKey> additional;
        private CompletenessReport(final LocaleId locale, final boolean catalogPresent,
                                   final Collection<MessageKey> missing,
                                   final Collection<MessageKey> additional) {
            this.locale = locale;
            this.catalogPresent = catalogPresent;
            final List<MessageKey> missingCopy = new ArrayList<MessageKey>(missing);
            final List<MessageKey> additionalCopy = new ArrayList<MessageKey>(additional);
            Collections.sort(missingCopy);
            Collections.sort(additionalCopy);
            this.missing = Collections.unmodifiableList(missingCopy);
            this.additional = Collections.unmodifiableList(additionalCopy);
        }
        private static CompletenessReport compare(final Catalog candidate, final Catalog fallback) {
            final Set<MessageKey> missing = new TreeSet<MessageKey>(fallback.keys());
            missing.removeAll(candidate.keys());
            final Set<MessageKey> additional = new TreeSet<MessageKey>(candidate.keys());
            additional.removeAll(fallback.keys());
            return new CompletenessReport(candidate.locale(), true, missing, additional);
        }
        private static CompletenessReport missingCatalog(final LocaleId locale) {
            return new CompletenessReport(locale, false, Collections.<MessageKey>emptyList(),
                    Collections.<MessageKey>emptyList());
        }
        /** @return locale inspected */ public LocaleId locale() { return locale; }
        /** @return whether a catalog exists */ public boolean catalogPresent() { return catalogPresent; }
        /** @return fallback keys absent from the catalog */ public List<MessageKey> missingKeys() { return missing; }
        /** @return catalog-specific additional keys */ public List<MessageKey> additionalKeys() { return additional; }
        /** @return whether switching to this catalog is safe */ public boolean isComplete() {
            return catalogPresent && missing.isEmpty();
        }
    }

    /** Deterministic UTF-8-neutral line codec; adapters own Reader, Writer and filesystem I/O. */
    public static final class CatalogCodec {
        /** @return canonical export with escaped keys and templates */
        public String exportCatalog(final Catalog catalog) {
            final StringBuilder output = new StringBuilder();
            output.append("# locale=").append(catalog.locale()).append('\n');
            for (Map.Entry<MessageKey, String> entry : catalog.templates().entrySet()) {
                output.append(entry.getKey()).append('=').append(escapeValue(entry.getValue())).append('\n');
            }
            return output.toString();
        }
        /** @return validated imported catalog */
        public Catalog importCatalog(final LocaleId locale, final String serialized) {
            Objects.requireNonNull(serialized, "serialized");
            final Map<MessageKey, String> templates = new LinkedHashMap<MessageKey, String>();
            final String[] lines = serialized.split("\\r?\\n", -1);
            for (int index = 0; index < lines.length; index++) {
                final String line = lines[index];
                if (line.isEmpty() || line.startsWith("#")) { continue; }
                final int separator = unescapedSeparator(line);
                if (separator < 1) { throw new IllegalArgumentException("Malformed catalog line"); }
                final MessageKey key = MessageKey.of(line.substring(0, separator));
                final String value = unescapeValue(line.substring(separator + 1));
                if (templates.put(key, value) != null) {
                    throw new IllegalArgumentException("Duplicate imported message key");
                }
            }
            return Catalog.of(locale, templates);
        }
        private static int unescapedSeparator(final String line) {
            boolean escaped = false;
            for (int index = 0; index < line.length(); index++) {
                final char character = line.charAt(index);
                if (character == '=' && !escaped) { return index; }
                if (character == '\\' && !escaped) { escaped = true; }
                else { escaped = false; }
            }
            return -1;
        }
        private static String escapeValue(final String value) {
            return value.replace("\\", "\\\\").replace("=", "\\=")
                    .replace("\t", "\\t");
        }
        private static String unescapeValue(final String value) {
            final StringBuilder output = new StringBuilder();
            boolean escaped = false;
            for (int index = 0; index < value.length(); index++) {
                final char character = value.charAt(index);
                if (escaped) {
                    if (character == 't') { output.append('\t'); }
                    else if (character == '=' || character == '\\') { output.append(character); }
                    else { throw new IllegalArgumentException("Unsupported catalog escape"); }
                    escaped = false;
                } else if (character == '\\') {
                    escaped = true;
                } else {
                    output.append(character);
                }
            }
            if (escaped) { throw new IllegalArgumentException("Trailing catalog escape"); }
            return output.toString();
        }
    }
}
