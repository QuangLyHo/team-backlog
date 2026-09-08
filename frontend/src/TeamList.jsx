import { useEffect, useState } from "react";
import { getTeams, createTeam, addTeamMember, getUsers, getTeamMembers } from "./api";
import Loading from "./Loading";
import Brand from "./Brand";

export default function TeamList({ token, onSelect, onLogout, onAuthError }) {
  const [teams, setTeams] = useState(null);
  const [users, setUsers] = useState([]);
  const [error, setError] = useState(null);
  const [name, setName] = useState("");
  const [creating, setCreating] = useState(false);
  const [addingMemberTo, setAddingMemberTo] = useState(null);
  const [selectedUserId, setSelectedUserId] = useState("");
  const [teamMembers, setTeamMembers] = useState({});

  function load() {
    setTeams(null);
    getTeams(token)
      .then(setTeams)
      .catch((err) => {
        if (err.status === 401) {
          onAuthError();
        } else {
          setError(err.message);
        }
      });
  }

  useEffect(load, [token]);

  useEffect(() => {
    getUsers(token)
      .then((page) => setUsers(page.content))
      .catch(() => {});
  }, [token]);

  useEffect(() => {
    if (!teams) return;

    teams.forEach((team) => {
      getTeamMembers(team.id, token)
        .then((members) => {
          setTeamMembers((prev) => ({ ...prev, [team.id]: members }));
        })
        .catch(() => {});
    });
  }, [token, teams]);

  async function handleCreate(e) {
    e.preventDefault();
    setCreating(true);
    setError(null);

    try {
      await createTeam({ name }, token);
      setName("");
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

  async function handleAddMember(teamId) {
    if (!selectedUserId) return;

    try {
      await addTeamMember(teamId, Number(selectedUserId), token);
      setAddingMemberTo(null);
      setSelectedUserId("");
    } catch (err) {
      if (err.status === 401) {
        onAuthError();
      } else {
        setError(err.message);
      }
    }
  }

  return (
    <div className="app-shell">
      <Brand />
      <div className="topbar">
        <h1>Teams</h1>
        <button className="btn-secondary" onClick={onLogout}>
          Log out
        </button>
      </div>

      <div className="card">
        <form onSubmit={handleCreate} className="form-row">
          <input
            placeholder="Team name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
          />
          <button
            type="submit"
            className="btn-primary"
            disabled={creating}
            style={{ flex: "none" }}
          >
            {creating ? <span className="spinner spinner-current" /> : "Create team"}
          </button>
        </form>
      </div>

      {error && <p className="error-text">{error}</p>}

      {!teams ? (
        <Loading />
      ) : teams.length === 0 ? (
        <p className="loading-text">No teams yet. Create one to get started.</p>
      ) : (
        <ul className="list">
          {teams.map((team) => (
            <li key={team.id} className="list-item" style={{ flexWrap: "wrap" }}>
              <div className="list-item-main">
                <span className="list-item-title">{team.name}</span>
              </div>

              <span className="list-item-meta">
                {!teamMembers[team.id] || teamMembers[team.id].length === 0
                  ? "No members yet"
                  : teamMembers[team.id].map((m) => `${m.firstName} ${m.lastName}`).join(", ")}
              </span>

              {addingMemberTo === team.id ? (
                <div style={{ display: "flex", gap: 8 }}>
                  <select
                    value={selectedUserId}
                    onChange={(e) => setSelectedUserId(e.target.value)}
                  >
                    <option value="">Select user...</option>
                    {users.map((user) => (
                      <option key={user.id} value={user.id}>
                        {user.firstName} {user.lastName}
                      </option>
                    ))}
                  </select>
                  <button className="btn-secondary" onClick={() => handleAddMember(team.id)}>
                    Add
                  </button>

                  <button className="btn-secondary" onClick={() => setAddingMemberTo(null)}>
                    Cancel
                  </button>
                </div>
              ) : (
                <div style={{ display: "flex", gap: 8 }}>
                  <button className="btn-secondary" onClick={() => setAddingMemberTo(team.id)}>
                    Add member
                  </button>
                  <button className="btn-secondary" onClick={() => onSelect(team)}>
                    Open
                  </button>
                </div>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
