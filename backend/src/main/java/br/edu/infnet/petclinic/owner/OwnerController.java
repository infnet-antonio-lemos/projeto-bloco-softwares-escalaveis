package br.edu.infnet.petclinic.owner;

import br.edu.infnet.petclinic.owner.dto.OwnerRequest;
import br.edu.infnet.petclinic.owner.dto.OwnerResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/owners")
@RequiredArgsConstructor
public class OwnerController {

    private final OwnerService service;

    @GetMapping
    public List<OwnerResponse> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public OwnerResponse getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OwnerResponse create(@Valid @RequestBody OwnerRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public OwnerResponse update(@PathVariable Long id, @Valid @RequestBody OwnerRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
