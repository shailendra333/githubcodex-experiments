package com.demo.csvupload.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * JPA entity representing a customer record loaded from CSV.
 */
@Entity
@Table(name = "customers",
        indexes = {
                @Index(name = "idx_email",   columnList = "email"),
                @Index(name = "idx_country", columnList = "country")
        },
        uniqueConstraints = {
                // Drives the upsert logic: if externalId already exists → UPDATE, else → INSERT
                @UniqueConstraint(name = "uq_external_id", columnNames = "external_id")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "registration_date")
    private LocalDate registrationDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}

