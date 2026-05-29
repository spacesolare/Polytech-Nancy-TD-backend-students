package com.example.todoapp;

import java.util.*;

/**
 * Data Access Object for {@link Task} model.
 */
public class TaskDao {

    private final Map<Integer, Task> storage = new HashMap<>();

    {
        save(new Task(1, "Réviser DS de maths", "Séries numériques et probabilités.", false));
        save(new Task(2, "Valider mon PIVE", "PIVE Club Poker.", true));
        save(new Task(3, "Choisir mon parcours de 4A", "SIR ou SIA ?", false));
    }

    /**
     * Persist {@link Task} model.
     * @param task task to save.
     * @return task model.
     */

    public Task save(Task task) {
        storage.put(task.id(), task);
        return task;
    }
    public List<Task> findAll(){return new ArrayList<>(storage.values());}

    public int remove(int id) {
        if (storage.containsKey(id)) {
            storage.remove(id);
            return 1;
        }
        else {
            return 0;
        }
    }
    public Optional<Task> modify(int id, Task task) {
        if (!storage.containsKey(id)) {
            return Optional.empty();
        }
        storage.replace(id,storage.get(id),task);

        return Optional.ofNullable(storage.get(id));
    }
    /**
     * Retrieve {@link Task} model by id.
     * @param id identifier of the {@link Task}.
     * @return {@link Task} model wrapped by Optional.
     */
    public Optional<Task> findById(int id) {
        return Optional.ofNullable(storage.get(id));
    }
}
