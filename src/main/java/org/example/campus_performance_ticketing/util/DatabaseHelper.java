package org.example.campus_performance_ticketing.util;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Component
public class DatabaseHelper {

    private final DataSource dataSource;

    @Autowired
    public DatabaseHelper(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void init() {
        System.out.println("DatabaseHelper initialized with DataSource: " + dataSource);
    }

    /**
     * 获取数据库连接（从连接池获取）
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection(); // 自动从HikariCP连接池获取
    }

    /**
     * 测试数据库连接是否可用
     */
    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn.isValid(2); // 2秒超时
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
