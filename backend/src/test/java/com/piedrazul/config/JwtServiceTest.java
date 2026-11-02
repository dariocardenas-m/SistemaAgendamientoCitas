package com.piedrazul.config;

import com.piedrazul.usuarios.Rol;
import io.jsonwebtoken.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
    private static final String TEST_KEY="clave-ficticia-exclusiva-para-pruebas-de-32-bytes-o-mas";
    @Test void tokenFirmadoConservaIdentidadYRol(){
        JwtService s=new JwtService(TEST_KEY,60000);String t=s.generarToken(4L,"persona@example.test",Rol.PACIENTE);
        Claims c=s.parsear(t);assertEquals("persona@example.test",c.getSubject());assertEquals(4L,((Number)c.get("uid")).longValue());assertEquals("PACIENTE",c.get("rol"));assertTrue(c.getExpiration().after(c.getIssuedAt()));
    }
    @Test void otraClaveNoPuedeValidarElToken(){
        JwtService emisor=new JwtService(TEST_KEY,60000), receptor=new JwtService(TEST_KEY+"otra",60000);
        assertThrows(JwtException.class,()->receptor.parsear(emisor.generarToken(4L,"persona@example.test",Rol.PACIENTE)));
    }
    @Test void tokenYaVencidoSeRechazaSinEsperar(){JwtService s=new JwtService(TEST_KEY,-60000);assertThrows(ExpiredJwtException.class,()->s.parsear(s.generarToken(4L,"persona@example.test",Rol.PACIENTE)));}
    @Test void textoSinFormatoJwtSeRechaza(){assertThrows(JwtException.class,()->new JwtService(TEST_KEY,60000).parsear("no-es-un-jwt"));}
}
