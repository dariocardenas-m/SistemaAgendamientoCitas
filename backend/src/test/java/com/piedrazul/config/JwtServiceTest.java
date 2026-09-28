package com.piedrazul.config;
import com.piedrazul.usuarios.Rol;
import io.jsonwebtoken.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class JwtServiceTest {
    static final String KEY="clave-exclusiva-de-tests-0123456789-ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    @Test void tokenConservaIdentidadRolYExpiracion() {
        JwtService s=new JwtService(KEY,60000); var c=s.parsear(s.generarToken(9L,"test@example.test",Rol.PACIENTE));
        assertThat(c.getSubject()).isEqualTo("test@example.test"); assertThat(c.get("uid",Long.class)).isEqualTo(9);
        assertThat(c.get("rol")).isEqualTo("PACIENTE"); assertThat(c.getExpiration()).isAfter(c.getIssuedAt());
    }
    @Test void rechazaFirmaDeOtraClave() {
        String token=new JwtService(KEY,60000).generarToken(1L,"u@example.test",Rol.PACIENTE);
        assertThatThrownBy(()->new JwtService(KEY+"otra",60000).parsear(token)).isInstanceOf(JwtException.class);
    }
    @Test void rechazaTokenExpiradoSinDormir() {
        JwtService s=new JwtService(KEY,-60000);
        assertThatThrownBy(()->s.parsear(s.generarToken(1L,"u@example.test",Rol.PACIENTE))).isInstanceOf(ExpiredJwtException.class);
    }
    @Test void rechazaTextoSinFormatoJwt() { assertThatThrownBy(()->new JwtService(KEY,60000).parsear("texto")).isInstanceOf(JwtException.class); }
}
