import { useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';

export default function SignupPage({ currentUser, onSignUp }) {
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [errorMessage, setErrorMessage] = useState('');

  const canSignUp =
    username.trim().length > 0 && password.length > 0 && confirmPassword.length > 0;

  if (currentUser !== null) {
    return <Navigate to="/" replace />;
  }

  function handleSubmit(event) {
    event.preventDefault();

    if (!canSignUp) {
      setErrorMessage('Username and password are required.');
      return;
    }

    if (password.length < 6) {
      setErrorMessage('Password must be at least 6 characters.');
      return;
    }

    if (password !== confirmPassword) {
      setErrorMessage('Passwords do not match.');
      return;
    }

    const signUpSucceeded = onSignUp(username, password);

    if (!signUpSucceeded) {
      setErrorMessage('Username is already taken.');
      return;
    }

    navigate('/login');
  }

  return (
    <section>
      <h2>Sign up</h2>

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

        <label>
          Confirm password
          <input
            type="password"
            value={confirmPassword}
            onChange={(event) => {
              setConfirmPassword(event.target.value);
              setErrorMessage('');
            }}
          />
        </label>

        {errorMessage.length > 0 && <p>{errorMessage}</p>}

        <button type="submit" disabled={!canSignUp}>
          Sign up
        </button>
      </form>

      <Link to="/login">Back to login</Link>
    </section>
  );
}
