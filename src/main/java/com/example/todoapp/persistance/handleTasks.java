package com.example.todoapp.persistance;

import com.example.todoapp.JsonUtils;
import com.example.todoapp.Task;
import com.example.todoapp.TaskDao;
import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.nonNull;

public final class handleTasks {

    private static final Logger log = LoggerFactory.getLogger(handleTasks.class);
    private static final int MAX_TITLE_LENGTH = 50;
    private static final int MAX_DESCRIPTION_LENGTH = 255;
    private static final Pattern TASK_WITH_ID_PATH = Pattern.compile("^/tasks/([^/]+)$");
    private static final TaskDao dao = new TaskDao();

    private handleTasks() {
    }

    public static void handleTasks(HttpExchange exchange) throws IOException {
        try {
            handleRequest(exchange);
        } catch (IllegalArgumentException e) {
            sendResponse(exchange, 400, null);
        } catch (Exception e) {
            log.error("Unexpected error while handling task request", e);
            sendResponse(exchange, 500, null);
        }
    }

    private static void handleRequest(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        //region Manage POST /tasks + JSON
        if ("POST".equals(method) && "/tasks".equals(path)) {
            Task input = readTask(exchange);
            validateTaskLength(input);
            Task createdTask = dao.save(input);

            exchange.getResponseHeaders().add("Location", "/tasks/" + createdTask.id());
            sendResponse(exchange, 201, JsonUtils.serialize(createdTask));
            return;
        }
        //endregion

        Matcher m = TASK_WITH_ID_PATH.matcher(path);

        //region Manage GET /tasks/{id}
        if ("GET".equals(method) && m.matches()) {
            int id = parseTaskId(m.group(1));
            Optional<Task> task = dao.findById(id);

            if (task.isPresent()) {
                sendResponse(exchange, 200, JsonUtils.serialize(task.get()));
            } else {
                sendResponse(exchange, 404, null);
            }
            return;
        }
        //endregion

        //region Manage GET /tasks
        if ("GET".equals(method) && "/tasks".equals(path)) {
            List<Task> tasks = dao.findAll();

            if (!tasks.isEmpty()) {
                sendResponse(exchange, 200, JsonUtils.serialize(tasks));
            } else {
                sendResponse(exchange, 404, null);
            }
            return;
        }
        //endregion

        //region Manage DELETE /tasks/{id}
        if ("DELETE".equals(method) && m.matches()) {
            int id = parseTaskId(m.group(1));
            int deleted = dao.remove(id);

            if (deleted == 1) {
                sendResponse(exchange, 204, null);
            } else {
                sendResponse(exchange, 404, null);
            }
            return;
        }
        //endregion

        //region Manage PUT /tasks/{id} + JSON
        if ("PUT".equals(method) && m.matches()) {
            int id = parseTaskId(m.group(1));
            Task updatedTask = readTask(exchange);
            validateTaskLength(updatedTask);
            Optional<Task> task = dao.modify(id, updatedTask);

            if (task.isPresent()) {
                sendResponse(exchange, 204, null);
            } else {
                sendResponse(exchange, 404, null);
            }
            return;
        }
        //endregion

        sendResponse(exchange, 404, null);
    }

    private static int parseTaskId(String rawId) {
        try {
            int id = Integer.parseInt(rawId);
            if (id < 1) {
                throw new IllegalArgumentException("Task id must be greater than 0");
            }
            return id;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Task id must be a valid integer", e);
        }
    }

    private static Task readTask(HttpExchange exchange) {
        try {
            return JsonUtils.deserialize(new String(exchange.getRequestBody().readAllBytes(), UTF_8), Task.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid task JSON", e);
        }
    }

    private static void validateTaskLength(Task task) {
        if (task.title() != null && task.title().length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("Task title must be 50 characters or less");
        }
        if (task.description() != null && task.description().length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("Task description must be 255 characters or less");
        }
    }

    private static void sendResponse(HttpExchange exchange, int status, String json) throws IOException {
        if (nonNull(json)) {
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            byte[] bytes = json.getBytes(UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } else {
            exchange.sendResponseHeaders(status, 0);
            exchange.close();
        }
    }
}
