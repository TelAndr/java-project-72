package hexlet.code;
//package com.example.repository;

import java.sql.Connection;

public abstract class BaseRepository {
    protected final Connection connection;

    protected BaseRepository(Connection connection) {
        this.connection = connection;
    }
}
