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
package org.neo4j.docs.driver;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;

import java.util.concurrent.TimeUnit;

public class ConfigConnectionPoolExample implements AutoCloseable {
    private final Driver driver;

    // tag::config-connection-pool[]
    public ConfigConnectionPoolExample(String uri, String user, String password) {
        var config = Config.builder()
                .withMaxConnectionLifetime(30, TimeUnit.MINUTES)
                .withMaxConnectionPoolSize(50)
                .withConnectionAcquisitionTimeout(2, TimeUnit.MINUTES)
                .build();

        driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password), config);
    }
    // end::config-connection-pool[]

    @Override
    public void close() throws RuntimeException {
        driver.close();
    }

    public boolean canConnect() {
        var result = driver.session().run("RETURN 1");
        return result.single().get(0).asInt() == 1;
    }
}
