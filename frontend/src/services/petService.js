const BASE = '/api/pets';

export async function getPets(filters = {}) {
  const params = new URLSearchParams();
  if (filters.ownerId) params.set('ownerId', filters.ownerId);
  if (filters.species) params.set('species', filters.species);
  const query = params.toString() ? `?${params}` : '';
  const res = await fetch(`${BASE}${query}`);
  if (!res.ok) throw new Error('Failed to fetch pets');
  return res.json();
}

export async function getPet(id) {
  const res = await fetch(`${BASE}/${id}`);
  if (!res.ok) throw new Error('Failed to fetch pet');
  return res.json();
}

export async function createPet(data) {
  const res = await fetch(BASE, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  if (!res.ok) throw new Error('Failed to create pet');
  return res.json();
}

export async function updatePet(id, data) {
  const res = await fetch(`${BASE}/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  if (!res.ok) throw new Error('Failed to update pet');
  return res.json();
}

export async function deletePet(id) {
  const res = await fetch(`${BASE}/${id}`, { method: 'DELETE' });
  if (!res.ok) throw new Error('Failed to delete pet');
}
