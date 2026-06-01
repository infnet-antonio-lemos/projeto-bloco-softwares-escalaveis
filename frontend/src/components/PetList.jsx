export default function PetList({ pets, onEdit, onDelete }) {
  if (pets.length === 0) return <p>No pets registered.</p>;

  return (
    <table>
      <thead>
        <tr>
          <th>Name</th>
          <th>Species</th>
          <th>Breed</th>
          <th>Birth Date</th>
          <th>Owner</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        {pets.map((p) => (
          <tr key={p.id}>
            <td>{p.name}</td>
            <td>{p.species}</td>
            <td>{p.breed}</td>
            <td>{p.birthDate}</td>
            <td>{p.ownerName}</td>
            <td>
              <button onClick={() => onEdit(p)}>Edit</button>
              <button onClick={() => onDelete(p.id)}>Delete</button>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
