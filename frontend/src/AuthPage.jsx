import { useState } from "react";
import { login, register } from "./api";

export default function AuthPage({ onLogin }) {
  const [mode, setMode] = useState("login");
  const [form, setForm] = useState({ email: "", password: "", firstName: "", lastName: "" });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  function handleChange(e) {
    setForm((prevForm) => ({ ...prevForm, [e.target.name]: e.target.value }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      if (mode === "register") {
        await register(form);
      }
      const { token } = await login({ email: form.email, password: form.password });

      onLogin(token);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth-shell">
      <div className="auth-layout">
        <div className="auth-header">
          <h1 className="auth-title">Team Backlog</h1>
          <p className="auth-tagline">
            A project and task tracker for small teams — projects, backlogs, assignees, and status
            workflow.
          </p>
          <p className="auth-tagline">
            Built end-to-end (Spring Boot API, JWT auth, MySQL, React frontend, Docker, and a live
            cloud deployment) as a hands-on way to learn full-stack development.
          </p>
        </div>

        <div className="auth-form-wrapper">
          <h1 style={{ marginBottom: 20 }}>{mode === "login" ? "Log in" : "Sign up"}</h1>

          <form onSubmit={handleSubmit} className="form-stack">
            {mode === "register" && (
              <>
                <input
                  name="firstName"
                  placeholder="First name"
                  value={form.firstName}
                  onChange={handleChange}
                  required
                />
                <input
                  name="lastName"
                  placeholder="Last name"
                  value={form.lastName}
                  onChange={handleChange}
                  required
                />
              </>
            )}
            <input
              name="email"
              type="email"
              placeholder="Email"
              value={form.email}
              onChange={handleChange}
              required
            />
            <input
              name="password"
              type="password"
              placeholder="Password"
              value={form.password}
              onChange={handleChange}
              required
            />

            <button type="submit" className="btn-primary" disabled={loading}>
              {loading ? (
                <span className="spinner spinner-current" />
              ) : mode === "login" ? (
                "Log in"
              ) : (
                "Sign up"
              )}
            </button>
          </form>

          {error && (
            <p className="error-text" style={{ marginTop: 12 }}>
              {error}
            </p>
          )}

          <button
            type="button"
            className="btn-link"
            style={{ marginTop: 16 }}
            onClick={() => setMode(mode === "login" ? "register" : "login")}
          >
            {mode === "login" ? "Need an account? Sign up" : "Already have an account? Log in"}
          </button>
        </div>
      </div>
    </div>
  );
}
