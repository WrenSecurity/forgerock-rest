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
 * Portions Copyright 2017 Wren Security.
 */
package org.forgerock.json.resource;

/**
 * A registry of internal connections.
 * <p>
 * Server contexts may be persisted to long term storage and so cannot store a direct
 * reference to an internal connection, since a {@code Connection} cannot be
 * serialized to JSON. Instead, they store the handle, or connection ID, of the
 * internal connection. This connection ID is used as a key for obtaining the
 * internal connection from a {@code ConnectionProvider}.
 *
 * @see ServerContext
 */
public interface ConnectionProvider {

    /**
     * Returns the internal connection having the provided connection ID.
     *
     * @param connectionId
     *            The connection ID.
     * @return The internal connection.
     * @throws NotFoundException
     *             If no such connection exists.
     * @throws ResourceException
     *             If the connection could not be obtained for some other reason
     *             (e.g. due to a configuration or initialization problem).
     */
    Connection getConnection(String connectionId) throws ResourceException;

    /**
     * Returns the connection ID which should be used for identifying the
     * provided internal connection.
     *
     * @param connection
     *            The internal connection.
     * @return The connection ID.
     * @throws NotFoundException
     *             If no such connection exists.
     * @throws ResourceException
     *             If the connection ID could not be obtained for some other
     *             reason (e.g. due to a configuration or initialization
     *             problem).
     */
    String getConnectionId(Connection connection) throws ResourceException;
}
