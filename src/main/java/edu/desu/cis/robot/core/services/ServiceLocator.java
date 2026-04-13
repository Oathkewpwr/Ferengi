package edu.desu.cis.robot.core.services;

import java.util.HashMap;
import java.util.Map;

/**
 * A simple service locator pattern implementation for registering and retrieving services.
 * This class provides a centralized way to manage service instances, making them accessible
 * throughout the application.
 * @author Marwan Rasamny
 * @version 0.5.0
 */
public class ServiceLocator {

    private static final Map<Class<?>, Object> services = new HashMap<>();

    /**
     * Registers a service with the service locator.
     * @param clazz The class type of the service to register.
     * @param service The instance of the service to register.
     * @param <T> The type of the service.
     */
    public static <T> void register(Class<T> clazz, T service) {
        services.put(clazz, service);
    }

    /**
     * Retrieves a registered service by its class type.
     * @param clazz The class type of the service to retrieve.
     * @param <T> The type of the service.
     * @return The instance of the registered service.
     * @throws IllegalStateException if no service is registered for the given class type.
     */
    public static <T> T get(Class<T> clazz) {
        T service = clazz.cast(services.get(clazz));
        if (service == null) {
            throw new IllegalStateException(
                    "No service registered for " + clazz.getName()
            );
        }
        return service;
    }
}