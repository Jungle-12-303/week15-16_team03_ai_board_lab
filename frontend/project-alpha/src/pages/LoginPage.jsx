import { Navigate, useNavigate } from 'react-router-dom';

export default function LoginPage({ currentUser, onLogin }) {
  const navigate = useNavigate();

  if (currentUser !== null) {
    return <Navigate to="/" replace />;
  }

  function handleSubmit(event) {
    event.preventDefault();
    onLogin();
    navigate('/');
  }

  return (
    <section>
      <h2>Login</h2>

      <form onSubmit={handleSubmit}>
        <label>
          Username
          <input value="cedis" readOnly />
        </label>

        <button type="submit">Login</button>
      </form>
    </section>
  );
}
