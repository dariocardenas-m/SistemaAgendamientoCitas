package com.piedrazul.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

    @GetMapping({
            "/", "/login", "/registro", "/agendar", "/mis-citas", "/agendador",
            "/reportes/citas", "/admin", "/admin/configuracion"
    })
    public String forwardToFrontend() {
        return "forward:/index.html";
    }
}
