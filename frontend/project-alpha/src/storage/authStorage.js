const currentUserStorageKey = 'project-alpha-current-user';

export function loadStoredCurrentUser() {
  const storedCurrentUser = localStorage.getItem(currentUserStorageKey);

  if (storedCurrentUser === null) {
    return null;
  }

  try {
    const parsedCurrentUser = JSON.parse(storedCurrentUser);

    if (
      parsedCurrentUser !== null &&
      typeof parsedCurrentUser.name === 'string' &&
      typeof parsedCurrentUser.token === 'string' &&
      parsedCurrentUser.token.length > 0
    ) {
      return parsedCurrentUser;
    }
  } catch {
    return null;
  }

  return null;
}

export function saveStoredCurrentUser(currentUser) {
  if (currentUser === null) {
    localStorage.removeItem(currentUserStorageKey);
    return;
  }

  localStorage.setItem(currentUserStorageKey, JSON.stringify(currentUser));
}
