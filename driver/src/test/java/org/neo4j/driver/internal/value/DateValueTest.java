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
package org.neo4j.driver.internal.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.exceptions.value.Uncoercible;
import org.neo4j.driver.internal.types.InternalTypeSystem;

class DateValueTest {
    @Test
    void shouldHaveCorrectType() {
        LocalDate localDate = LocalDate.now();
        DateValue dateValue = new DateValue(localDate);
        assertEquals(InternalTypeSystem.TYPE_SYSTEM.DATE(), dateValue.type());
    }

    @Test
    void shouldSupportAsObject() {
        LocalDate localDate = LocalDate.now();
        DateValue dateValue = new DateValue(localDate);
        assertEquals(localDate, dateValue.asObject());
    }

    @Test
    void shouldSupportAsLocalDate() {
        LocalDate localDate = LocalDate.now();
        DateValue dateValue = new DateValue(localDate);
        assertEquals(localDate, dateValue.asLocalDate());
    }

    @Test
    void shouldNotSupportAsLong() {
        LocalDate localDate = LocalDate.now();
        DateValue dateValue = new DateValue(localDate);

        assertThrows(Uncoercible.class, dateValue::asLong);
    }
}
