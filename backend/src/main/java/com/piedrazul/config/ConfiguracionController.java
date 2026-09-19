package com.piedrazul.config;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/configuracion")
public class ConfiguracionController {

    private final ConfiguracionGlobalRepository repository;

    public ConfiguracionController(ConfiguracionGlobalRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public Map<String, Integer> obtener() {
        ConfiguracionGlobal cfg = repository.findAll().stream().findFirst()
                .orElseGet(() -> {
                    ConfiguracionGlobal n = new ConfiguracionGlobal();
                    n.setVentanaSemanas(4);
                    return repository.save(n);
                });
        return Map.of("ventanaSemanas", cfg.getVentanaSemanas());
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Map<String, Integer> actualizar(@RequestBody Map<String, Integer> body) {
        Integer semanas = body.get("ventanaSemanas");
        if (semanas == null || semanas < 1 || semanas > 12) {
            throw new ReglaNegocioException("La ventana de tiempo debe estar entre 1 y 12 semanas.");
        }
        ConfiguracionGlobal cfg = repository.findAll().stream().findFirst().orElseGet(ConfiguracionGlobal::new);
        cfg.setVentanaSemanas(semanas);
        repository.save(cfg);
        return Map.of("ventanaSemanas", cfg.getVentanaSemanas());
    }
}
