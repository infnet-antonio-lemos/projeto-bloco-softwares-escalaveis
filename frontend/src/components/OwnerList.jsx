export default function OwnerList({ owners, onEdit, onDelete }) {
  if (owners.length === 0) return <p>No owners registered.</p>;

  return (
    <table>
      <thead>
        <tr>
          <th>Name</th>
          <th>Email</th>
          <th>Phone</th>
          <th>Address</th>
          <th>Pets</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        {owners.map((o) => (
          <tr key={o.id}>
            <td>{o.name}</td>
            <td>{o.email}</td>
            <td>{o.phone}</td>
            <td>{o.address}</td>
            <td>{o.petCount}</td>
            <td>
              <button onClick={() => onEdit(o)}>Edit</button>
              <button onClick={() => onDelete(o.id)}>Delete</button>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
