package rubidium.database;

import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

public class Database {
    
    private static final Logger logger = Logger.getLogger("Rubidium-Database");
    
    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final ExecutorService executor;
    private final Queue<Connection> connectionPool;
    private final int maxConnections;
    
    public Database(String jdbcUrl, String username, String password) {
        this(jdbcUrl, username, password, 10);
    }
    
    public Database(String jdbcUrl, String username, String password, int maxConnections) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.maxConnections = maxConnections;
        this.executor = Executors.newFixedThreadPool(4);
        this.connectionPool = new ConcurrentLinkedQueue<>();
    }
    
    public Connection getConnection() throws SQLException {
        Connection conn = connectionPool.poll();
        if (conn != null && !conn.isClosed()) {
            return conn;
        }
        return createConnection();
    }
    
    private Connection createConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }
    
    public void releaseConnection(Connection conn) {
        if (conn != null) {
            if (connectionPool.size() < maxConnections) {
                connectionPool.offer(conn);
            } else {
                try {
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
    }
    
    public CompletableFuture<List<Map<String, Object>>> queryAsync(String sql, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return query(sql, params);
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        }, executor);
    }
    
    public List<Map<String, Object>> query(String sql, Object... params) throws SQLException {
        Connection conn = getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            setParameters(stmt, params);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return resultSetToList(rs);
            }
        } finally {
            releaseConnection(conn);
        }
    }
    
    public CompletableFuture<Optional<Map<String, Object>>> queryOneAsync(String sql, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return queryOne(sql, params);
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        }, executor);
    }
    
    public Optional<Map<String, Object>> queryOne(String sql, Object... params) throws SQLException {
        List<Map<String, Object>> results = query(sql, params);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
    
    public CompletableFuture<Integer> updateAsync(String sql, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return update(sql, params);
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        }, executor);
    }
    
    public int update(String sql, Object... params) throws SQLException {
        Connection conn = getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            setParameters(stmt, params);
            return stmt.executeUpdate();
        } finally {
            releaseConnection(conn);
        }
    }
    
    public CompletableFuture<Long> insertAsync(String sql, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return insert(sql, params);
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        }, executor);
    }
    
    public long insert(String sql, Object... params) throws SQLException {
        Connection conn = getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setParameters(stmt, params);
            stmt.executeUpdate();
            
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
            return -1;
        } finally {
            releaseConnection(conn);
        }
    }
    
    public void execute(String sql) throws SQLException {
        Connection conn = getConnection();
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } finally {
            releaseConnection(conn);
        }
    }
    
    public void transaction(TransactionCallback callback) throws SQLException {
        Connection conn = getConnection();
        boolean autoCommit = conn.getAutoCommit();
        
        try {
            conn.setAutoCommit(false);
            callback.execute(conn);
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw new SQLException("Transaction failed: " + e.getMessage(), e);
        } finally {
            conn.setAutoCommit(autoCommit);
            releaseConnection(conn);
        }
    }
    
    private void setParameters(PreparedStatement stmt, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object param = params[i];
            if (param == null) {
                stmt.setNull(i + 1, Types.NULL);
            } else if (param instanceof String s) {
                stmt.setString(i + 1, s);
            } else if (param instanceof Integer n) {
                stmt.setInt(i + 1, n);
            } else if (param instanceof Long n) {
                stmt.setLong(i + 1, n);
            } else if (param instanceof Double n) {
                stmt.setDouble(i + 1, n);
            } else if (param instanceof Float n) {
                stmt.setFloat(i + 1, n);
            } else if (param instanceof Boolean b) {
                stmt.setBoolean(i + 1, b);
            } else if (param instanceof byte[] bytes) {
                stmt.setBytes(i + 1, bytes);
            } else if (param instanceof UUID uuid) {
                stmt.setString(i + 1, uuid.toString());
            } else {
                stmt.setObject(i + 1, param);
            }
        }
    }
    
    private List<Map<String, Object>> resultSetToList(ResultSet rs) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();
        ResultSetMetaData meta = rs.getMetaData();
        int columnCount = meta.getColumnCount();
        
        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                String columnName = meta.getColumnLabel(i);
                Object value = rs.getObject(i);
                row.put(columnName, value);
            }
            results.add(row);
        }
        
        return results;
    }
    
    public void close() {
        executor.shutdown();
        
        Connection conn;
        while ((conn = connectionPool.poll()) != null) {
            try {
                conn.close();
            } catch (SQLException ignored) {}
        }
        
        logger.info("Database connection pool closed");
    }
    
    @FunctionalInterface
    public interface TransactionCallback {
        void execute(Connection connection) throws Exception;
    }
}
