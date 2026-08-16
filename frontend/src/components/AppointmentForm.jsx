import { useState, useEffect } from 'react';

const EMPTY = { petId: '', scheduledAt: '', veterinarian: '', reason: '', notes: '' };

// O backend usa LocalDateTime (sem timezone) e o <input type="datetime-local">
// trabalha com 'YYYY-MM-DDTHH:mm' — recortar os segundos alinha os dois formatos.
const toInputValue = (isoDateTime) => (isoDateTime ? isoDateTime.slice(0, 16) : '');

// Espelha o @Future do AppointmentRequest no próprio seletor de data: sem isto o
// browser aceita qualquer data e a regra só se manifesta como 400 depois do submit.
// toISOString() é UTC, então descontamos o offset para obter o horário local.
const nowAsInputValue = () => {
  const now = new Date();
  now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
  return now.toISOString().slice(0, 16);
};

export default function AppointmentForm({ initial, pets, onSubmit, onCancel }) {
  const [form, setForm] = useState(EMPTY);

  useEffect(() => {
    setForm(
      initial
        ? {
            petId: initial.petId,
            scheduledAt: toInputValue(initial.scheduledAt),
            veterinarian: initial.veterinarian,
            reason: initial.reason || '',
            notes: initial.notes || '',
          }
        : EMPTY
    );
  }, [initial]);

  const handle = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const submit = (e) => {
    e.preventDefault();
    // Só petId é enviado: o serviço resolve tutor e nomes consultando o cadastro de pets.
    onSubmit({ ...form, petId: Number(form.petId) });
  };

  return (
    <form className="form" onSubmit={submit}>
      <h2>{initial ? 'Edit Appointment' : 'New Appointment'}</h2>
      <div className="form-row">
        <label>Pet</label>
        <select name="petId" value={form.petId} onChange={handle} required>
          <option value="">-- select --</option>
          {pets.map((p) => (
            <option key={p.id} value={p.id}>{p.name} ({p.ownerName})</option>
          ))}
        </select>
      </div>
      <div className="form-row">
        <label>Date / Time</label>
        <input
          name="scheduledAt"
          type="datetime-local"
          min={nowAsInputValue()}
          value={form.scheduledAt}
          onChange={handle}
          required
        />
      </div>
      <div className="form-row">
        <label>Veterinarian</label>
        <input name="veterinarian" value={form.veterinarian} onChange={handle} required />
      </div>
      <div className="form-row">
        <label>Reason</label>
        <input name="reason" value={form.reason} onChange={handle} />
      </div>
      <div className="form-row">
        <label>Notes</label>
        <input name="notes" value={form.notes} onChange={handle} maxLength={1000} />
      </div>
      <div className="form-actions">
        <button type="submit">{initial ? 'Update' : 'Create'}</button>
        {initial && <button type="button" onClick={onCancel}>Cancel</button>}
      </div>
    </form>
  );
}
