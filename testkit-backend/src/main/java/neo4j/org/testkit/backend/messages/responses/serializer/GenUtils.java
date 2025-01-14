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
package neo4j.org.testkit.backend.messages.responses.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import neo4j.org.testkit.backend.messages.requests.deserializer.types.CypherDateTime;
import neo4j.org.testkit.backend.messages.requests.deserializer.types.CypherTime;
import org.neo4j.driver.types.IsoDuration;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class GenUtils {
    public static void object(JsonGenerator gen, RunnableWithIOException runnable) throws IOException {
        gen.writeStartObject();
        runnable.run();
        gen.writeEndObject();
    }

    public static <T> void list(JsonGenerator gen, List<T> list) throws IOException {
        gen.writeStartArray();
        for (T element : list) {
            gen.writeObject(element);
        }
        gen.writeEndArray();
    }

    public static <T> void cypherObject(JsonGenerator gen, String name, T value) throws IOException {
        cypherObject(gen, name, () -> gen.writeObjectField("value", value));
    }

    public static <T> void cypherObject(JsonGenerator gen, String name, RunnableWithIOException runnable)
            throws IOException {
        object(gen, () -> {
            gen.writeStringField("name", name);
            gen.writeFieldName("data");
            object(gen, runnable);
        });
    }

    public static void writeDate(JsonGenerator gen, int year, int month, int day) throws IOException {
        gen.writeFieldName("year");
        gen.writeNumber(year);
        gen.writeFieldName("month");
        gen.writeNumber(month);
        gen.writeFieldName("day");
        gen.writeNumber(day);
    }

    public static void writeTime(JsonGenerator gen, int hour, int minute, int second, int nano) throws IOException {
        gen.writeFieldName("hour");
        gen.writeNumber(hour);
        gen.writeFieldName("minute");
        gen.writeNumber(minute);
        gen.writeFieldName("second");
        gen.writeNumber(second);
        gen.writeFieldName("nanosecond");
        gen.writeNumber(nano);
    }

    public static Class<?> cypherTypeToJavaType(String typeString) {
        switch (typeString) {
            case "CypherBool":
                return Boolean.class;
            case "CypherInt":
                return Long.class;
            case "CypherFloat":
                return Double.class;
            case "CypherString":
                return String.class;
            case "CypherList":
                return List.class;
            case "CypherMap":
                return Map.class;
            case "CypherDateTime":
                return CypherDateTime.class;
            case "CypherTime":
                return CypherTime.class;
            case "CypherDate":
                return LocalDate.class;
            case "CypherDuration":
                return IsoDuration.class;
            case "CypherNull":
                return null;
            default:
                return null;
        }
    }

    interface RunnableWithIOException {
        void run() throws IOException;
    }
}
