package br.edu.infnet.petclinic.pet;

import br.edu.infnet.petclinic.pet.dto.PetRequest;
import br.edu.infnet.petclinic.pet.dto.PetResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pets")
@RequiredArgsConstructor
public class PetController {

    private final PetService service;

    @GetMapping
    public List<PetResponse> getAll(
            @RequestParam(required = false) Long ownerId,
            @RequestParam(required = false) Species species) {
        return service.findAll(ownerId, species);
    }

    @GetMapping("/{id}")
    public PetResponse getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PetResponse create(@Valid @RequestBody PetRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public PetResponse update(@PathVariable Long id, @Valid @RequestBody PetRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
