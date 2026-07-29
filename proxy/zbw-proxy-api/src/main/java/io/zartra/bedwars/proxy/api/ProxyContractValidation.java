package io.zartra.bedwars.proxy.api;

import java.util.Objects;
import java.util.regex.Pattern;

/** Shared validation for privacy-safe proxy contract values. */
final class ProxyContractValidation {
private static final Pattern TOKEN = Pattern.compile("[a-z0-9][a-z0-9._:-]{0,127}");
private ProxyContractValidation() {
}
static String token(final String value, final String name) {
String checked = Objects.requireNonNull(value, name);
if (!TOKEN.matcher(checked).matches()) {
throw new IllegalArgumentException(name + " must be a lowercase opaque token");
}
return checked;
}
}
