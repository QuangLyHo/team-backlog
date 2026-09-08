import { useEffect, useState } from "react";
import AuthPage from "./AuthPage";
import TaskList from "./TaskList";
import CreateTaskForm from "./CreateTaskForm";
import ProjectList from "./ProjectList";
import TeamList from "./TeamList";
import Brand from "./Brand";

function App() {
  const [token, setToken] = useState(() => localStorage.getItem("token"));
  const [selectedTeam, setSelectedTeam] = useState(null);
  const [selectedProject, setSelectedProject] = useState(null);
  const [refreshKey, setRefreshKey] = useState(0);

  function handleAuthError() {
    setToken(null);
  }

  useEffect(() => {
    if (token) {
      localStorage.setItem("token", token);
    } else {
      localStorage.removeItem("token");
    }
  }, [token]);

  if (!token) {
    return <AuthPage onLogin={setToken} />;
  }

  if (!selectedTeam) {
    return (
      <TeamList
        token={token}
        onSelect={setSelectedTeam}
        onLogout={() => setToken(null)}
        onAuthError={handleAuthError}
      />
    );
  }

  if (!selectedProject) {
    return (
      <ProjectList
        token={token}
        team={selectedTeam}
        onSelect={setSelectedProject}
        onBack={() => setSelectedTeam(null)}
        onLogout={() => setToken(null)}
        onAuthError={handleAuthError}
      />
    );
  }

  return (
    <div className="app-shell">
      <Brand />
      <div className="topbar">
        <button className="btn-secondary" onClick={() => setSelectedProject(null)}>
          ← Back to projects
        </button>
        <button className="btn-secondary" onClick={() => setToken(null)}>
          Log out
        </button>
      </div>

      <h1>{selectedProject.name}</h1>

      <CreateTaskForm
        token={token}
        projectId={selectedProject.id}
        onCreated={() => setRefreshKey((k) => k + 1)}
        onAuthError={handleAuthError}
      />
      <TaskList
        token={token}
        projectId={selectedProject.id}
        refreshKey={refreshKey}
        onAuthError={handleAuthError}
      />
    </div>
  );
}

export default App;
