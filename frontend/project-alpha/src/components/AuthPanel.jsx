export default function AuthPanel({ currentUser, onLogin, onLogout }) {
  return (
    <div className="auth-panel">
      {currentUser === null ? (
        <button type="button" onClick={onLogin}>
          Login as cedis
        </button>
      ) : (
        <>
          <p>Logged in as {currentUser.name}</p>
          <button type="button" onClick={onLogout}>
            Logout
          </button>
        </>
      )}
    </div>
  );
}
