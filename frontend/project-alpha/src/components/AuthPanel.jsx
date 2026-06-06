export default function AuthPanel({ currentUser, onLogout }) {
  if (currentUser === null) {
    return null;
  }

  return (
    <div className="auth-panel">
      <p>Logged in as {currentUser.name}</p>
      <button type="button" onClick={onLogout}>
        Logout
      </button>
    </div>
  );
}
