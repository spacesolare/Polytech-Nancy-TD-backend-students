package com.example.todoapp;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for {@link Task} model.
 */
public class TaskDao {

    private static final String DATABASE_URL = "jdbc:sqlite:database.db";

    public TaskDao() {
        initializeDatabase();
    }

    /**
     * Persist {@link Task} model.
     *
     * @param task task to save.
     * @return task model.
     */
    public Task save(Task task) {
        validateTask(task);

        if (task.id() == null) {
            return insertTask(task);
        }

        String sql = """
                INSERT INTO task(id, title, description, done)
                VALUES (?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    title = excluded.title,
                    description = excluded.description,
                    done = excluded.done
                """;

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, task.id());
            statement.setString(2, task.title());
            statement.setString(3, task.description());
            statement.setBoolean(4, task.done());
            statement.executeUpdate();
            return task;
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to save task", e);
        }
    }

    public List<Task> findAll() {
        String sql = "SELECT id, title, description, done FROM task ORDER BY id";
        List<Task> tasks = new ArrayList<>();

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                tasks.add(toTask(resultSet));
            }
            return tasks;
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to find tasks", e);
        }
    }

    public int remove(int id) {
        validateId(id);

        String sql = "DELETE FROM task WHERE id = ?";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to remove task", e);
        }
    }

    public Optional<Task> modify(int id, Task task) {
        validateId(id);
        validateTask(task);

        String sql = "UPDATE task SET title = ?, description = ?, done = ? WHERE id = ?";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, task.title());
            statement.setString(2, task.description());
            statement.setBoolean(3, task.done());
            statement.setInt(4, id);

            if (statement.executeUpdate() == 0) {
                return Optional.empty();
            }

            return findById(id);
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to modify task", e);
        }
    }

    /**
     * Retrieve {@link Task} model by id.
     *
     * @param id identifier of the {@link Task}.
     * @return {@link Task} model wrapped by Optional.
     */
    public Optional<Task> findById(int id) {
        validateId(id);

        String sql = "SELECT id, title, description, done FROM task WHERE id = ?";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(toTask(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to find task by id", e);
        }
    }

    private void initializeDatabase() {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            boolean tableExists = taskTableExists(connection);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS task (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        title TEXT NOT NULL,
                        description TEXT,
                        done INTEGER NOT NULL
                    )
                    """);

            if (!tableExists) {
                seedDefaultTasks(connection);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to initialize database", e);
        }
    }

    private boolean taskTableExists(Connection connection) throws SQLException {
        String sql = "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'task'";

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next();
        }
    }

    private void seedDefaultTasks(Connection connection) throws SQLException {
        String sql = "INSERT INTO task(id, title, description, done) VALUES (?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            addBatch(statement, new Task(1, "R\u00e9viser DS de maths", "S\u00e9ries num\u00e9riques et probabilit\u00e9s.", false));
            addBatch(statement, new Task(2, "Valider mon PIVE", "PIVE Club Poker.", true));
            addBatch(statement, new Task(3, "Choisir mon parcours de 4A", "SIR ou SIA ?", false));
            statement.executeBatch();
        }
    }

    private Task insertTask(Task task) {
        String sql = "INSERT INTO task(title, description, done) VALUES (?, ?, ?)";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, task.title());
            statement.setString(2, task.description());
            statement.setBoolean(3, task.done());
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return new Task(generatedKeys.getInt(1), task.title(), task.description(), task.done());
                }
                throw new IllegalStateException("Unable to retrieve generated task id");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to insert task", e);
        }
    }

    private void addBatch(PreparedStatement statement, Task task) throws SQLException {
        statement.setInt(1, task.id());
        statement.setString(2, task.title());
        statement.setString(3, task.description());
        statement.setBoolean(4, task.done());
        statement.addBatch();
    }

    private void validateTask(Task task) {
        if (task == null) {
            throw new IllegalArgumentException("Task is required");
        }
        if (task.id() != null) {
            validateId(task.id());
        }
        if (task.title() == null || task.title().isBlank()) {
            throw new IllegalArgumentException("Task title is required");
        }
    }

    private void validateId(int id) {
        if (id < 1) {
            throw new IllegalArgumentException("Task id must be greater than 0");
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DATABASE_URL);
    }

    private Task toTask(ResultSet resultSet) throws SQLException {
        return new Task(
                resultSet.getInt("id"),
                resultSet.getString("title"),
                resultSet.getString("description"),
                resultSet.getBoolean("done")
        );
    }
}
