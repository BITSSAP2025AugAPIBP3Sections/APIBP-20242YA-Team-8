package com.rip.vaultify.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Entity
@Table(name = "permissions")
@Getter
@Setter
public class Permission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(optional = false)
    private File file;

    @ManyToOne(optional = false)
    private User user;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Access access;

    @Column(nullable = false)
    private Boolean viewed = false;

    public enum Access {
        READ, WRITE, OWNER
    }
}