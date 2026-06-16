import { useEffect, useState } from 'react';
import {
  getCurrentUser,
  logoutAll as logoutAllRequest,
  login as loginRequest,
  logout as logoutRequest,
  signUp as signUpRequest,
} from '../api/authApi';

export default function useAuth() {
  const [currentUser, setCurrentUser] = useState(null);
  const [isLoadingAuth, setIsLoadingAuth] = useState(true);

  useEffect(() => {
    let ignore = false;

    async function loadCurrentUser() {
      try {
        const user = await getCurrentUser();

        if (!ignore) {
          setCurrentUser(user);
        }
      } catch {
        if (!ignore) {
          setCurrentUser(null);
        }
      } finally {
        if (!ignore) {
          setIsLoadingAuth(false);
        }
      }
    }

    loadCurrentUser();

    return () => {
      ignore = true;
    };
  }, []);

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

  async function logout() {
    try {
      await logoutRequest();
    } finally {
      setCurrentUser(null);
    }
  }

  async function logoutAll() {
    try {
      await logoutAllRequest();
    } finally {
      setCurrentUser(null);
    }
  }

  return {
    currentUser,
    isLoadingAuth,
    login,
    signUp,
    logout,
    logoutAll,
  };
}
