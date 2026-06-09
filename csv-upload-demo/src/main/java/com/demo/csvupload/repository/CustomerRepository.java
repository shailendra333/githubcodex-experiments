package com.demo.csvupload.repository;

import com.demo.csvupload.model.Customer;
import com.demo.csvupload.model.CustomerIdProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    @Query("SELECT COUNT(c) FROM Customer c")
    long countAllCustomers();

    /**
     * TECHNIQUE 2 — Projection-based upsert lookup.
     *
     * <p>Returns only {@code (externalId, id)} pairs instead of full Customer
     * entities.  A Spring Data projection proxy is a tiny JDK dynamic proxy
     * (~200 bytes) versus a full Customer entity (~400–600 bytes with field
     * values + Hibernate internal state).  For a batch of 500 this saves
     * roughly 100–200 KB of heap per batch, which compounds to gigabytes
     * saved across 10 M rows.
     */
    @Query("SELECT c.externalId AS externalId, c.id AS id FROM Customer c WHERE c.externalId IN :externalIds")
    List<CustomerIdProjection> findIdsByExternalIdIn(Collection<String> externalIds);
}
