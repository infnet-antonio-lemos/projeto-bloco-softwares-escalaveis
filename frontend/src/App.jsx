import { BrowserRouter, Routes, Route, NavLink } from 'react-router-dom';
import PetsPage from './pages/PetsPage';
import OwnersPage from './pages/OwnersPage';

export default function App() {
  return (
    <BrowserRouter>
      <nav className="navbar">
        <span className="navbar-brand">Pet Clinic</span>
        <NavLink to="/" end className={({ isActive }) => isActive ? 'nav-link active' : 'nav-link'}>Pets</NavLink>
        <NavLink to="/owners" className={({ isActive }) => isActive ? 'nav-link active' : 'nav-link'}>Owners</NavLink>
      </nav>
      <main className="container">
        <Routes>
          <Route path="/" element={<PetsPage />} />
          <Route path="/owners" element={<OwnersPage />} />
        </Routes>
      </main>
    </BrowserRouter>
  );
}
