package com.techmentor.api.controller;
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
    public ResponseEntity<List<Usuario>> listarUsuarios(){
        List<Usuario> usuarios = service.listarUsuarios();
        return ResponseEntity.ok(usuarios);
    }

    @PostMapping()
    public ResponseEntity<String> cadastrar(@RequestBody @Validated Usuario usuario){
        this.service.criarUsuario(usuario);
        return ResponseEntity.status(201).build();
    }


}
