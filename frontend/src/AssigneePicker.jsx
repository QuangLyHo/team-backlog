import { useEffect, useRef, useState } from "react";

export default function AssigneePicker({ users, selectedIds, onChange }) {
  const [open, setOpen] = useState(false);
  const ref = useRef(null);

  useEffect(() => {
    function handleClickOutside(e) {
      if (ref.current && !ref.current.contains(e.target)) {
        setOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  function toggleUser(id) {
    if (selectedIds.includes(id)) {
      onChange(selectedIds.filter((existingId) => existingId !== id));
    } else {
      onChange([...selectedIds, id]);
    }
  }

  const selectedUsers = users.filter((user) => selectedIds.includes(user.id));
  const summary =
    selectedUsers.length === 0
      ? "Assign to..."
      : selectedUsers.map((user) => `${user.firstName} ${user.lastName}`).join(", ");

  return (
    <div className="assignee-picker" ref={ref}>
      <button
        type="button"
        className={`assignee-picker-trigger${selectedUsers.length === 0 ? " assignee-picker-trigger-empty" : ""}`}
        onClick={() => setOpen((o) => !o)}
      >
        {summary}
      </button>

      {open && (
        <div className="assignee-picker-dropdown">
          {users.length === 0 ? (
            <p className="loading-text" style={{ padding: "8px 10px" }}>
              No users found.
            </p>
          ) : (
            users.map((user) => (
              <label key={user.id} className="assignee-picker-option">
                <input
                  type="checkbox"
                  checked={selectedIds.includes(user.id)}
                  onChange={() => toggleUser(user.id)}
                />
                {user.firstName} {user.lastName}
              </label>
            ))
          )}
        </div>
      )}
    </div>
  );
}
