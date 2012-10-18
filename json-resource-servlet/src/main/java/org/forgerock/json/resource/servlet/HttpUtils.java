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
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2012 ForgeRock AS.
 */
package org.forgerock.json.resource.servlet;

import java.io.Closeable;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.ResourceException;

/**
 * HTTP utility methods and constants.
 */
final class HttpUtils {

    static final String CHARACTER_ENCODING = "UTF-8";

    static final String CONTENT_TYPE = "application/json";
    static final String CRLF = "\r\n";

    static final String ETAG_ANY = "*";
    static final String HEADER_ETAG = "ETag";
    static final String HEADER_LOCATION = "Location";
    static final String HEADER_IF_MATCH = "If-Match";
    static final String HEADER_IF_NONE_MATCH = "If-None-Match";

    static final String HEADER_X_HTTP_METHOD_OVERRIDE = "X-HTTP-Method-Override";
    static final String METHOD_DELETE = "DELETE";
    static final String METHOD_GET = "GET";
    static final String METHOD_HEAD = "HEAD";
    static final String METHOD_OPTIONS = "OPTIONS";
    static final String METHOD_PATCH = "PATCH";

    static final String METHOD_POST = "POST";
    static final String METHOD_PUT = "PUT";

    static final String METHOD_TRACE = "TRACE";
    static final String PARAM_ACTION = "_action";
    static final String PARAM_DEBUG = "_debug";

    static final String PARAM_PRETTY_PRINT = "_prettyPrint";

    /**
     * Adapts an {@code Exception} to a {@code ResourceException}.
     *
     * @param e
     *            The exception which caused the request to fail.
     * @return The equivalent resource exception.
     */
    static ResourceException adapt(final Exception e) {
        if (e instanceof ResourceException) {
            return (ResourceException) e;
        } else {
            return new InternalServerErrorException(e);
        }
    }

    /**
     * Parses a header or request parameter as a boolean value.
     *
     * @param name
     *            The name of the header or parameter.
     * @param values
     *            The header or parameter values.
     * @return The boolean value.
     * @throws ResourceException
     *             If the value could not be parsed as a boolean.
     */
    static boolean asBooleanValue(final String name, final String[] values)
            throws ResourceException {
        final String value = asSingleValue(name, values);
        return Boolean.parseBoolean(value);
    }

    /**
     * Parses a header or request parameter as an integer value.
     *
     * @param name
     *            The name of the header or parameter.
     * @param values
     *            The header or parameter values.
     * @return The integer value.
     * @throws ResourceException
     *             If the value could not be parsed as a integer.
     */
    static int asIntValue(final String name, final String[] values) throws ResourceException {
        final String value = asSingleValue(name, values);
        try {
            return Integer.parseInt(value);
        } catch (final NumberFormatException e) {
            // FIXME: i18n.
            throw new BadRequestException("The value \'" + value + "\' for parameter '" + name
                    + "' could not be parsed as a valid integer");
        }
    }

    /**
     * Parses a header or request parameter as a single string value.
     *
     * @param name
     *            The name of the header or parameter.
     * @param values
     *            The header or parameter values.
     * @return The single string value.
     * @throws ResourceException
     *             If the value could not be parsed as a single string.
     */
    static String asSingleValue(final String name, final String[] values) throws ResourceException {
        if (values == null || values.length == 0) {
            // FIXME: i18n.
            throw new BadRequestException("No values provided for the request parameter \'" + name
                    + "\'");
        } else if (values.length > 1) {
            // FIXME: i18n.
            throw new BadRequestException(
                    "Multiple values provided for the single-valued request parameter \'" + name
                            + "\'");
        }
        return values[0];
    }

    /**
     * Throws a {@code NullPointerException} if the provided object is
     * {@code null}.
     *
     * @param <T>
     *            The parameter type.
     * @param object
     *            The object to test.
     * @return The object if not {@code null}.
     * @throws NullPointerException
     *             If {@code object} is {@code null}.
     */
    static final <T> T checkNotNull(final T object) {
        if (object == null) {
            throw new NullPointerException();
        }
        return object;
    }

    /**
     * Closes the provided {@code Closeable}s ignoring null values and any
     * exceptions.
     *
     * @param closeables
     *            The {@code Closeable}s to be closed.
     */
    static void closeQuietly(final Closeable... closeables) {
        for (final Closeable closeable : closeables) {
            if (closeable != null) {
                try {
                    closeable.close();
                } catch (final Exception ignored) {
                    // Ignore.
                }
            }
        }
    }

    /**
     * Safely fail an HTTP request using the provided {@code Exception}.
     *
     * @param resp
     *            The HTTP response.
     * @param e
     *            The resource exception indicating why the request failed.
     */
    static void fail(final HttpServletResponse resp, final Exception e) {
        final ResourceException re = adapt(e);
        try {
            resp.sendError(re.getCode(), re.getMessage());
        } catch (final IOException ignored) {
            // Ignore the error since this was probably the cause.
        }
    }

    /**
     * Returns the effective method name for an HTTP request taking into account
     * the "X-HTTP-Method-Override" header.
     *
     * @param req
     *            The HTTP request.
     * @return The effective method name.
     */
    static String getMethod(final HttpServletRequest req) {
        String method = req.getMethod();
        if (HttpUtils.METHOD_POST.equals(method)
                && req.getHeader(HttpUtils.HEADER_X_HTTP_METHOD_OVERRIDE) != null) {
            method = req.getHeader(HttpUtils.HEADER_X_HTTP_METHOD_OVERRIDE);
        }
        return method;
    }

    /**
     * Determines whether debugging was requested in an HTTP request.
     *
     * @param req
     *            The HTTP request.
     * @return {@code true} if debugging was requested.
     * @throws ResourceException
     *             If the debug parameter could not be parsed.
     */
    static boolean isDebugRequested(final HttpServletRequest req) throws ResourceException {
        final String[] values = req.getParameterValues(PARAM_DEBUG);
        return (values != null && asBooleanValue(PARAM_DEBUG, values));
    }

    private HttpUtils() {
        // Prevent instantiation.
    }

}