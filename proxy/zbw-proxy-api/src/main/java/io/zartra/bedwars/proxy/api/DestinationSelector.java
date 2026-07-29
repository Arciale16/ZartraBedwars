package io.zartra.bedwars.proxy.api;

import java.util.List;
import java.util.Optional;

/** Deterministic policy port for choosing one eligible backend from an ordered snapshot. */ public interface DestinationSelector{
/** Selects without mutating registry or domain state. */ Optional<BackendRegistration> select(List<BackendRegistration> registrations,RoutingRequest request);
}
