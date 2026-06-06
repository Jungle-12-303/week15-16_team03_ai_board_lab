import AuthPanel from './AuthPanel';

export default function Topbar({ currentUser, onLogout, searchTerm, onSearchChange }) {
  const hasSearch = onSearchChange !== undefined;

  return (
    <header className="topbar">
      <div className="topbar-inner">
        <div className="brand">Project Alpha</div>

        {hasSearch ? (
          <input
            className="search"
            value={searchTerm}
            onChange={(event) => onSearchChange(event.target.value)}
            placeholder="Search posts, tags, comments"
          />
        ) : (
          <div className="topbar-spacer" />
        )}

        <AuthPanel currentUser={currentUser} onLogout={onLogout} />
      </div>
    </header>
  );
}
