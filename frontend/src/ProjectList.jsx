import { useEffect, useState } from "react";
import { getTeamProjects, createProject } from "./api";
import Loading from "./Loading";
import Brand from "./Brand";

const currencyFormatter = new Intl.NumberFormat("en-US", {
  style: "currency",
  currency: "USD",
  maximumFractionDigits: 0,
});

function formatCurrency(value) {
  return currencyFormatter.format(value);
}

export default function ProjectList({ token, team, onSelect, onBack, onLogout, onAuthError }) {
  const [projects, setProjects] = useState(null);
  const [error, setError] = useState(null);
  const [name, setName] = useState("");
  const [budget, setBudget] = useState("");
  const [creating, setCreating] = useState(false);

  function load() {
    setProjects(null);

    getTeamProjects(team.id, token)
      .then((page) => setProjects(page.content))
      .catch((err) => {
        if (err.status === 401) {
          onAuthError();
        } else {
          setError(err.message);
        }
      });
  }

  useEffect(load, [token, team.id]);

  async function handleCreate(e) {
    e.preventDefault();
    setCreating(true);
    setError(null);

    try {
      await createProject({ name, budget: Number(budget), teamId: team.id }, token);
      setName("");
      setBudget("");
      load();
    } catch (err) {
      if (err.status === 401) {
        onAuthError();
      } else {
        setError(err.message);
      }
    } finally {
      setCreating(false);
    }
  }

  return (
    <div className="app-shell">
      <Brand />
      <div className="topbar">
        <button className="btn-secondary" onClick={onBack}>
          ← Back to teams
        </button>
        <button className="btn-secondary" onClick={onLogout}>
          Log out
        </button>
      </div>

      <h1>{team.name}</h1>

      <div className="card">
        <form onSubmit={handleCreate} className="form-row">
          <input
            placeholder="Project name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
          />
          <input
            type="number"
            placeholder="Budget"
            value={budget}
            onChange={(e) => setBudget(e.target.value)}
          />
          <button
            type="submit"
            className="btn-primary"
            disabled={creating}
            style={{ flex: "none" }}
          >
            {creating ? <span className="spinner spinner-current" /> : "Create project"}
          </button>
        </form>
      </div>

      {error && <p className="error-text">{error}</p>}

      {!projects ? (
        <Loading />
      ) : projects.length === 0 ? (
        <p className="loading-text">No projects yet.</p>
      ) : (
        <ul className="list">
          {projects.map((project) => (
            <li key={project.id} className="list-item">
              <div className="list-item-main">
                <span className="list-item-title">{project.name}</span>
                <span className="list-item-meta">Budget: {formatCurrency(project.budget)}</span>
              </div>
              <button className="btn-secondary" onClick={() => onSelect(project)}>
                View backlog
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
