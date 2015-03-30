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
 * information: "Portions Copyright [year] [name of copyright owner]".
 *
 * Copyright 2013 ForgeRock AS. All rights reserved.
 */

package org.forgerock.json.resource;

import static org.forgerock.util.Reject.checkNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;

import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;

/**
 * An individual patch operation which is to be performed against a field within
 * a resource. This class defines four core types of operation as well as
 * allowing for extensibility by supporting user-defined operations. The core
 * operations are defined below and their behavior depends on the type of the
 * field being targeted by the operation:
 * <p>
 * <ul>
 * <li>an object (Java {@code Map}) or primitive (Java {@code String},
 * {@code Boolean}, or {@code Number}): these are considered to be
 * <i>single-valued</i> fields
 * <p>
 * <li>an array (Java {@code List}): these are considered to be
 * <i>multi-valued</i> fields exhibiting either:
 * <ul>
 * <li><i>list</i> semantics - an ordered collection of potentially non-unique
 * values, or
 * <li><i>set</i> semantics - a collection of unique values whose ordering is
 * implementation defined.
 * </ul>
 * The choice of semantic (list or set) associated with a multi-valued field is
 * implementation defined, although it is usual for it to be defined using a
 * schema.
 * </ul>
 * <p>
 * The four core patch operations are:
 * <p>
 * <ul>
 * <li>{@link #add(String, Object) add} - ensures that the targeted field
 * contains the provided value(s). Missing parent fields will be created as
 * needed. If the targeted field is already present and it is single-valued
 * (i.e. not an array) then the existing value will be replaced. If the targeted
 * field is already present and it is multi-valued (i.e. an array) then the
 * behavior depends on whether the field is a <i>list</i> or a <i>set</i>:
 * <ul>
 * <li>list - the provided array of values will be appended to the existing list
 * of values,
 * <li>set - the provided array of values will be merged with the existing set
 * of values and duplicates removed.
 * </ul>
 * Add operations which target a specific index of a multi-valued field are
 * permitted as long as the field is a <i>list</i>. In this case the patch value
 * must represent a single element of the list (i.e. it must not be an array of
 * new elements) which will be inserted at the specified position. Indexed
 * updates to <i>set</i>s are not permitted, although implementations may
 * support the special index "-" which can be used to add a single value to a
 * list or set.
 * <p>
 * <li>{@link #remove(String, Object) remove} - ensures that the targeted field
 * does not contain the provided value(s) if present. If no values are provided
 * with the remove operation then the entire field will be removed if it is
 * present. If the remove operation targets a single-valued field and a patch
 * value is provided then it must match the existing value for it to be removed,
 * otherwise the field is left unchanged. If the remove operation targets a
 * multi-valued field then the behavior depends on whether the field is a
 * <i>list</i> or a <i>set</i>:
 * <ul>
 * <li>list - the provided array of values will be removed from the existing
 * list of values. Each value in the remove operation will result in at most one
 * value being removed from the existing list. In other words, if the existing
 * list contains a pair of duplicate values and both of them need to be removed,
 * then the values must be include twice in the remove operation,
 * <li>set - the provided array of values will be removed from the existing set
 * of values.
 * </ul>
 * Remove operations which target a specific index of a multi-valued field are
 * permitted as long as the field is a <i>list</i>. If a patch value is provided
 * then it must match the existing value for it to be removed, otherwise the
 * field is left unchanged. Indexed updates to <i>set</i>s are not permitted.
 * <p>
 * <li>{@link #replace(String, Object) replace} - removes any existing value(s)
 * of the targeted field and replaces them with the provided value(s). A replace
 * operation is semantically equivalent to a {@code remove} followed by an
 * {@code add}, except that indexed updates are not permitted regardless of
 * whether or not the field is a list.
 * <p>
 * <li>{@link #increment(String, Number) increment} - increments or decrements
 * the targeted numerical field value(s) by the specified amount. If the amount
 * is negative then the value(s) are decremented. It is an error to attempt to
 * increment a field which does not contain a number or an array of numbers. It
 * is also an error if the patch value is not a single value.
 * <p>
 * </ul>
 * Additional types of patch operation are supported via the
 * {@link #operation(String, String, Object) operation} methods.
 * <p>
 * <b>NOTE:</b> this class does not define how field values will be matched, nor
 * does it define whether a resource supports indexed based modifications, nor
 * whether fields are single or multi-valued. Instead these matters are the
 * responsibility of the resource provider and, in particular, the JSON schema
 * being enforced for the targeted resource.
 */
public final class PatchOperation {

    /**
     * The name of the field which contains the target field in the JSON
     * representation.
     */
    public static final String FIELD_FIELD = "field";

    /**
     * The name of the field which contains the type of patch operation in the
     * JSON representation.
     */
    public static final String FIELD_OPERATION = "operation";

    /**
     * The name of the field which contains the operation value in the JSON
     * representation.
     */
    public static final String FIELD_VALUE = "value";

    /**
     * The identifier used for "add" operations.
     */
    public static final String OPERATION_ADD = "add";

    /**
     * The identifier used for "increment" operations.
     */
    public static final String OPERATION_INCREMENT = "increment";

    /**
     * The identifier used for "remove" operations.
     */
    public static final String OPERATION_REMOVE = "remove";

    /**
     * The identifier used for "replace" operations.
     */
    public static final String OPERATION_REPLACE = "replace";

    /**
     * Creates a new "add" patch operation which will add the provided value(s)
     * to the specified field.
     *
     * @param field
     *            The field to be added.
     * @param value
     *            The new value(s) to be added, which may be a {@link JsonValue}
     *            or a JSON object, such as a {@code String}, {@code Map}, etc.
     * @return The new patch operation.
     * @throws NullPointerException
     *             If the value is {@code null}.
     */
    public static PatchOperation add(final JsonPointer field, final Object value) {
        return operation(OPERATION_ADD, field, json(value));
    }

    /**
     * Creates a new "add" patch operation which will add the provided value(s)
     * to the specified field.
     *
     * @param field
     *            The field to be added.
     * @param value
     *            The new value(s) to be added, which may be a {@link JsonValue}
     *            or a JSON object, such as a {@code String}, {@code Map}, etc.
     * @return The new patch operation.
     * @throws NullPointerException
     *             If the value is {@code null}.
     */
    public static PatchOperation add(final String field, final Object value) {
        return add(new JsonPointer(field), value);
    }

    /**
     * Returns a deep copy of the provided patch operation. This method may be
     * used in cases where the immutability of the underlying JSON value cannot
     * be guaranteed.
     *
     * @param operation
     *            The patch operation to be defensively copied.
     * @return A deep copy of the provided patch operation.
     */
    public static PatchOperation copyOf(final PatchOperation operation) {
        return operation(operation.getOperation(), operation.getField(), operation.getValue()
                .copy());
    }

    /**
     * Creates a new "increment" patch operation which will increment the
     * value(s) of the specified field by the amount provided.
     *
     * @param field
     *            The field to be incremented.
     * @param amount
     *            The amount to be added or removed (if negative) from the
     *            field's value(s).
     * @return The new patch operation.
     * @throws NullPointerException
     *             If the amount is {@code null}.
     */
    public static PatchOperation increment(final JsonPointer field, final Number amount) {
        return operation(OPERATION_INCREMENT, field, new JsonValue(amount));
    }

    /**
     * Creates a new "increment" patch operation which will increment the
     * value(s) of the specified field by the amount provided.
     *
     * @param field
     *            The field to be incremented.
     * @param amount
     *            The amount to be added or removed (if negative) from the
     *            field's value(s).
     * @return The new patch operation.
     * @throws NullPointerException
     *             If the amount is {@code null}.
     */
    public static PatchOperation increment(final String field, final Number amount) {
        return increment(new JsonPointer(field), amount);
    }

    /**
     * Creates a new patch operation having the specified operation type, field,
     * and value(s).
     *
     * @param operation
     *            The type of patch operation to be performed.
     * @param field
     *            The field targeted by the patch operation.
     * @param value
     *            The possibly {@code null} value for the patch operation, which
     *            may be a {@link JsonValue} or a JSON object, such as a
     *            {@code String}, {@code Map}, etc.
     * @return The new patch operation.
     * @throws NullPointerException
     *             If the operation is an add or increment and the value is
     *             {@code null}.
     * @throws IllegalArgumentException
     *             If the operation is an increment an the value was not a
     *             number.
     */
    public static PatchOperation operation(final String operation, final JsonPointer field,
            final Object value) {
        return new PatchOperation(operation, field, json(value));
    }

    /**
     * Creates a new patch operation having the specified operation type, field,
     * and value(s).
     *
     * @param operation
     *            The type of patch operation to be performed.
     * @param field
     *            The field targeted by the patch operation.
     * @param value
     *            The possibly {@code null} value for the patch operation, which
     *            may be a {@link JsonValue} or a JSON object, such as a
     *            {@code String}, {@code Map}, etc.
     * @return The new patch operation.
     */
    public static PatchOperation operation(final String operation, final String field,
            final Object value) {
        return operation(operation, new JsonPointer(field), value);
    }

    /**
     * Creates a new "remove" patch operation which will remove the specified
     * field.
     *
     * @param field
     *            The field to be removed.
     * @return The new patch operation.
     */
    public static PatchOperation remove(final JsonPointer field) {
        return remove(field, null);
    }

    /**
     * Creates a new "remove" patch operation which will remove the provided
     * value(s) from the specified field.
     *
     * @param field
     *            The field to be removed.
     * @param value
     *            The value(s) to be removed, which may be a {@link JsonValue}
     *            or a JSON object, such as a {@code String}, {@code Map}, etc.
     * @return The new patch operation.
     */
    public static PatchOperation remove(final JsonPointer field, final Object value) {
        return operation(OPERATION_REMOVE, field, json(value));
    }

    /**
     * Creates a new "remove" patch operation which will remove the specified
     * field.
     *
     * @param field
     *            The field to be removed.
     * @return The new patch operation.
     */
    public static PatchOperation remove(final String field) {
        return remove(new JsonPointer(field));
    }

    /**
     * Creates a new "remove" patch operation which will remove the provided
     * value(s) from the specified field.
     *
     * @param field
     *            The field to be removed.
     * @param value
     *            The value(s) to be removed, which may be a {@link JsonValue}
     *            or a JSON object, such as a {@code String}, {@code Map}, etc.
     * @return The new patch operation.
     */
    public static PatchOperation remove(final String field, final Object value) {
        return remove(new JsonPointer(field), value);
    }

    /**
     * Creates a new "replace" patch operation which will replace the value(s)
     * of the specified field with the provided value(s).
     *
     * @param field
     *            The field to be replaced.
     * @param value
     *            The new value(s) for the field, which may be a
     *            {@link JsonValue} or a JSON object, such as a {@code String},
     *            {@code Map}, etc.
     * @return The new patch operation.
     */
    public static PatchOperation replace(final JsonPointer field, final Object value) {
        return operation(OPERATION_REPLACE, field, json(value));
    }

    /**
     * Creates a new "replace" patch operation which will replace the value(s)
     * of the specified field with the provided value(s).
     *
     * @param field
     *            The field to be replaced.
     * @param value
     *            The new value(s) for the field, which may be a
     *            {@link JsonValue} or a JSON object, such as a {@code String},
     *            {@code Map}, etc.
     * @return The new patch operation.
     */
    public static PatchOperation replace(final String field, final Object value) {
        return replace(new JsonPointer(field), value);
    }

    /**
     * Parses the provided JSON content as a patch operation.
     *
     * @param json
     *            The patch operation to be parsed.
     * @return The parsed patch operation.
     * @throws BadRequestException
     *             If the JSON value is not a JSON patch operation.
     */
    public static PatchOperation valueOf(final JsonValue json) throws BadRequestException {
        if (!json.isMap()) {
            throw new BadRequestException(
                        "The request could not be processed because the provided "
                                + "content is not a valid JSON patch");
        }
        try {
            final String       type = json.get(FIELD_OPERATION).required().asString();
            final JsonPointer field = json.get(FIELD_FIELD).required().asPointer();
            final JsonValue   value = json.get(FIELD_VALUE);
            return operation(type, field, value);
        } catch (final Exception e) {
            throw new BadRequestException(
                    "The request could not be processed because the provided "
                            + "content is not a valid JSON patch: " + e.getMessage(), e);
        }
    }

    /**
     * Parses the provided JSON content as a list of patch operations.
     *
     * @param json
     *            The list of patch operations to be parsed.
     * @return The list of parsed patch operations.
     * @throws BadRequestException
     *             If the JSON value is not a list of JSON patch operations.
     */
    public static List<PatchOperation> valueOfList(final JsonValue json) throws BadRequestException {
        if (!json.isList()) {
            throw new BadRequestException(
                    "The request could not be processed because the provided "
                            + "content is not a JSON array of patch operations");
        }
        final List<PatchOperation> patch = new ArrayList<PatchOperation>(json.size());
        for (final JsonValue operation : json) {
            patch.add(valueOf(operation));
        }
        return patch;
    }

    private static JsonValue json(final Object value) {
        /*
         * It doesn't matter if value is null because it will be wrapped anyway
         * by the constructor.
         */
        return (value instanceof JsonValue) ? (JsonValue) value : new JsonValue(value);
    }

    private final JsonPointer field;
    private final String operation;
    private final JsonValue value;

    private PatchOperation(final String operation, final JsonPointer field, final JsonValue value) {
        this.operation = checkNotNull(operation, "Cannot instantiate PatchOperation with null operation value");
        this.field = checkNotNull(field, "Cannot instantiate PatchOperation with null field value");
        this.value = value;
        checkOperationType();
        checkOperationValue();
    }

    /**
     * Returns the field targeted by the patch operation.
     *
     * @return The field targeted by the patch operation.
     */
    public JsonPointer getField() {
        return field;
    }

    /**
     * Returns the type of patch operation to be performed.
     *
     * @return The type of patch operation to be performed.
     */
    public String getOperation() {
        return operation;
    }

    /**
     * Returns the value for the patch operation. The return value may be
     * {@code null}, and may also be a JSON value whose value is {@code null}.
     *
     * @return The nullable value for the patch operation.
     */
    public JsonValue getValue() {
        return value;
    }

    /**
     * Returns {@code true} if this is an "add" patch operation.
     *
     * @return {@code true} if this is an "add" patch operation.
     */
    public boolean isAdd() {
        return is(OPERATION_ADD);
    }

    /**
     * Returns {@code true} if this is an "increment" patch operation.
     *
     * @return {@code true} if this is an "increment" patch operation.
     */
    public boolean isIncrement() {
        return is(OPERATION_INCREMENT);
    }

    /**
     * Returns {@code true} if this is an "remove" patch operation.
     *
     * @return {@code true} if this is an "remove" patch operation.
     */
    public boolean isRemove() {
        return is(OPERATION_REMOVE);
    }

    /**
     * Returns {@code true} if this is an "replace" patch operation.
     *
     * @return {@code true} if this is an "replace" patch operation.
     */
    public boolean isReplace() {
        return is(OPERATION_REPLACE);
    }

    /**
     * Returns a JSON value representation of this patch operation.
     *
     * @return A JSON value representation of this patch operation.
     */
    public JsonValue toJsonValue() {
        final JsonValue json = new JsonValue(new LinkedHashMap<String, Object>(3));
        json.put(FIELD_OPERATION, operation);
        json.put(FIELD_FIELD, field.toString());
        if (value != null) {
            json.put(FIELD_VALUE, value.getObject());
        }
        return json;
    }

    @Override
    public String toString() {
        return toJsonValue().toString();
    }

    private void checkOperationValue() {
        if (isAdd() && value == null) {
            throw new NullPointerException("No value provided for add patch operation");
        } else if (isIncrement()) {
            if (value == null || value.isNull()) {
                throw new NullPointerException("No value provided for increment patch operation");
            } else if (!value.isNumber()) {
                throw new IllegalArgumentException(
                        "Non-numeric value provided for increment patch operation");
            }
        }
    }

    private void checkOperationType() {
        if (!isAdd() && !isRemove() && !isIncrement() && !isReplace()) {
            throw new IllegalArgumentException("Invalid patch operation type " + operation);
        }
    }

    private boolean is(final String type) {
        return operation == type || operation.equalsIgnoreCase(type);
    }
}
