// Consultas são servidas pelo appointment-service. A URL é relativa, igual às demais:
// quem decide qual serviço atende /api/appointments é o api-gateway, não o frontend.
const BASE = '/api/appointments';

// Os backends respondem 404/409/503 com o motivo em texto puro. Propagar essa
// mensagem em vez de descartá-la é o que faz a UI dizer "veterinário já ocupado"
// ou "cadastro de pets indisponível" em vez de um erro genérico.
async function check(res, fallback) {
  if (!res.ok) {
    const detail = await res.text().catch(() => '');
    throw new Error(detail || fallback);
  }
  return res;
}

export async function getAppointments(filters = {}) {
  const params = new URLSearchParams();
  if (filters.petId) params.set('petId', filters.petId);
  if (filters.ownerId) params.set('ownerId', filters.ownerId);
  if (filters.status) params.set('status', filters.status);
  const query = params.toString() ? `?${params}` : '';
  const res = await fetch(`${BASE}${query}`);
  await check(res, 'Failed to fetch appointments');
  return res.json();
}

export async function getAppointment(id) {
  const res = await fetch(`${BASE}/${id}`);
  await check(res, 'Failed to fetch appointment');
  return res.json();
}

export async function createAppointment(data) {
  const res = await fetch(BASE, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  await check(res, 'Failed to create appointment');
  return res.json();
}

export async function updateAppointment(id, data) {
  const res = await fetch(`${BASE}/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  await check(res, 'Failed to update appointment');
  return res.json();
}

export async function updateAppointmentStatus(id, status) {
  const res = await fetch(`${BASE}/${id}/status`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ status }),
  });
  await check(res, 'Failed to update appointment status');
  return res.json();
}

export async function deleteAppointment(id) {
  const res = await fetch(`${BASE}/${id}`, { method: 'DELETE' });
  await check(res, 'Failed to delete appointment');
}
