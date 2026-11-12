package ar.edu.universidad.gestionacademica.Seguridad;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Traduce el JWT de CORE a autoridades de Spring:
 * <ul>
 *   <li>cada rol de {@code roles} queda con y sin prefijo ROLE_ (ACADEMIC_ADMIN, ROLE_ACADEMIC_ADMIN);</li>
 *   <li>cada permiso de {@code permissions}, {@code scp} o {@code scope} queda tal cual (academic:manage);</li>
 *   <li>un token de servicio (claim {@code service}) suma SERVICE y SERVICE:&lt;nombre&gt;.</li>
 * </ul>
 * El nombre del principal es el {@code sub}.
 */
public class ConvertidorJwtCore implements Converter<Jwt, AbstractAuthenticationToken> {
    public static final String SERVICIO = "SERVICE";

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Set<String> nombres = new LinkedHashSet<>();
        for (String rol : valores(jwt, "roles")) {
            String limpio = rol.startsWith("ROLE_") ? rol.substring(5) : rol;
            nombres.add(limpio);
            nombres.add("ROLE_" + limpio);
        }
        nombres.addAll(valores(jwt, "permissions"));
        nombres.addAll(valores(jwt, "scp"));
        nombres.addAll(valores(jwt, "scope"));
        String servicio = jwt.getClaimAsString("service");
        if (servicio != null && !servicio.isBlank()) {
            nombres.add(SERVICIO);
            nombres.add(SERVICIO + ":" + servicio);
        }
        List<GrantedAuthority> autoridades = nombres.stream()
                .<GrantedAuthority>map(SimpleGrantedAuthority::new).toList();
        return new JwtAuthenticationToken(jwt, autoridades, jwt.getSubject());
    }

    /** Acepta el claim como lista o como texto separado por espacios o comas. */
    private static List<String> valores(Jwt jwt, String claim) {
        Object valor = jwt.getClaims().get(claim);
        List<String> resultado = new ArrayList<>();
        if (valor instanceof Collection<?> lista) {
            lista.stream().filter(item -> item != null && !item.toString().isBlank())
                    .forEach(item -> resultado.add(item.toString().trim()));
        } else if (valor != null) {
            for (String parte : valor.toString().split("[\\s,]+")) {
                if (!parte.isBlank()) resultado.add(parte.trim());
            }
        }
        return resultado;
    }
}
