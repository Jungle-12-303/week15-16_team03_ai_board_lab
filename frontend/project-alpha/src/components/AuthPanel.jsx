export default function AuthPanel({ currentUser, onLogout, onLogoutAll }) {
  if (currentUser === null) {
    return null;
  }

  return (
    <div className="user-area">
      <span>{currentUser.name}</span>
      <button type="button" className="plain-button" onClick={onLogout}>
        Logout
      </button>
      {onLogoutAll !== undefined && (
        <button type="button" className="plain-button" onClick={onLogoutAll}>
          Logout all
        </button>
      )}
    </div>
  );
}
