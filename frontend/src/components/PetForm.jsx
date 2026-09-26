import { useState } from 'react';

const SPECIES = ['DOG', 'CAT', 'BIRD', 'RABBIT', 'OTHER'];
const EMPTY = { name: '', species: 'DOG', breed: '', birthDate: '', ownerId: '' };

const formFor = (pet) =>
  pet
    ? {
        name: pet.name,
        species: pet.species,
        breed: pet.breed || '',
        birthDate: pet.birthDate || '',
        ownerId: pet.ownerId,
      }
    : EMPTY;

// Estado inicializado uma vez; a troca de registro remonta via `key` na página.
export default function PetForm({ initial, owners, onSubmit, onCancel }) {
  const [form, setForm] = useState(() => formFor(initial));

  const handle = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const submit = (e) => {
    e.preventDefault();
    onSubmit({ ...form, ownerId: Number(form.ownerId) });
  };

  return (
    <form className="form" onSubmit={submit}>
      <h2>{initial ? 'Edit Pet' : 'New Pet'}</h2>
      <div className="form-row">
        <label>Name</label>
        <input name="name" value={form.name} onChange={handle} required />
      </div>
      <div className="form-row">
        <label>Species</label>
        <select name="species" value={form.species} onChange={handle} required>
          {SPECIES.map((s) => <option key={s} value={s}>{s}</option>)}
        </select>
      </div>
      <div className="form-row">
        <label>Breed</label>
        <input name="breed" value={form.breed} onChange={handle} />
      </div>
      <div className="form-row">
        <label>Birth Date</label>
        <input name="birthDate" type="date" value={form.birthDate} onChange={handle} />
      </div>
      <div className="form-row">
        <label>Owner</label>
        <select name="ownerId" value={form.ownerId} onChange={handle} required>
          <option value="">-- select --</option>
          {owners.map((o) => <option key={o.id} value={o.id}>{o.name}</option>)}
        </select>
      </div>
      <div className="form-actions">
        <button type="submit">{initial ? 'Update' : 'Create'}</button>
        {initial && <button type="button" onClick={onCancel}>Cancel</button>}
      </div>
    </form>
  );
}
