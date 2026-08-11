const formatDateTime = (value) =>
  value ? value.replace('T', ' ').slice(0, 16) : '';

export default function AppointmentList({ appointments, onEdit, onChangeStatus, onDelete }) {
  if (appointments.length === 0) return <p>No appointments scheduled.</p>;

  return (
    <table>
      <thead>
        <tr>
          <th>Date / Time</th>
          <th>Pet</th>
          <th>Owner</th>
          <th>Veterinarian</th>
          <th>Reason</th>
          <th>Status</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        {appointments.map((a) => (
          <tr key={a.id}>
            <td>{formatDateTime(a.scheduledAt)}</td>
            {/* petName/ownerName vêm do snapshot gravado pelo microsserviço no
                momento do agendamento, obtido do cadastro de pets via Feign */}
            <td>{a.petName}</td>
            <td>{a.ownerName}</td>
            <td>{a.veterinarian}</td>
            <td>{a.reason}</td>
            <td>
              <span className={`status-badge status-${a.status.toLowerCase()}`}>{a.status}</span>
            </td>
            <td>
              <button onClick={() => onEdit(a)}>Edit</button>
              {a.status === 'SCHEDULED' && (
                <>
                  <button onClick={() => onChangeStatus(a.id, 'COMPLETED')}>Complete</button>
                  <button onClick={() => onChangeStatus(a.id, 'CANCELLED')}>Cancel</button>
                </>
              )}
              <button onClick={() => onDelete(a.id)}>Delete</button>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
