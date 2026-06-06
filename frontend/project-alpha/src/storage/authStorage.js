import { initialUsers } from '../data/seedData';

const usersStorageKey = 'project-alpha-users';
const currentUserStorageKey = 'project-alpha-current-user';

export function loadStoredUsers() {
  const storedUsers = localStorage.getItem(usersStorageKey);

  if (storedUsers === null) {
    return initialUsers;
  }

  try {
    const parsedUsers = JSON.parse(storedUsers);

    if (!Array.isArray(parsedUsers)) {
      return initialUsers;
    }

    return parsedUsers.filter(
      (user) => typeof user.username === 'string' && typeof user.password === 'string',
    );
  } catch {
    return initialUsers;
  }
}

export function loadStoredCurrentUser() {
  const storedCurrentUser = localStorage.getItem(currentUserStorageKey);

  if (storedCurrentUser === null) {
    return null;
  }

  try {
    const parsedCurrentUser = JSON.parse(storedCurrentUser);

    if (parsedCurrentUser !== null && typeof parsedCurrentUser.name === 'string') {
      return parsedCurrentUser;
    }
  } catch {
    return null;
  }

  return null;
}

export function saveStoredUsers(users) {
  localStorage.setItem(usersStorageKey, JSON.stringify(users));
}

export function saveStoredCurrentUser(currentUser) {
  if (currentUser === null) {
    localStorage.removeItem(currentUserStorageKey);
    return;
  }

  localStorage.setItem(currentUserStorageKey, JSON.stringify(currentUser));
}
