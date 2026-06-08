import { useEffect, useState } from 'react';
import { login as loginRequest, signUp as signUpRequest } from '../api/authApi';
import { loadStoredCurrentUser, saveStoredCurrentUser } from '../storage/authStorage';

export default function useAuth() {
  const [currentUser, setCurrentUser] = useState(loadStoredCurrentUser);

  useEffect(() => {
    saveStoredCurrentUser(currentUser);
  }, [currentUser]);

  async function login(username, password) {
    const trimmedUsername = username.trim();

    try {
      const user = await loginRequest(trimmedUsername, password);
      setCurrentUser(user);
      return true;
    } catch {
      return false;
    }
  }

  async function signUp(username, password) {
    const trimmedUsername = username.trim();

    try {
      await signUpRequest(trimmedUsername, password);
      return true;
    } catch {
      return false;
    }
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
