import { useEffect, useState } from "react";
import { getProjectTasks, updateTaskStatus } from "./api";
import Loading from "./Loading";

export default function TaskList({ token, projectId, refreshKey, onAuthError }) {
  const [tasks, setTasks] = useState(null);
  const [error, setError] = useState(null);

  function load() {
    setTasks(null);
    getProjectTasks(projectId, token)
      .then(setTasks)
      .catch((err) => {
        if (err.status === 401) {
          onAuthError();
        } else {
          setError(err.message);
        }
      });
  }

  useEffect(load, [token, projectId, refreshKey]);

  async function handleStatusChange(taskId, newStatus) {
    try {
      await updateTaskStatus(taskId, newStatus, token);
      load();
    } catch (err) {
      if (err.status === 401) {
        onAuthError();
      } else {
        setError(err.message);
      }
    }
  }

  if (error) return <p className="error-text">{error}</p>;
  if (!tasks) return <Loading />;
  if (tasks.length === 0) return <p className="loading-text">No tasks yet.</p>;

  return (
    <ul className="list">
      {tasks.map((task) => (
        <li key={task.id} className="list-item">
          <div className="list-item-main">
            <span className="list-item-title">{task.title}</span>
            <span className="list-item-meta">
              {task.assignees.length === 0
                ? "Unassigned"
                : task.assignees.map((a) => `${a.firstName} ${a.lastName}`).join(", ")}
            </span>
          </div>
          <select
            className={`badge badge-${task.status}`}
            value={task.status}
            onChange={(e) => handleStatusChange(task.id, e.target.value)}
          >
            <option value="todo">To do</option>
            <option value="in_progress">In progress</option>
            <option value="done">Done</option>
          </select>
        </li>
      ))}
    </ul>
  );
}
