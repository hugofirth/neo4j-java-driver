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
 * A <em>DatabaseException</em> indicates that there is a problem within the underlying database.
 * The error code provided can be used to determine further detail for the problem.
 * @since 1.0
 */
public class DatabaseException extends Neo4jException {
    @Serial
    private static final long serialVersionUID = 4591061578560201032L;

    public DatabaseException(String code, String message) {
        super(code, message);
    }
}
