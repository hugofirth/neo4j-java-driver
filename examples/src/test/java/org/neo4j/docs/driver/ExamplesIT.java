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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.internal.util.EnabledOnNeo4jWith;
import org.neo4j.driver.summary.QueryType;
import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.driver.testutil.DatabaseExtension;
import org.neo4j.driver.testutil.ParallelizableIT;
import org.neo4j.driver.testutil.StdIOCapture;
import org.neo4j.driver.testutil.TestUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.neo4j.driver.Values.parameters;
import static org.neo4j.driver.internal.util.Neo4jEdition.ENTERPRISE;
import static org.neo4j.driver.internal.util.Neo4jFeature.BOLT_V4;
import static org.neo4j.driver.testutil.TestUtil.await;
import static org.neo4j.driver.testutil.TestUtil.createDatabase;
import static org.neo4j.driver.testutil.TestUtil.dropDatabase;

@ParallelizableIT
class ExamplesIT {
    static final String USER = "neo4j";

    @RegisterExtension
    static final DatabaseExtension neo4j = new DatabaseExtension();

    private String uri;

    private int readInt(String database, final String query, final Value parameters) {
        SessionConfig sessionConfig;
        if (database == null) {
            sessionConfig = SessionConfig.defaultConfig();
        } else {
            sessionConfig = SessionConfig.forDatabase(database);
        }
        try (Session session = neo4j.driver().session(sessionConfig)) {
            return session.executeRead(
                    tx -> tx.run(query, parameters).single().get(0).asInt());
        }
    }

    private int readInt(final String query, final Value parameters) {
        return readInt(null, query, parameters);
    }

    private int readInt(final String query) {
        return readInt(query, parameters());
    }

    private void write(final String query, final Value parameters) {
        try (Session session = neo4j.driver().session()) {
            session.executeWriteWithoutResult(tx -> tx.run(query, parameters).consume());
        }
    }

    private void write(String query) {
        write(query, parameters());
    }

    private int personCount(String name) {
        return readInt("MATCH (a:Person {name: $name}) RETURN count(a)", parameters("name", name));
    }

    private int companyCount(String name) {
        return readInt("MATCH (a:Company {name: $name}) RETURN count(a)", parameters("name", name));
    }

    @BeforeEach
    void setUp() {
        uri = neo4j.uri().toString();
        TestUtil.cleanDb(neo4j.driver());
    }

    @Test
    void testShouldRunAutocommitTransactionExample() {
        // Given
        try (AutocommitTransactionExample example =
                new AutocommitTransactionExample(uri, USER, neo4j.adminPassword())) {
            // When
            example.addPerson("Alice");

            // Then
            assertThat(personCount("Alice"), greaterThan(0));
        }
    }

    @Test
    void testShouldRunAsyncAutocommitTransactionExample() {
        try (AsyncAutocommitTransactionExample example =
                new AsyncAutocommitTransactionExample(uri, USER, neo4j.adminPassword())) {
            // create some 'Product' nodes
            try (Session session = neo4j.driver().session()) {
                session.run("UNWIND ['Tesseract', 'Orb', 'Eye of Agamotto'] AS item "
                        + "CREATE (:Product {id: 0, title: item})");
            }

            // read all 'Product' nodes
            List<String> titles = await(example.readProductTitles());
            assertEquals(new HashSet<>(asList("Tesseract", "Orb", "Eye of Agamotto")), new HashSet<>(titles));
        }
    }

    @Test
    void testShouldAsyncRunResultConsumeExample() {
        // Given
        write("CREATE (a:Person {name: 'Alice'})");
        write("CREATE (a:Person {name: 'Bob'})");
        try (AsyncResultConsumeExample example = new AsyncResultConsumeExample(uri, USER, neo4j.adminPassword())) {
            // When
            List<String> names = await(example.getPeople());

            // Then
            assertThat(names, equalTo(asList("Alice", "Bob")));
        }
    }

    @Test
    void testShouldAsyncRunMultipleTransactionExample() {
        // Given
        write("CREATE (a:Person {name: 'Alice'})");
        write("CREATE (a:Person {name: 'Bob'})");
        try (AsyncRunMultipleTransactionExample example =
                new AsyncRunMultipleTransactionExample(uri, USER, neo4j.adminPassword())) {
            // When
            Integer nodesCreated = await(example.addEmployees("Acme"));

            // Then
            int employeeCount =
                    readInt("MATCH (emp:Person)-[WORKS_FOR]->(com:Company) WHERE com.name = 'Acme' RETURN count(emp)");
            assertThat(employeeCount, equalTo(2));
            assertThat(nodesCreated, equalTo(1));
        }
    }

    @Test
    void testShouldRunConfigConnectionPoolExample() {
        // Given
        try (ConfigConnectionPoolExample example = new ConfigConnectionPoolExample(uri, USER, neo4j.adminPassword())) {
            // Then
            assertTrue(example.canConnect());
        }
    }

    @Test
    void testShouldRunBasicAuthExample() {
        // Given
        try (BasicAuthExample example = new BasicAuthExample(uri, USER, neo4j.adminPassword())) {
            // Then
            assertTrue(example.canConnect());
        }
    }

    @Test
    void testShouldRunConfigConnectionTimeoutExample() {
        // Given
        try (ConfigConnectionTimeoutExample example =
                new ConfigConnectionTimeoutExample(uri, USER, neo4j.adminPassword())) {
            // Then
            assertThat(example, instanceOf(ConfigConnectionTimeoutExample.class));
        }
    }

    @Test
    void testShouldRunConfigMaxRetryTimeExample() {
        // Given
        try (ConfigMaxRetryTimeExample example = new ConfigMaxRetryTimeExample(uri, USER, neo4j.adminPassword())) {
            // Then
            assertThat(example, instanceOf(ConfigMaxRetryTimeExample.class));
        }
    }

    @Test
    void testShouldRunConfigTrustExample() {
        // Given
        try (ConfigTrustExample example = new ConfigTrustExample(uri, USER, neo4j.adminPassword())) {
            // Then
            assertThat(example, instanceOf(ConfigTrustExample.class));
        }
    }

    @Test
    void testShouldRunConfigUnencryptedExample() {
        // Given
        try (ConfigUnencryptedExample example = new ConfigUnencryptedExample(uri, USER, neo4j.adminPassword())) {
            // Then
            assertThat(example, instanceOf(ConfigUnencryptedExample.class));
        }
    }

    @Test
    void testShouldRunDriverLifecycleExample() {
        // Given
        try (DriverLifecycleExample example = new DriverLifecycleExample(uri, USER, neo4j.adminPassword())) {
            // Then
            assertThat(example, instanceOf(DriverLifecycleExample.class));
        }
    }

    @Test
    void testShouldRunHelloWorld() throws Exception {
        // Given
        try (HelloWorldExample greeter = new HelloWorldExample(uri, USER, neo4j.adminPassword())) {
            // When
            StdIOCapture stdIO = StdIOCapture.capture();

            try (stdIO) {
                greeter.printGreeting("hello, world");
            }

            // Then
            assertThat(stdIO.stdout().size(), equalTo(1));
            assertThat(stdIO.stdout().get(0), containsString("hello, world"));
        }
    }

    @Test
    void testShouldRunReadWriteTransactionExample() {
        // Given
        try (ReadWriteTransactionExample example = new ReadWriteTransactionExample(uri, USER, neo4j.adminPassword())) {
            // When
            long nodeID = example.addPerson("Alice");

            // Then
            assertThat(nodeID, greaterThanOrEqualTo(0L));
        }
    }

    @Test
    void testShouldRunResultConsumeExample() {
        // Given
        write("CREATE (a:Person {name: 'Alice'})");
        write("CREATE (a:Person {name: 'Bob'})");
        try (ResultConsumeExample example = new ResultConsumeExample(uri, USER, neo4j.adminPassword())) {
            // When
            List<String> names = example.getPeople();

            // Then
            assertThat(names, equalTo(asList("Alice", "Bob")));
        }
    }

    @Test
    void testShouldRunResultRetainExample() {
        // Given
        write("CREATE (a:Person {name: 'Alice'})");
        write("CREATE (a:Person {name: 'Bob'})");
        try (ResultRetainExample example = new ResultRetainExample(uri, USER, neo4j.adminPassword())) {
            // When
            example.addEmployees("Acme");

            // Then
            int employeeCount =
                    readInt("MATCH (emp:Person)-[WORKS_FOR]->(com:Company) WHERE com.name = 'Acme' RETURN count(emp)");
            assertThat(employeeCount, equalTo(2));
        }
    }

    @Test
    void testShouldRunTransactionFunctionExample() {
        // Given
        try (TransactionFunctionExample example = new TransactionFunctionExample(uri, USER, neo4j.adminPassword())) {
            // When
            example.addPerson("Alice");

            // Then
            assertThat(personCount("Alice"), greaterThan(0));
        }
    }

    @Test
    void testShouldConfigureTransactionTimeoutExample() {
        // Given
        try (TransactionTimeoutConfigExample example =
                new TransactionTimeoutConfigExample(uri, USER, neo4j.adminPassword())) {
            // When
            example.addPerson("Alice");

            // Then
            assertThat(personCount("Alice"), greaterThan(0));
        }
    }

    @Test
    void testShouldConfigureTransactionMetadataExample() {
        // Given
        try (TransactionMetadataConfigExample example =
                new TransactionMetadataConfigExample(uri, USER, neo4j.adminPassword())) {
            // When
            example.addPerson("Alice");

            // Then
            assertThat(personCount("Alice"), greaterThan(0));
        }
    }

    @Test
    void testShouldRunAsyncTransactionFunctionExample() throws Exception {
        try (AsyncTransactionFunctionExample example =
                new AsyncTransactionFunctionExample(uri, USER, neo4j.adminPassword())) {
            // create some 'Product' nodes
            try (Session session = neo4j.driver().session()) {
                session.run(
                        "UNWIND ['Infinity Gauntlet', 'Mjölnir'] AS item " + "CREATE (:Product {id: 0, title: item})");
            }

            StdIOCapture stdIOCapture = StdIOCapture.capture();

            // print all 'Product' nodes to fake stdout
            try (stdIOCapture) {
                ResultSummary summary = await(example.printAllProducts());
                assertEquals(QueryType.READ_ONLY, summary.queryType());
            }

            Set<String> capturedOutput = new HashSet<>(stdIOCapture.stdout());
            assertEquals(new HashSet<>(asList("Infinity Gauntlet", "Mjölnir")), capturedOutput);
        }
    }

    @Test
    void testPassBookmarksExample() {
        try (PassBookmarkExample example = new PassBookmarkExample(uri, USER, neo4j.adminPassword())) {
            // When
            example.addEmployAndMakeFriends();

            // Then
            assertThat(companyCount("Wayne Enterprises"), is(1));
            assertThat(companyCount("LexCorp"), is(1));
            assertThat(personCount("Alice"), is(1));
            assertThat(personCount("Bob"), is(1));

            int employeeCountOfWayne = readInt(
                    "MATCH (emp:Person)-[WORKS_FOR]->(com:Company) WHERE com.name = 'Wayne Enterprises' RETURN count(emp)");
            assertThat(employeeCountOfWayne, is(1));

            int employeeCountOfLexCorp = readInt(
                    "MATCH (emp:Person)-[WORKS_FOR]->(com:Company) WHERE com.name = 'LexCorp' RETURN count(emp)");
            assertThat(employeeCountOfLexCorp, is(1));

            int friendCount =
                    readInt("MATCH (a:Person {name: 'Alice'})-[:KNOWS]->(b:Person {name: 'Bob'}) RETURN count(a)");
            assertThat(friendCount, is(1));
        }
    }

    @Test
    @EnabledOnNeo4jWith(BOLT_V4)
    void testShouldRunRxAutocommitTransactionExample() {
        try (RxAutocommitTransactionExample example =
                new RxAutocommitTransactionExample(uri, USER, neo4j.adminPassword())) {
            // create some 'Product' nodes
            try (Session session = neo4j.driver().session()) {
                session.run("UNWIND ['Tesseract', 'Orb', 'Eye of Agamotto'] AS item "
                        + "CREATE (:Product {id: 0, title: item})");
            }

            // read all 'Product' nodes
            List<String> titles = await(example.readProductTitles());
            assertEquals(new HashSet<>(asList("Tesseract", "Orb", "Eye of Agamotto")), new HashSet<>(titles));
        }
    }

    @Test
    @EnabledOnNeo4jWith(BOLT_V4)
    void testShouldRunRxTransactionFunctionExampleReactor() throws Exception {
        try (RxTransactionFunctionExample example =
                new RxTransactionFunctionExample(uri, USER, neo4j.adminPassword())) {
            // create some 'Product' nodes
            try (Session session = neo4j.driver().session()) {
                session.run(
                        "UNWIND ['Infinity Gauntlet', 'Mjölnir'] AS item " + "CREATE (:Product {id: 0, title: item})");
            }

            StdIOCapture stdIOCapture = StdIOCapture.capture();

            // print all 'Product' nodes to fake stdout
            try (stdIOCapture) {
                final List<ResultSummary> summaryList = await(example.printAllProducts());
                assertThat(summaryList.size(), equalTo(1));
                ResultSummary summary = summaryList.get(0);
                assertEquals(QueryType.READ_ONLY, summary.queryType());
            }

            Set<String> capturedOutput = new HashSet<>(stdIOCapture.stdout());
            assertEquals(new HashSet<>(asList("Infinity Gauntlet", "Mjölnir")), capturedOutput);
        }
    }

    @Test
    @EnabledOnNeo4jWith(BOLT_V4)
    void testShouldRunRxResultConsumeExampleReactor() {
        // Given
        write("CREATE (a:Person {name: 'Alice'})");
        write("CREATE (a:Person {name: 'Bob'})");
        try (RxResultConsumeExample example = new RxResultConsumeExample(uri, USER, neo4j.adminPassword())) {
            // When
            List<String> names = await(example.getPeople());

            // Then
            assertThat(names, equalTo(asList("Alice", "Bob")));
        }
    }

    @Test
    @EnabledOnNeo4jWith(value = BOLT_V4, edition = ENTERPRISE)
    void testUseAnotherDatabaseExample() {
        Driver driver = neo4j.driver();
        dropDatabase(driver, "examples");
        createDatabase(driver, "examples");

        try (DatabaseSelectionExample example = new DatabaseSelectionExample(uri, USER, neo4j.adminPassword())) {
            // When
            example.useAnotherDatabaseExample();

            // Then
            int greetingCount = readInt("examples", "MATCH (a:Greeting) RETURN count(a)", Values.parameters());
            assertThat(greetingCount, is(1));
        }
    }
}
