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

package org.forgerock.json.resource;

import java.util.LinkedHashMap;

import org.forgerock.json.JsonValue;

/**
 * A utility class containing various factory methods for creating and
 * manipulating responses.
 */
public final class Responses {

    /**
     * Returns a new {@code JsonValue} response with the provided JSON content.
     *
     * @param json The JSON content.
     * @return The new {@code JsonValue} response.
     */
    public static ActionResponse newActionResponse(JsonValue json) {
        return new ActionResponseImpl(json);
    }

    /**
     * Returns a new {@code Resource} response with the provided Resource as
     * content.
     *
     * @param id The resource ID if applicable otherwise {@code null}.
     * @param revision The resource version, if known.
     * @param content The resource content.
     * @return The new {@code Resource} response.
     */
    public static ResourceResponse newResourceResponse(String id, String revision, JsonValue content) {
        return new ResourceResponseImpl(id, revision, content);
    }

    /**
     * Creates a new query result with a {@code null} paged results cookie and
     * no count of the total number of remaining results.
     */
    public static QueryResponse newQueryResponse() {
        return newQueryResponse(null);
    }

    /**
     * Creates a new query result with the provided paged results cookie and
     * no count.
     *
     * @param pagedResultsCookie
     *            The opaque cookie which should be used with the next paged
     *            results query request, or {@code null} if paged results were
     *            not requested, or if there are not more pages to be returned.
     */
    public static QueryResponse newQueryResponse(String pagedResultsCookie) {
        return newQueryResponse(pagedResultsCookie, CountPolicy.NONE, QueryResponse.NO_COUNT);
    }

    /**
     * Creates a new query result with the provided paged results cookie and
     * a count of the total number of remaining results according to
     * {@literal totalPagedResultsPolicy}.
     *
     * @param pagedResultsCookie
     *            The opaque cookie which should be used with the next paged
     *            results query request, or {@code null} if paged results were
     *            not requested, or if there are not more pages to be returned.
     * @param totalPagedResultsPolicy
     *            The policy that was used to calculate {@literal totalPagedResults}
     * @param totalPagedResults
     *            The total number of paged results requested in adherence to
     *            the {@link QueryRequest#getTotalPagedResultsPolicy()} in the request,
     *            or {@link QueryResponse#NO_COUNT} if paged results were not requested,
     *            the count policy is {@code NONE}, or if the total number of remaining
     *            results is unknown.
     */
    public static QueryResponse newQueryResponse(String pagedResultsCookie, CountPolicy totalPagedResultsPolicy,
            int totalPagedResults) {
        return new QueryResponseImpl(pagedResultsCookie, totalPagedResultsPolicy, totalPagedResults);
    }

    private static abstract class AbstractResponseImpl implements Response {

    }

    private static final class ActionResponseImpl extends AbstractResponseImpl implements ActionResponse {

        private final JsonValue content;

        private ActionResponseImpl(JsonValue content) {
            this.content = content;
        }

        @Override
        public JsonValue getJsonContent() {
            return content;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ActionResponse that = (ActionResponse) o;

            return getJsonContent().getObject().equals(that.getJsonContent().getObject());
        }

        @Override
        public int hashCode() {
            return getJsonContent().getObject().hashCode();
        }
    }

    private static final class ResourceResponseImpl extends AbstractResponseImpl implements ResourceResponse {

        private final JsonValue content;
        private final String id;
        private final String revision;

        private ResourceResponseImpl(String id, String revision, JsonValue content) {
            this.id = id;
            this.revision = revision;
            this.content = content;
        }

        @Override
        public JsonValue getContent() {
            return content;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getRevision() {
            return revision;
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == this) {
                return true;
            } else if (obj instanceof ResourceResponseImpl) {
                final ResourceResponseImpl that = (ResourceResponseImpl) obj;
                return isEqual(id, that.id) && isEqual(revision, that.revision);
            } else {
                return false;
            }
        }

        private boolean isEqual(final String s1, final String s2) {
            if (s1 == s2) {
                return true;
            } else if (s1 == null || s2 == null) {
                return false;
            } else {
                return s1.equals(s2);
            }
        }

        @Override
        public int hashCode() {
            final int hash = id != null ? id.hashCode() : 17;
            return (hash * 31) + (revision != null ? revision.hashCode() : 0);
        }

        @Override
        public String toString() {
            final JsonValue wrapper = new JsonValue(new LinkedHashMap<>(3));
            wrapper.add("id", id);
            wrapper.add("rev", revision);
            wrapper.add("content", content);
            return wrapper.toString();
        }
    }

    private static final class QueryResponseImpl extends AbstractResponseImpl implements QueryResponse {

        private final String pagedResultsCookie;
        private final CountPolicy totalPagedResultsPolicy;
        private final int totalPagedResults;

        /**
         * Creates a new query result with the provided paged results cookie and
         * a count of the total number of remaining results according to
         * {@link #totalPagedResultsPolicy}.
         *
         * @param pagedResultsCookie
         *            The opaque cookie which should be used with the next paged
         *            results query request, or {@code null} if paged results were
         *            not requested, or if there are not more pages to be returned.
         * @param totalPagedResultsPolicy
         *            The policy that was used to calculate {@link #totalPagedResults}
         * @param totalPagedResults
         *            The total number of paged results requested in adherence to
         *            the {@link QueryRequest#getTotalPagedResultsPolicy()} in the request,
         *            or {@link #NO_COUNT} if paged results were not requested, the count
         *            policy is {@code NONE}, or if the total number of remaining
         *            results is unknown.
         */
        private QueryResponseImpl(String pagedResultsCookie, CountPolicy totalPagedResultsPolicy,
                int totalPagedResults) {
            this.pagedResultsCookie = pagedResultsCookie;
            this.totalPagedResultsPolicy = totalPagedResultsPolicy;
            this.totalPagedResults = totalPagedResults;
        }

        /**
         * Returns the policy that was used to calculate the {@link #totalPagedResults}.
         *
         * @see #getTotalPagedResults()
         */
        @Override
        public CountPolicy getTotalPagedResultsPolicy() { return totalPagedResultsPolicy; }

        /**
         * Returns the opaque cookie which should be used with the next paged
         * results query request.
         *
         * @return The opaque cookie which should be used with the next paged
         *         results query request, or {@code null} if paged results were not
         *         requested, or if there are not more pages to be returned.
         */
        @Override
        public String getPagedResultsCookie() {
            return pagedResultsCookie;
        }

        /**
         * Returns the total number of paged results in adherence with
         * the {@link QueryRequest#getTotalPagedResultsPolicy()} in the request
         * or {@link #NO_COUNT} if paged results were not requested, the count
         * policy is {@code NONE}, or the total number of remaining
         * results is unknown.
         *
         * @return A count of the total number of paged results to be
         *         returned in subsequent paged results query requests, or
         *         {@link #NO_COUNT} if paged results were not requested, or if the total
         *         number of remaining results is unknown.
         */
        @Override
        public int getTotalPagedResults() {
            return totalPagedResults;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            QueryResponseImpl that = (QueryResponseImpl) o;

            if (totalPagedResults != that.totalPagedResults) return false;
            if (!pagedResultsCookie.equals(that.pagedResultsCookie)) return false;
            return totalPagedResultsPolicy == that.totalPagedResultsPolicy;
        }

        @Override
        public int hashCode() {
            int result = pagedResultsCookie.hashCode();
            result = 31 * result + totalPagedResultsPolicy.hashCode();
            result = 31 * result + totalPagedResults;
            return result;
        }
    }
}