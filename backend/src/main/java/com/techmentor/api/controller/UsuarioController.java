package com.techmentor.api.controller;
import com.techmentor.api.dto.UsuarioResponseDTO;
import com.techmentor.api.model.Usuario;
import com.techmentor.api.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping()
    public ResponseEntity<List<UsuarioResponseDTO>> listarUsuarios(){
        return ResponseEntity.ok(service.listarUsuarios());
    }

    @PostMapping()
    public ResponseEntity<String> cadastrar(@RequestBody @Validated Usuario usuario){
        this.service.criarUsuario(usuario);
        return ResponseEntity.status(201).build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id){
        this.service.deletarUsuario(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> atualizar(@PathVariable Long id, @RequestBody Usuario usuario){
        this.service.atualizarUsuario(id, usuario);
        return ResponseEntity.ok().build();
    }
}
