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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.neo4j.driver.AccessMode;
import org.neo4j.driver.Driver;
import org.neo4j.driver.reactive.RxResult;
import org.neo4j.driver.reactive.RxSession;
import org.neo4j.driver.reactive.RxTransaction;
import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.driver.types.Node;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class RxReadQueryInTx<C extends AbstractContext> extends AbstractRxQuery<C> {
    public RxReadQueryInTx(Driver driver, boolean useBookmark) {
        super(driver, useBookmark);
    }

    @Override
    @SuppressWarnings("deprecation")
    public CompletionStage<Void> execute(C context) {
        CompletableFuture<Void> queryFinished = new CompletableFuture<>();
        RxSession session = newSession(AccessMode.READ, context);
        Flux.usingWhen(
                        session.beginTransaction(),
                        this::processAndGetSummary,
                        RxTransaction::commit,
                        (tx, error) -> tx.rollback(),
                        null)
                .subscribe(
                        summary -> {
                            context.readCompleted(summary);
                            queryFinished.complete(null);
                        },
                        error -> {
                            // ignores the error
                            queryFinished.complete(null);
                        });
        return queryFinished;
    }

    @SuppressWarnings("deprecation")
    private Publisher<ResultSummary> processAndGetSummary(RxTransaction tx) {
        RxResult result = tx.run("MATCH (n) RETURN n LIMIT 1");
        Mono<Node> records = Flux.from(result.records()).singleOrEmpty().map(record -> record.get(0)
                .asNode());
        Mono<ResultSummary> summaryMono = Mono.from(result.consume()).single();
        return records.then(summaryMono);
    }
}
