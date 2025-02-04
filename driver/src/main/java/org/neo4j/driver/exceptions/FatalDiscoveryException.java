/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.driver.exceptions;

import java.io.Serial;

/**
 * This error indicate a fatal problem to obtain routing tables such as the routing table for a specified database does not exist.
 * This exception should not be retried.
 * @since 4.0
 */
public class FatalDiscoveryException extends ClientException {
    @Serial
    private static final long serialVersionUID = -2831830142554054420L;

    public FatalDiscoveryException(String message) {
        super(message);
    }

    public FatalDiscoveryException(String code, String message) {
        super(code, message);
    }
}
