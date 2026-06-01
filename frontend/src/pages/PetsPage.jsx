import { useState, useEffect } from 'react';
import { getOwners } from '../services/ownerService';
import { getPets, createPet, updatePet, deletePet } from '../services/petService';
import PetForm from '../components/PetForm';
import PetList from '../components/PetList';

const SPECIES = ['', 'DOG', 'CAT', 'BIRD', 'RABBIT', 'OTHER'];

export default function PetsPage() {
  const [pets, setPets] = useState([]);
  const [owners, setOwners] = useState([]);
  const [speciesFilter, setSpeciesFilter] = useState('');
  const [editing, setEditing] = useState(null);
  const [error, setError] = useState('');

  const load = () =>
    getPets(speciesFilter ? { species: speciesFilter } : {})
      .then(setPets)
      .catch(() => setError('Failed to load pets'));

  useEffect(() => { getOwners().then(setOwners).catch(() => setError('Failed to load owners')); }, []);
  useEffect(() => { load(); }, [speciesFilter]);

  const handleSubmit = async (data) => {
    try {
      if (editing) {
        await updatePet(editing.id, data);
      } else {
        await createPet(data);
      }
      setEditing(null);
      load();
    } catch {
      setError('Failed to save pet');
    }
  };

  const handleDelete = async (id) => {
    try {
      await deletePet(id);
      load();
    } catch {
      setError('Failed to delete pet');
    }
  };

  return (
    <div className="page">
      <h1>Pets</h1>
      {error && <p className="error">{error}</p>}
      <div className="filter-row">
        <label htmlFor="species-filter">Filter by species:</label>
        <select
          id="species-filter"
          value={speciesFilter}
          onChange={(e) => setSpeciesFilter(e.target.value)}
        >
          {SPECIES.map((s) => (
            <option key={s} value={s}>{s || 'All'}</option>
          ))}
        </select>
      </div>
      <PetForm
        initial={editing}
        owners={owners}
        onSubmit={handleSubmit}
        onCancel={() => setEditing(null)}
      />
      <PetList pets={pets} onEdit={setEditing} onDelete={handleDelete} />
    </div>
  );
}
