package hexlet.code;
//package com.example.repository;

import java.sql.Connection;

public abstract class BaseRepository {
    private final Connection connection;

    protected BaseRepository(Connection connection) {
        this.connection = connection;
    }

    /**
     * метод определяет, добавлено ли новое значение соединения.
     *
     * @return возвращает значение соединения
     */
    protected Connection getConnection() {
        return connection;
    }
}
