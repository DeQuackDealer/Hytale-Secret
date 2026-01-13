package rubidium.database;

import java.sql.SQLException;
import java.util.*;

public class QueryBuilder {
    
    private final Database database;
    private final String table;
    private final List<String> selectColumns;
    private final List<String> whereClauses;
    private final List<Object> whereParams;
    private final List<String> orderBy;
    private String groupBy;
    private Integer limit;
    private Integer offset;
    
    public QueryBuilder(Database database, String table) {
        this.database = database;
        this.table = table;
        this.selectColumns = new ArrayList<>();
        this.whereClauses = new ArrayList<>();
        this.whereParams = new ArrayList<>();
        this.orderBy = new ArrayList<>();
    }
    
    public static QueryBuilder table(Database database, String table) {
        return new QueryBuilder(database, table);
    }
    
    public QueryBuilder select(String... columns) {
        selectColumns.addAll(Arrays.asList(columns));
        return this;
    }
    
    public QueryBuilder where(String column, Object value) {
        whereClauses.add(column + " = ?");
        whereParams.add(value);
        return this;
    }
    
    public QueryBuilder where(String column, String operator, Object value) {
        whereClauses.add(column + " " + operator + " ?");
        whereParams.add(value);
        return this;
    }
    
    public QueryBuilder whereIn(String column, Collection<?> values) {
        String placeholders = String.join(", ", Collections.nCopies(values.size(), "?"));
        whereClauses.add(column + " IN (" + placeholders + ")");
        whereParams.addAll(values);
        return this;
    }
    
    public QueryBuilder whereNull(String column) {
        whereClauses.add(column + " IS NULL");
        return this;
    }
    
    public QueryBuilder whereNotNull(String column) {
        whereClauses.add(column + " IS NOT NULL");
        return this;
    }
    
    public QueryBuilder whereLike(String column, String pattern) {
        whereClauses.add(column + " LIKE ?");
        whereParams.add(pattern);
        return this;
    }
    
    public QueryBuilder whereBetween(String column, Object start, Object end) {
        whereClauses.add(column + " BETWEEN ? AND ?");
        whereParams.add(start);
        whereParams.add(end);
        return this;
    }
    
    public QueryBuilder orderBy(String column) {
        orderBy.add(column + " ASC");
        return this;
    }
    
    public QueryBuilder orderBy(String column, String direction) {
        orderBy.add(column + " " + direction.toUpperCase());
        return this;
    }
    
    public QueryBuilder orderByDesc(String column) {
        orderBy.add(column + " DESC");
        return this;
    }
    
    public QueryBuilder groupBy(String column) {
        this.groupBy = column;
        return this;
    }
    
    public QueryBuilder limit(int limit) {
        this.limit = limit;
        return this;
    }
    
    public QueryBuilder offset(int offset) {
        this.offset = offset;
        return this;
    }
    
    public List<Map<String, Object>> get() throws SQLException {
        return database.query(buildSelectQuery(), whereParams.toArray());
    }
    
    public Optional<Map<String, Object>> first() throws SQLException {
        limit(1);
        List<Map<String, Object>> results = get();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
    
    public long count() throws SQLException {
        String sql = "SELECT COUNT(*) as count FROM " + table + buildWhereClause();
        Optional<Map<String, Object>> result = database.queryOne(sql, whereParams.toArray());
        return result.map(r -> ((Number) r.get("count")).longValue()).orElse(0L);
    }
    
    public boolean exists() throws SQLException {
        return count() > 0;
    }
    
    public int update(Map<String, Object> values) throws SQLException {
        StringBuilder sql = new StringBuilder("UPDATE ").append(table).append(" SET ");
        
        List<Object> params = new ArrayList<>();
        List<String> sets = new ArrayList<>();
        
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            sets.add(entry.getKey() + " = ?");
            params.add(entry.getValue());
        }
        
        sql.append(String.join(", ", sets));
        sql.append(buildWhereClause());
        
        params.addAll(whereParams);
        
        return database.update(sql.toString(), params.toArray());
    }
    
    public long insert(Map<String, Object> values) throws SQLException {
        StringBuilder sql = new StringBuilder("INSERT INTO ").append(table).append(" (");
        
        List<String> columns = new ArrayList<>(values.keySet());
        List<Object> params = new ArrayList<>(values.values());
        
        sql.append(String.join(", ", columns));
        sql.append(") VALUES (");
        sql.append(String.join(", ", Collections.nCopies(columns.size(), "?")));
        sql.append(")");
        
        return database.insert(sql.toString(), params.toArray());
    }
    
    public int delete() throws SQLException {
        String sql = "DELETE FROM " + table + buildWhereClause();
        return database.update(sql, whereParams.toArray());
    }
    
    private String buildSelectQuery() {
        StringBuilder sql = new StringBuilder("SELECT ");
        
        if (selectColumns.isEmpty()) {
            sql.append("*");
        } else {
            sql.append(String.join(", ", selectColumns));
        }
        
        sql.append(" FROM ").append(table);
        sql.append(buildWhereClause());
        
        if (groupBy != null) {
            sql.append(" GROUP BY ").append(groupBy);
        }
        
        if (!orderBy.isEmpty()) {
            sql.append(" ORDER BY ").append(String.join(", ", orderBy));
        }
        
        if (limit != null) {
            sql.append(" LIMIT ").append(limit);
        }
        
        if (offset != null) {
            sql.append(" OFFSET ").append(offset);
        }
        
        return sql.toString();
    }
    
    private String buildWhereClause() {
        if (whereClauses.isEmpty()) {
            return "";
        }
        return " WHERE " + String.join(" AND ", whereClauses);
    }
}
