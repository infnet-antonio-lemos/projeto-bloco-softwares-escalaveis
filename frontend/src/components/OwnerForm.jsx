import { useState } from 'react';

const EMPTY = { name: '', email: '', phone: '', address: '' };

const formFor = (owner) =>
  owner
    ? { name: owner.name, email: owner.email, phone: owner.phone || '', address: owner.address || '' }
    : EMPTY;

// O estado inicial vem de `initial` uma única vez. Trocar de registro remonta o
// componente, porque a página passa `key={initial?.id ?? 'new'}` — é o mecanismo do
// React para "resetar estado quando a identidade muda". A alternativa (useEffect
// chamando setForm) renderiza duas vezes a cada troca e descarta o que o usuário
// tiver digitado se `initial` mudar de referência por qualquer outro motivo.
export default function OwnerForm({ initial, onSubmit, onCancel }) {
  const [form, setForm] = useState(() => formFor(initial));

  const handle = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const submit = (e) => {
    e.preventDefault();
    onSubmit(form);
  };

  return (
    <form className="form" onSubmit={submit}>
      <h2>{initial ? 'Edit Owner' : 'New Owner'}</h2>
      <div className="form-row">
        <label>Name</label>
        <input name="name" value={form.name} onChange={handle} required />
      </div>
      <div className="form-row">
        <label>Email</label>
        <input name="email" type="email" value={form.email} onChange={handle} required />
      </div>
      <div className="form-row">
        <label>Phone</label>
        <input name="phone" value={form.phone} onChange={handle} />
      </div>
      <div className="form-row">
        <label>Address</label>
        <input name="address" value={form.address} onChange={handle} />
      </div>
      <div className="form-actions">
        <button type="submit">{initial ? 'Update' : 'Create'}</button>
        {initial && <button type="button" onClick={onCancel}>Cancel</button>}
      </div>
    </form>
  );
}
