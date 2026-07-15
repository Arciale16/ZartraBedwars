package io.zartra.bedwars.storage.sql;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.result.ApiError;
import io.zartra.bedwars.api.result.Result;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.sql.DataSource;

/**
 * Java-8-linkable bridge to the approved Flyway runtime.
 *
 * <p>Flyway 10 is loaded only when the selected runtime supports its class-file level and required
 * database plugin. The built-in checksum runner remains the Java 8 migration path. A missing or
 * incompatible Flyway runtime produces a typed failure and never silently skips migration.</p>
 */
public final class FlywayMigrationProvider {
    private static final ApiError UNAVAILABLE = ApiError.of(
            DefinitionId.of("zartra", "storage.flyway_unavailable"),
            "storage.error.flyway_unavailable", ApiError.RetryDisposition.PERMANENT);

    private FlywayMigrationProvider() { }

    /** Applies Flyway migrations from an explicit classpath location. */
    public static Result<Boolean> migrate(final DataSource dataSource, final String location) {
        if (dataSource == null) { throw new NullPointerException("dataSource"); }
        if (location == null || !location.matches("classpath:[a-zA-Z0-9_./-]+")) {
            throw new IllegalArgumentException("location must be a bounded classpath location");
        }
        try {
            final Class<?> flyway = Class.forName("org.flywaydb.core.Flyway");
            Object configuration = flyway.getMethod("configure").invoke(null);
            final Method dataSourceMethod = configuration.getClass()
                    .getMethod("dataSource", DataSource.class);
            configuration = dataSourceMethod.invoke(configuration, dataSource);
            configuration = configuration.getClass().getMethod("locations", String[].class)
                    .invoke(configuration, new Object[] {new String[] {location}});
            configuration = configuration.getClass().getMethod("validateMigrationNaming", boolean.class)
                    .invoke(configuration, true);
            final Object instance = configuration.getClass().getMethod("load").invoke(configuration);
            flyway.getMethod("migrate").invoke(instance);
            return Result.success(Boolean.TRUE);
        } catch (ClassNotFoundException exception) {
            return Result.failure(UNAVAILABLE);
        } catch (LinkageError exception) {
            return Result.failure(UNAVAILABLE);
        } catch (NoSuchMethodException exception) {
            return Result.failure(UNAVAILABLE);
        } catch (IllegalAccessException exception) {
            return Result.failure(UNAVAILABLE);
        } catch (InvocationTargetException exception) {
            return Result.failure(UNAVAILABLE);
        }
    }
}
