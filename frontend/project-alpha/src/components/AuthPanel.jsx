export default function AuthPanel({ currentUser, onLogout }) {
  if (currentUser === null) {
    return null;
  }

  return (
    <div className="user-area">
      <span>{currentUser.name}</span>
      <button type="button" className="plain-button" onClick={onLogout}>
        Logout
      </button>
    </div>
  );
}
