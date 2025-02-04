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
package org.neo4j.driver.stress;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.neo4j.driver.internal.util.Matchers.arithmeticError;

import java.util.List;
import java.util.concurrent.CompletionStage;
import org.neo4j.driver.AccessMode;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.async.AsyncSession;
import org.neo4j.driver.async.ResultCursor;
import org.neo4j.driver.internal.util.Futures;

public class AsyncFailingQueryWithRetries<C extends AbstractContext> extends AbstractAsyncQuery<C> {
    public AsyncFailingQueryWithRetries(Driver driver) {
        super(driver, false);
    }

    @Override
    @SuppressWarnings("deprecation")
    public CompletionStage<Void> execute(C context) {
        AsyncSession session = newSession(AccessMode.READ, context);

        CompletionStage<List<Record>> txStage = session.readTransactionAsync(
                tx -> tx.runAsync("UNWIND [10, 5, 0] AS x RETURN 10 / x").thenCompose(ResultCursor::listAsync));

        CompletionStage<Void> resultsProcessingStage = txStage.handle((records, error) -> {
            assertNull(records);
            Throwable cause = Futures.completionExceptionCause(error);
            assertThat(cause, is(arithmeticError()));

            return null;
        });

        return resultsProcessingStage.whenComplete((nothing, throwable) -> session.closeAsync());
    }
}
