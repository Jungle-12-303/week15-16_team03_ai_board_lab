import { useEffect, useState } from 'react';
import {
  loadStoredCurrentUser,
  loadStoredUsers,
  saveStoredCurrentUser,
  saveStoredUsers,
} from '../storage/authStorage';

export default function useAuth() {
  const [currentUser, setCurrentUser] = useState(loadStoredCurrentUser);
  const [users, setUsers] = useState(loadStoredUsers);

  useEffect(() => {
    saveStoredUsers(users);
  }, [users]);

  useEffect(() => {
    saveStoredCurrentUser(currentUser);
  }, [currentUser]);

  function login(username, password) {
    const trimmedUsername = username.trim();
    const foundUser = users.find(
      (user) => user.username === trimmedUsername && user.password === password,
    );

    if (!foundUser) {
      return false;
    }

    setCurrentUser({ name: foundUser.username });
    return true;
  }

  function signUp(username, password) {
    const trimmedUsername = username.trim();
    const isUsernameTaken = users.some((user) => user.username === trimmedUsername);

    if (isUsernameTaken) {
      return false;
    }

    setUsers((currentUsers) => [
      ...currentUsers,
      {
        username: trimmedUsername,
        password: password,
      },
    ]);

    return true;
  }

  function logout() {
    setCurrentUser(null);
  }

  return {
    currentUser,
    login,
    signUp,
    logout,
  };
}
