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
package org.neo4j.driver.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.neo4j.driver.BookmarkManager;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Query;
import org.neo4j.driver.QueryConfig;
import org.neo4j.driver.QueryTask;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.RoutingControl;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.TransactionCallback;
import org.neo4j.driver.TransactionContext;
import org.neo4j.driver.summary.ResultSummary;

class InternalQueryTaskTest {
    @Test
    void shouldNotAcceptNullDriverOnInstantiation() {
        assertThrows(
                NullPointerException.class,
                () -> new InternalQueryTask(null, new Query("string"), QueryConfig.defaultConfig()));
    }

    @Test
    void shouldNotAcceptNullQueryOnInstantiation() {
        assertThrows(
                NullPointerException.class,
                () -> new InternalQueryTask(mock(Driver.class), null, QueryConfig.defaultConfig()));
    }

    @Test
    void shouldNotAcceptNullConfigOnInstantiation() {
        assertThrows(
                NullPointerException.class, () -> new InternalQueryTask(mock(Driver.class), new Query("string"), null));
    }

    @Test
    void shouldNotAcceptNullParameters() {
        var queryTask = new InternalQueryTask(mock(Driver.class), new Query("string"), QueryConfig.defaultConfig());
        assertThrows(NullPointerException.class, () -> queryTask.withParameters(null));
    }

    @Test
    void shouldUpdateParameters() {
        // GIVEN
        var query = new Query("string");
        var params = Map.<String, Object>of("$param", "value");
        var queryTask = new InternalQueryTask(mock(Driver.class), query, QueryConfig.defaultConfig());

        // WHEN
        queryTask = (InternalQueryTask) queryTask.withParameters(params);

        // THEN
        assertEquals(params, queryTask.parameters());
    }

    @Test
    void shouldNotAcceptNullConfig() {
        var queryTask = new InternalQueryTask(mock(Driver.class), new Query("string"), QueryConfig.defaultConfig());
        assertThrows(NullPointerException.class, () -> queryTask.withConfig(null));
    }

    @Test
    void shouldUpdateConfig() {
        // GIVEN
        var query = new Query("string");
        var queryTask = new InternalQueryTask(mock(Driver.class), query, QueryConfig.defaultConfig());
        var config = QueryConfig.builder().withDatabase("database").build();

        // WHEN
        queryTask = (InternalQueryTask) queryTask.withConfig(config);

        // THEN
        assertEquals(config, queryTask.config());
    }

    @ParameterizedTest
    @EnumSource(RoutingControl.class)
    @SuppressWarnings("unchecked")
    void shouldExecuteAndReturnResult(RoutingControl routingControl) {
        // GIVEN
        var driver = mock(Driver.class);
        var bookmarkManager = mock(BookmarkManager.class);
        given(driver.queryTaskBookmarkManager()).willReturn(bookmarkManager);
        var session = mock(Session.class);
        given(driver.session(any(SessionConfig.class))).willReturn(session);
        var txContext = mock(TransactionContext.class);
        BiFunction<Session, TransactionCallback<Object>, Object> executeMethod =
                switch (routingControl) {
                    case WRITERS -> Session::executeWrite;
                    case READERS -> Session::executeRead;
                };
        given(executeMethod.apply(session, any())).willAnswer(answer -> {
            TransactionCallback<?> txCallback = answer.getArgument(0);
            return txCallback.execute(txContext);
        });
        var result = mock(Result.class);
        given(txContext.run(any(Query.class))).willReturn(result);
        var keys = List.of("key");
        given(result.keys()).willReturn(keys);
        given(result.hasNext()).willReturn(true, false);
        var record = mock(Record.class);
        given(result.next()).willReturn(record);
        var summary = mock(ResultSummary.class);
        given(result.consume()).willReturn(summary);
        var query = new Query("string");
        var params = Map.<String, Object>of("$param", "value");
        var config = QueryConfig.builder()
                .withDatabase("db")
                .withImpersonatedUser("user")
                .withRouting(routingControl)
                .build();
        Collector<Record, Object, String> recordCollector = mock(Collector.class);
        var resultContainer = new Object();
        given(recordCollector.supplier()).willReturn(() -> resultContainer);
        BiConsumer<Object, Record> accumulator = mock(BiConsumer.class);
        given(recordCollector.accumulator()).willReturn(accumulator);
        var collectorResult = "0";
        Function<Object, String> finisher = mock(Function.class);
        given(finisher.apply(resultContainer)).willReturn(collectorResult);
        given(recordCollector.finisher()).willReturn(finisher);
        QueryTask.ResultFinisher<String, String> finisherWithSummary = mock(QueryTask.ResultFinisher.class);
        var expectedExecuteResult = "1";
        given(finisherWithSummary.finish(any(List.class), any(String.class), any(ResultSummary.class)))
                .willReturn(expectedExecuteResult);
        var queryTask = new InternalQueryTask(driver, query, config).withParameters(params);

        // WHEN
        var executeResult = queryTask.execute(recordCollector, finisherWithSummary);

        // THEN
        ArgumentCaptor<SessionConfig> sessionConfigCapture = ArgumentCaptor.forClass(SessionConfig.class);
        then(driver).should().session(sessionConfigCapture.capture());
        var sessionConfig = sessionConfigCapture.getValue();
        var expectedSessionConfig = SessionConfig.builder()
                .withDatabase(config.database().get())
                .withImpersonatedUser(config.impersonatedUser().get())
                .withBookmarkManager(bookmarkManager)
                .build();
        assertEquals(expectedSessionConfig, sessionConfig);
        executeMethod.apply(then(session).should(), any(TransactionCallback.class));
        then(txContext).should().run(query.withParameters(params));
        then(result).should(times(2)).hasNext();
        then(result).should().next();
        then(result).should().consume();
        then(recordCollector).should().supplier();
        then(recordCollector).should().accumulator();
        then(accumulator).should().accept(resultContainer, record);
        then(recordCollector).should().finisher();
        then(finisher).should().apply(resultContainer);
        then(finisherWithSummary).should().finish(keys, collectorResult, summary);
        assertEquals(expectedExecuteResult, executeResult);
    }
}
