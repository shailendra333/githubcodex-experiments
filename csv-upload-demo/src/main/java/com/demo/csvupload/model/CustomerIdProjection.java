package com.demo.csvupload.model;

/**
 * Lightweight JPA projection for the upsert lookup SELECT.
 *
 * <p>Instead of loading full {@link Customer} entities (all 8 columns) just to
 * get the database primary key, this projection fetches only the two fields
 * needed — cutting heap allocation for the lookup by ~70%.
 *
 * <p>Spring Data JPA materialises interface projections as JDK proxy objects;
 * each proxy is far smaller than a full entity with all its field values.
 */
public interface CustomerIdProjection {
    String getExternalId();
    Long   getId();
}

