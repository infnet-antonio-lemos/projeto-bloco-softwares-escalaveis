import { BrowserRouter, Routes, Route, NavLink } from 'react-router-dom';
import PetsPage from './pages/PetsPage';
import OwnersPage from './pages/OwnersPage';
import AppointmentsPage from './pages/AppointmentsPage';

export default function App() {
  return (
    <BrowserRouter>
      <nav className="navbar">
        <span className="navbar-brand">Pet Clinic</span>
        <NavLink to="/" end className={({ isActive }) => isActive ? 'nav-link active' : 'nav-link'}>Pets</NavLink>
        <NavLink to="/owners" className={({ isActive }) => isActive ? 'nav-link active' : 'nav-link'}>Owners</NavLink>
        <NavLink to="/appointments" className={({ isActive }) => isActive ? 'nav-link active' : 'nav-link'}>Appointments</NavLink>
      </nav>
      <main className="container">
        <Routes>
          <Route path="/" element={<PetsPage />} />
          <Route path="/owners" element={<OwnersPage />} />
          <Route path="/appointments" element={<AppointmentsPage />} />
        </Routes>
      </main>
    </BrowserRouter>
  );
}
