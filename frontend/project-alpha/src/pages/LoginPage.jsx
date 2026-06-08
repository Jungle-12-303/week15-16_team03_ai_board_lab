import { useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';

export default function LoginPage({ currentUser, onLogin }) {
  const navigate = useNavigate();
  const [username, setUsername] = useState('cedis');
  const [password, setPassword] = useState('');
  const [errorMessage, setErrorMessage] = useState('');

  const canLogin = username.trim().length > 0 && password.trim().length > 0;

  if (currentUser !== null) {
    return <Navigate to="/" replace />;
  }

  async function handleSubmit(event) {
    event.preventDefault();

    const trimmedUsername = username.trim();

    if (!canLogin) {
      setErrorMessage('Username and password are required.');
      return;
    }

    const loginSucceeded = await onLogin(trimmedUsername, password);

    if (!loginSucceeded) {
      setErrorMessage('Username or password is incorrect.');
      return;
    }

    navigate('/');
  }

  return (
    <section>
      <h2>Login</h2>

      <form onSubmit={handleSubmit}>
        <label>
          Username
          <input
            value={username}
            onChange={(event) => {
              setUsername(event.target.value);
              setErrorMessage('');
            }}
          />
        </label>

        <label>
          Password
          <input
            type="password"
            value={password}
            onChange={(event) => {
              setPassword(event.target.value);
              setErrorMessage('');
            }}
          />
        </label>

        {errorMessage.length > 0 && <p>{errorMessage}</p>}

        <button type="submit" disabled={!canLogin}>
          Login
        </button>
      </form>

      <Link to="/signup">Create an account</Link>
    </section>
  );
}
