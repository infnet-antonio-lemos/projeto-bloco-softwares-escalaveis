const BASE = '/api/owners';

export async function getOwners() {
  const res = await fetch(BASE);
  if (!res.ok) throw new Error('Failed to fetch owners');
  return res.json();
}

export async function getOwner(id) {
  const res = await fetch(`${BASE}/${id}`);
  if (!res.ok) throw new Error('Failed to fetch owner');
  return res.json();
}

export async function createOwner(data) {
  const res = await fetch(BASE, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  if (!res.ok) throw new Error('Failed to create owner');
  return res.json();
}

export async function updateOwner(id, data) {
  const res = await fetch(`${BASE}/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  if (!res.ok) throw new Error('Failed to update owner');
  return res.json();
}

export async function deleteOwner(id) {
  const res = await fetch(`${BASE}/${id}`, { method: 'DELETE' });
  if (!res.ok) throw new Error('Failed to delete owner');
}
