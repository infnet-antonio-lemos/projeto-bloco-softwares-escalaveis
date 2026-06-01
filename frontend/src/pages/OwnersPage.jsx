import { useState, useEffect } from 'react';
import { getOwners, createOwner, updateOwner, deleteOwner } from '../services/ownerService';
import OwnerForm from '../components/OwnerForm';
import OwnerList from '../components/OwnerList';

export default function OwnersPage() {
  const [owners, setOwners] = useState([]);
  const [editing, setEditing] = useState(null);
  const [error, setError] = useState('');

  const load = () => getOwners().then(setOwners).catch(() => setError('Failed to load owners'));

  useEffect(() => { load(); }, []);

  const handleSubmit = async (data) => {
    try {
      if (editing) {
        await updateOwner(editing.id, data);
      } else {
        await createOwner(data);
      }
      setEditing(null);
      load();
    } catch {
      setError('Failed to save owner');
    }
  };

  const handleDelete = async (id) => {
    try {
      await deleteOwner(id);
      load();
    } catch {
      setError('Failed to delete owner');
    }
  };

  return (
    <div className="page">
      <h1>Owners</h1>
      {error && <p className="error">{error}</p>}
      <OwnerForm
        initial={editing}
        onSubmit={handleSubmit}
        onCancel={() => setEditing(null)}
      />
      <OwnerList owners={owners} onEdit={setEditing} onDelete={handleDelete} />
    </div>
  );
}
