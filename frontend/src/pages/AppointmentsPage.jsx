import { useState, useEffect } from 'react';
import { getPets } from '../services/petService';
import {
  getAppointments,
  createAppointment,
  updateAppointment,
  updateAppointmentStatus,
  deleteAppointment,
} from '../services/appointmentService';
import AppointmentForm from '../components/AppointmentForm';
import AppointmentList from '../components/AppointmentList';

const STATUSES = ['', 'SCHEDULED', 'COMPLETED', 'CANCELLED', 'NO_SHOW'];

export default function AppointmentsPage() {
  const [appointments, setAppointments] = useState([]);
  const [pets, setPets] = useState([]);
  const [statusFilter, setStatusFilter] = useState('');
  const [editing, setEditing] = useState(null);
  const [error, setError] = useState('');

  const load = () =>
    getAppointments(statusFilter ? { status: statusFilter } : {})
      .then(setAppointments)
      .catch((e) => setError(e.message));

  // A lista de pets vem do monolito — outro serviço, mesma origem graças ao gateway
  useEffect(() => {
    getPets().then(setPets).catch(() => setError('Failed to load pets'));
  }, []);
  useEffect(() => { load(); }, [statusFilter]);

  const handleSubmit = async (data) => {
    setError('');
    try {
      if (editing) {
        await updateAppointment(editing.id, data);
      } else {
        await createAppointment(data);
      }
      setEditing(null);
      load();
    } catch (e) {
      setError(e.message);
    }
  };

  const handleChangeStatus = async (id, status) => {
    setError('');
    try {
      await updateAppointmentStatus(id, status);
      load();
    } catch (e) {
      setError(e.message);
    }
  };

  const handleDelete = async (id) => {
    setError('');
    try {
      await deleteAppointment(id);
      load();
    } catch (e) {
      setError(e.message);
    }
  };

  return (
    <div className="page">
      <h1>Appointments</h1>
      {error && <p className="error">{error}</p>}
      <div className="filter-row">
        <label htmlFor="status-filter">Filter by status:</label>
        <select
          id="status-filter"
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
        >
          {STATUSES.map((s) => (
            <option key={s} value={s}>{s || 'All'}</option>
          ))}
        </select>
      </div>
      <AppointmentForm
        initial={editing}
        pets={pets}
        onSubmit={handleSubmit}
        onCancel={() => setEditing(null)}
      />
      <AppointmentList
        appointments={appointments}
        onEdit={setEditing}
        onChangeStatus={handleChangeStatus}
        onDelete={handleDelete}
      />
    </div>
  );
}
