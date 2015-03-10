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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.json.test.fest;

import org.fest.assertions.Assertions;
import org.fest.assertions.BooleanAssert;
import org.fest.assertions.Condition;
import org.fest.assertions.DoubleAssert;
import org.fest.assertions.GenericAssert;
import org.fest.assertions.IntAssert;
import org.fest.assertions.ListAssert;
import org.fest.assertions.LongAssert;
import org.fest.assertions.MapAssert;
import org.fest.assertions.StringAssert;
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.util.promise.Function;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.test.assertj.AbstractAssertJPromiseAssert;
import org.forgerock.util.test.fest.AbstractFestPromiseAssert;

/**
 * Provides fest assertions for {@link JsonValue} instances.
 */
public abstract class FestJsonValueAssert {

    /**
     * Creates the relevant {@code AbstractJsonValueAssert} instance for the provided {@link JsonValue}.
     * @param value The actual value.
     * @return Either a {@code ArrayJsonValueAssert} if the JsonValue is underpinned by a {@link java.util.Set}
     * or {@link java.util.List}, or a {@code ObjectJsonValueAssert} if underpinned by a {@link java.util.Map}.
     * @throws java.lang.IllegalArgumentException If the JsonValue is not a JSON array or object.
     */
    public static AbstractJsonValueAssert assertThat(JsonValue value) {
        if (value.isList() || value.isSet()) {
            return new ArrayJsonValueAssert(value);
        } else if (value.isMap()) {
            return new ObjectJsonValueAssert(value);
        } else {
            throw new IllegalArgumentException("Should be a root JsonValue - either an Object or an Array");
        }
    }

    /**
     * Creates a promise assert class for {@link JsonValue} instances.
     * <p>
     * On calling the succeeded method, the {@link AbstractJsonValueAssert#isObject()} and
     * {@link AbstractJsonValueAssert#isArray()} must be used to access array/object specific assert methods.
     * @param promise The {@link JsonValue} promise.
     * @return The assertion object.
     */
    public static FestJsonValuePromiseAssert assertThat(Promise<JsonValue, ?> promise) {
        return new FestJsonValuePromiseAssert(promise);
    }

    /**
     * An assertion class for promises that return {@code JsonValue}s.
     */
    public static class FestJsonValuePromiseAssert
            extends AbstractFestPromiseAssert<JsonValue, FestJsonValuePromiseAssert, PromisedJsonValueAssert> {

        private FestJsonValuePromiseAssert(Promise<JsonValue, ?> promise) {
            super(promise, FestJsonValuePromiseAssert.class,
                    new Function<JsonValue, PromisedJsonValueAssert, RuntimeException>() {
                        @Override
                        public PromisedJsonValueAssert apply(JsonValue jsonValue) throws RuntimeException {
                            return new PromisedJsonValueAssert(jsonValue);
                        }
                    });
        }
    }

    public static abstract class AbstractJsonValueAssert<T> extends GenericAssert<T, JsonValue> {

        private AbstractJsonValueAssert(Class<T> type, JsonValue value) {
            super(type, value);
        }

        /**
         * Check that the {@link JsonValue} is an object.
         * @return The {@code ObjectJsonValueAssert} representation of this Assert instance.
         */
        public ObjectJsonValueAssert isObject() {
            Assertions.assertThat(actual.isMap()).isTrue();
            return new ObjectJsonValueAssert(actual);
        }

        /**
         * Check that the {@link JsonValue} is an array.
         * @return The {@code ArrayJsonValueAssert} representation of this Assert instance.
         */
        public ArrayJsonValueAssert isArray() {
            Assertions.assertThat(actual.isSet() || actual.isList()).isTrue();
            return new ArrayJsonValueAssert(actual);
        }

        /**
         * Check that the referenced {@link JsonValue} is an object.
         * @param path The {@link JsonPointer} path to the expected value.
         * @return The {@code ObjectJsonValueAssert} for that node.
         */
        public ObjectJsonValueAssert hasObject(String path) {
            JsonValue child = actual.get(new JsonPointer(path));
            Assertions.assertThat(child.isMap()).isTrue();
            return new ObjectJsonValueAssert(child);
        }

        /**
         * Check that the referenced {@link JsonValue} is an array.
         * @param path The {@link JsonPointer} path to the expected value.
         * @return The {@code ArrayJsonValueAssert} for that node.
         */
        public ArrayJsonValueAssert hasArray(String path) {
            JsonValue child = actual.get(new JsonPointer(path));
            Assertions.assertThat(child.isList() || child.isSet()).isTrue();
            return new ArrayJsonValueAssert(child);
        }

        /**
         * Check that the referenced {@link JsonValue} is null.
         * @param path The {@link JsonPointer} path to the expected null.
         * @return This assert object, for further processing.
         */
        public T hasNull(String path) {
            JsonValue child = actual.get(new JsonPointer(path));
            Assertions.assertThat(child.isNull()).isTrue();
            return myself;
        }

        /**
         * Check that the referenced {@link JsonValue} is a boolean.
         * @param path The {@link JsonPointer} path to the expected value.
         * @param condition What condition you expect the value to match.
         * @return This assert object, for further processing.
         */
        public T booleanIs(String path, Condition<Boolean> condition) {
            JsonValue child = actual.get(new JsonPointer(path));
            Assertions.assertThat(child.isBoolean()).isTrue();
            Assertions.assertThat(child.asBoolean()).is(condition);
            return myself;
        }

        /**
         * Check that the referenced {@link JsonValue} is a boolean, irrespective of its value.
         * @param path The {@link JsonPointer} path to the expected value.
         * @return This assert object, for further processing.
         */
        public T hasBoolean(String path) {
            JsonValue child = actual.get(new JsonPointer(path));
            Assertions.assertThat(child.isBoolean()).isTrue();
            return myself;
        }

        /**
         * Get a {@link BooleanAssert} for the referenced {@link JsonValue} is a boolean, to check its value.
         * @param path The {@link JsonPointer} path to the expected value.
         * @return This {@link BooleanAssert} instance.
         */
        public BooleanAssert booleanAt(String path) {
            JsonValue child = actual.get(new JsonPointer(path));
            Assertions.assertThat(child.isBoolean()).isTrue();
            return Assertions.assertThat(child.asBoolean());
        }

        /**
         * Check that the referenced {@link JsonValue} is a string, irrespective of its value.
         * @param path The {@link JsonPointer} path to the expected value.
         * @return This assert object, for further processing.
         */
        public T hasString(String path) {
            JsonValue child = actual.get(new JsonPointer(path));
            Assertions.assertThat(child.isString()).isTrue();
            return myself;
        }

        /**
         * Check the value of the referenced {@link JsonValue} string.
         * @param path The {@link JsonPointer} path to the expected value.
         * @param condition What condition you expect the value to match.
         * @return This assert object, for further processing.
         */
        public T stringIs(String path, Condition<String> condition) {
            JsonValue child = actual.get(new JsonPointer(path));
            Assertions.assertThat(child.isString()).isTrue();
            Assertions.assertThat(child.asString()).is(condition);
            return myself;
        }

        /**
         * Get a {@link StringAssert} for the referenced {@link JsonValue} is a string, to check its value.
         * @param path The {@link JsonPointer} path to the expected value.
         * @return This {@link StringAssert} instance.
         */
        public StringAssert stringAt(String path) {
            JsonValue child = actual.get(new JsonPointer(path));
            Assertions.assertThat(child.isString()).isTrue();
            return Assertions.assertThat(child.asString());
        }

        /**
         * Check that the referenced {@link JsonValue} is a number, irrespective of its value.
         * @param path The {@link JsonPointer} path to the expected value.
         * @return This assert object, for further processing.
         */
        public T hasNumber(String path) {
            JsonValue child = actual.get(new JsonPointer(path));
            Assertions.assertThat(child.isNumber()).isTrue();
            return myself;
        }

        /**
         * Check the integer value of the referenced {@link JsonValue}.
         * @param path The {@link JsonPointer} path to the expected value.
         * @param condition What condition you expect the value to match.
         * @return This assert object, for further processing.
         */
        public T integerIs(String path, Condition<Integer> condition) {
            JsonValue child = actual.get(new JsonPointer(path));
            Assertions.assertThat(child.isNumber()).isTrue();
            Assertions.assertThat(child.asInteger()).is(condition);
            return myself;
        }

        /**
         * Get a {@link IntAssert} for the referenced {@link JsonValue} is an integer, to check its value.
         * @param path The {@link JsonPointer} path to the expected value.
         * @return This {@link IntAssert} instance.
         */
        public IntAssert integerAt(String path) {
            JsonValue child = actual.get(new JsonPointer(path));
            Assertions.assertThat(child.isNumber()).isTrue();
            return Assertions.assertThat(child.asInteger());
        }

        /**
         * Check the long value of the referenced {@link JsonValue}.
         * @param path The {@link JsonPointer} path to the expected value.
         * @param condition What condition you expect the value to match.
         * @return This assert object, for further processing.
         */
        public T longIs(String path, Condition<Long> condition) {
            JsonValue child = actual.get(new JsonPointer(path));
            Assertions.assertThat(child.isNumber()).isTrue();
            Assertions.assertThat(child.asLong()).is(condition);
            return myself;
        }

        /**
         * Get a {@link LongAssert} for the referenced {@link JsonValue} is a long, to check its value.
         * @param path The {@link JsonPointer} path to the expected value.
         * @return This {@link LongAssert} instance.
         */
        public LongAssert longAt(String path) {
            JsonValue child = actual.get(new JsonPointer(path));
            Assertions.assertThat(child.isNumber()).isTrue();
            return Assertions.assertThat(child.asLong());
        }

        /**
         * Check the double value of the referenced {@link JsonValue}.
         * @param path The {@link JsonPointer} path to the expected value.
         * @param condition What condition you expect the value to match.
         * @return This assert object, for further processing.
         */
        public T doubleIs(String path, Condition<Double> condition) {
            JsonValue child = actual.get(new JsonPointer(path));
            Assertions.assertThat(child.isNumber()).isTrue();
            Assertions.assertThat(child.asDouble()).is(condition);
            return myself;
        }

        /**
         * Get a {@link DoubleAssert} for the referenced {@link JsonValue} is a double, to check its value.
         * @param path The {@link JsonPointer} path to the expected value.
         * @return This {@link DoubleAssert} instance.
         */
        public DoubleAssert doubleAt(String path) {
            JsonValue child = actual.get(new JsonPointer(path));
            Assertions.assertThat(child.isNumber()).isTrue();
            return Assertions.assertThat(child.asDouble());
        }

    }

    public static class PromisedJsonValueAssert extends AbstractJsonValueAssert<PromisedJsonValueAssert> {
        public PromisedJsonValueAssert(JsonValue value) {
            super(PromisedJsonValueAssert.class, value);
        }
    }

    public static class ObjectJsonValueAssert extends AbstractJsonValueAssert<ObjectJsonValueAssert> {

        private MapAssert mapAssert;

        private ObjectJsonValueAssert(JsonValue value) {
            super(ObjectJsonValueAssert.class, value);
            this.mapAssert = Assertions.assertThat(value.asMap());
        }

        /**
         * Check that this object contains a property with the given name, and value.
         * @param key The name of the object property.
         * @param value The expected value.
         * @return This assert instance for further processing (if required).
         * @see MapAssert#includes
         */
        public ObjectJsonValueAssert contains(String key, Object value) {
            mapAssert.includes(MapAssert.entry(key, value));
            return this;
        }

        /**
         * Check that this object contains the specified properties.
         * @param entries The expected values.
         * @return This assert instance for further processing (if required).
         * @see MapAssert#includes
         */
        public ObjectJsonValueAssert contains(MapAssert.Entry... entries) {
            mapAssert.includes(entries);
            return this;
        }

        /**
         * Check that this object does not contain a property with the given name, and value.
         * @param key The name of the object property.
         * @param value The expected value it should not equal if it exists.
         * @return This assert instance for further processing (if required).
         * @see MapAssert#excludes
         */
        public ObjectJsonValueAssert doesNotContain(String key, Object value) {
            mapAssert.excludes(MapAssert.entry(key, value));
            return this;
        }

        /**
         * Check that this object does not contain a property with the given name, and value.
         * @param entries The expected entries that should not exist.
         * @return This assert instance for further processing (if required).
         * @see MapAssert#excludes
         */
        public ObjectJsonValueAssert doesNotContain(MapAssert.Entry... entries) {
            mapAssert.excludes(entries);
            return this;
        }

    }

    public static class ArrayJsonValueAssert extends AbstractJsonValueAssert<ArrayJsonValueAssert> {
        private ListAssert listAssert;

        private ArrayJsonValueAssert(JsonValue value) {
            super(ArrayJsonValueAssert.class, value);
            this.listAssert = Assertions.assertThat(value.asList());
        }

        /**
         * Check that this array contains the given values.
         * @param values The expected values.
         * @return This assert instance for further processing (if required).
         * @see ListAssert#contains
         */
        public ArrayJsonValueAssert contains(Object... values) {
            listAssert.contains(values);
            return this;
        }

        /**
         * Check that this array contains exactly the given values.
         * @param values The expected values.
         * @return This assert instance for further processing (if required).
         * @see ListAssert#containsExactly
         */
        public ArrayJsonValueAssert containsExactly(Object... values) {
            listAssert.containsExactly(values);
            return this;
        }

        /**
         * Check that this array contains the given values as a sequence.
         * @param values The expected values.
         * @return This assert instance for further processing (if required).
         * @see ListAssert#containsSequence
         */
        public ArrayJsonValueAssert containsSequence(Object... values) {
            listAssert.containsSequence(values);
            return this;
        }

        /**
         * Check that this array contains only the given values.
         * @param values The expected values.
         * @return This assert instance for further processing (if required).
         * @see ListAssert#containsOnly
         */
        public ArrayJsonValueAssert containsOnly(Object... values) {
            listAssert.containsOnly(values);
            return this;
        }

        /**
         * Check that this array does not contain the given values.
         * @param values The values expected to not be contained.
         * @return This assert instance for further processing (if required).
         * @see ListAssert#excludes
         */
        public ArrayJsonValueAssert doesNotContain(Object... values) {
            listAssert.excludes(values);
            return this;
        }

        /**
         * Check that this array starts with the given values.
         * @param values The expected values.
         * @return This assert instance for further processing (if required).
         * @see ListAssert#startsWith
         */
        public ArrayJsonValueAssert startsWith(Object... values) {
            listAssert.startsWith(values);
            return this;
        }

        /**
         * Check that this array ends with the given values.
         * @param values The expected values.
         * @return This assert instance for further processing (if required).
         * @see ListAssert#endsWith
         */
        public ArrayJsonValueAssert endsWith(Object... values) {
            listAssert.endsWith(values);
            return this;
        }

        /**
         * Check that this array does not contain duplicates.
         * @return This assert instance for further processing (if required).
         * @see ListAssert#doesNotHaveDuplicates
         */
        public ArrayJsonValueAssert doesNotHaveDuplicates() {
            listAssert.doesNotHaveDuplicates();
            return this;
        }

        /**
         * Check that this array contains the given size.
         * @param size The expected size.
         * @return This assert instance for further processing (if required).
         * @see ListAssert#hasSize
         */
        public ArrayJsonValueAssert hasSize(int size) {
            listAssert.hasSize(size);
            return this;
        }

    }
}
