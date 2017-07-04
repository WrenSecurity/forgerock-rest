/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2012-2014 ForgeRock AS. All rights reserved.
 * Portions Copyright © 2017 Wren Security.
 */
package org.forgerock.json.resource;

import java.lang.reflect.Constructor;
import java.util.HashMap;

import org.forgerock.json.fluent.JsonValue;

/**
 * A base implementation of the context associated with a request currently
 * being processed by a JSON resource provider. A request context can be used to
 * query state information about the request. Implementations may provide
 * additional information, time-stamp information, HTTP headers, etc. Contexts
 * are linked together to form a parent-child chain of context, whose root is a
 * {@link RootContext}.
 * <p>
 * Derived Contexts <b>MUST</b> support persistence by providing
 * <ul>
 * <li>a <b>public</b> constructor having the same declaration as
 * {@link #AbstractContext(JsonValue, PersistenceConfig)}</li>
 * <li>a <b>public</b> method having the same declaration as
 * {@link org.forgerock.json.resource.Context#toJsonValue()}. See the
 * documentation for more details.</li>
 * </ul>
 * <p>
 * Here is an example of the JSON representation of the core attributes of all
 * contexts:
 *
 * <pre>
 * {
 *   "id"     : "56f0fb7e-3837-464d-b9ec-9d3b6af665c3",
 *   "class"  : ...Java class name..,
 *   "parent" : {
 *       ...
 *   }
 * }
 * </pre>
 */
public abstract class AbstractContext implements Context {
    // Persisted attribute names.
    private static final String ATTR_CLASS = "class";
    private static final String ATTR_ID = "id";
    private static final String ATTR_PARENT = "parent";

    private static Context load0(final JsonValue savedContext, final PersistenceConfig config)
            throws ResourceException {
        // Determine the context implementation class and instantiate it.
        final String className = savedContext.get(ATTR_CLASS).required().asString();
        try {
            final Class<? extends Context> clazz = Class.forName(className, true,
                    config.getClassLoader()).asSubclass(AbstractContext.class);
            final Constructor<? extends Context> constructor = clazz.getDeclaredConstructor(
                    JsonValue.class, PersistenceConfig.class);
            return constructor.newInstance(savedContext, config);
        } catch (final Exception e) {
            throw new IllegalArgumentException(
                    "Unable to instantiate Context implementation class '" + className + "'", e);
        }
    }

    /**
     * The parent Context.
     */
    private final Context parent;

    /**
     * The Context data.
     */
    protected final JsonValue data;

    /**
     * Creates a new context having the provided parent and no ID.
     *
     * @param parent
     *            The parent context.
     */
    protected AbstractContext(final Context parent) {
        this(null, parent);
    }

    /**
     * Creates a new context having the provided ID, and parent.
     *
     * @param id
     *            The context ID.
     * @param parent
     *            The parent context.
     */
    protected AbstractContext(final String id, final Context parent) {
        final int size = (id == null) ? 1 : 2;
        this.data = new JsonValue(new HashMap<String, Object>(size));
        this.data.put(ATTR_CLASS, this.getClass().getName());
        if (id != null) {
            this.data.put(ATTR_ID, id);
        }
        this.parent = parent;
    }

    /**
     * Creates a new context from the JSON representation of a previously
     * persisted context.
     * <p>
     * Sub-classes <b>MUST</b> provide a constructor having the same declaration
     * as this constructor in order to support persistence. Implementations
     * <b>MUST</b> take care to invoke the super class implementation before
     * parsing their own context attributes. Below is an example implementation
     * for a security context which stores the user name and password of the
     * authenticated user:
     *
     * <pre>
     * protected SecurityContext(JsonValue savedContext, PersistenceConfig config)
     *         throws ResourceException {
     *     // Invoke the super-class implementation first.
     *     super.saveToJson(savedContext, config);
     *
     *     // Now parse the attributes for this context.
     *     this.username = savedContext.get(&quot;username&quot;).required().asString();
     *     this.password = savedContext.get(&quot;password&quot;).required().asString();
     * }
     * </pre>
     *
     * In order to creation of a context's persisted JSON representation,
     * implementations must override
     *
     * @param savedContext
     *            The JSON representation from which this context's attributes
     *            should be parsed.
     * @param config
     *            The persistence configuration.
     * @throws ResourceException
     *             If the JSON representation could not be parsed.
     */
    protected AbstractContext(final JsonValue savedContext, final PersistenceConfig config)
            throws ResourceException {
        final JsonValue savedParentContext = savedContext.get(ATTR_PARENT);
        savedContext.remove(ATTR_PARENT);
        this.parent = savedParentContext.isNull() ? null : load0(savedParentContext, config);
        this.data = savedContext;
    }

    /**
     * Get this Context's name.
     *
     * @return this object's name
     */
    public abstract String getContextName();

    /**
     * Returns the first context in the chain whose type is a sub-type of the
     * provided {@code Context} class. The method first checks this context to
     * see if it has the required type, before proceeding to the parent context,
     * and then continuing up the chain of parents until the root context is
     * reached.
     *
     * @param <T>
     *            The context type.
     * @param clazz
     *            The class of context to be returned.
     * @return The first context in the chain whose type is a sub-type of the
     *         provided {@code Context} class.
     * @throws IllegalArgumentException
     *             If no matching context was found in this context's parent
     *             chain.
     */
    public final <T extends Context> T asContext(final Class<T> clazz) {
        final T context = asContext0(clazz);
        if (context != null) {
            return context;
        } else {
            throw new IllegalArgumentException("No context of type " + clazz.getName() + " found.");
        }
    }

    /**
     * Returns the first context in the chain whose context name matches the
     * provided name.
     *
     * @param contextName
     *            The name of the context to be returned.
     * @return The first context in the chain whose name matches the
     *         provided context name.
     * @throws IllegalArgumentException
     *             If no matching context was found in this context's parent
     *             chain.
     */
    public final Context getContext(final String contextName) {
        final Context context = getContext0(contextName);
        if (context != null) {
            return context;
        } else {
            throw new IllegalArgumentException("No context of named " + contextName + " found.");
        }
    }

    /**
     * Returns {@code true} if there is a context in the chain whose type is a
     * sub-type of the provided {@code Context} class. The method first checks
     * this context to see if it has the required type, before proceeding to the
     * parent context, and then continuing up the chain of parents until the
     * root context is reached.
     *
     * @param clazz
     *            The class of context to be checked.
     * @return {@code true} if there is a context in the chain whose type is a
     *         sub-type of the provided {@code Context} class.
     */
    public final boolean containsContext(final Class<? extends Context> clazz) {
        return asContext0(clazz) != null;
    }

    /**
     * Returns {@code true} if there is a context in the chain whose context name is
     * matches the provided context name.
     *
     * @param contextName
     *            The name of the context to locate.
     * @return {@code true} if there is a context in the chain whose context name
     *            matches {@code contextName}.
     */
    public final boolean containsContext(final String contextName) {
        return getContext0(contextName) != null;
    }

    /**
     * Returns the unique ID identifying this context.  Ids are not required for
     * all contexts in a context chain.  If this context's id is null, we will
     * return the parent's id until we reach the RootContext, which always has
     * an id, usually a UUID.
     *
     * @return The unique ID identifying this context.
     */
    public final String getId() {
        return data.get(ATTR_ID).isNull() && !isRootContext()
                ? getParent().getId()
                : data.get(ATTR_ID).required().asString();
    }

    /**
     * Returns the parent of this context.
     *
     * @return The parent of this context, or {@code null} if this context is a
     *         root context.
     */
    public final Context getParent() {
        return parent;
    }

    /**
     * Returns {@code true} if this context is a root context.
     *
     * @return {@code true} if this context is a root context.
     */
    public final boolean isRootContext() {
        return getParent() == null;
    }

    /**
     * Return this Context as a JsonValue (for persistence).
     *
     * @return the Context data as a JsonValue.
     */
    public JsonValue toJsonValue() {
        final JsonValue value = data.copy();
        value.put(ATTR_PARENT, parent != null ? parent.toJsonValue().getObject() : null);
        return value;
    }

    @Override
    public String toString() {
        return toJsonValue().toString();
    }

    private final <T extends Context> T asContext0(final Class<T> clazz) {
        try {
            for (Context context = this; context != null; context = context.getParent()) {
                final Class<?> contextClass = context.getClass();
                if (clazz.isAssignableFrom(contextClass)) {
                    return contextClass.asSubclass(clazz).cast(context);
                }
            }
        } catch (final Exception e) {
            throw new IllegalArgumentException(
                    "Unable to instantiate Context implementation class '" + clazz.getName().toString() + "'", e);
        }
        return null;
    }

    private final Context getContext0(final String contextName) {
        for (Context context = this; context != null; context = context.getParent()) {
            if (context.getContextName().equals(contextName)) {
                return context;
            }
        }
        return null;
    }
}
