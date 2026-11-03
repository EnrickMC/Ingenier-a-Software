package com.pac.modulos.usuarios.controller;

import com.pac.modulos.usuarios.model.EstadoUsuario;
import com.pac.modulos.usuarios.model.Usuario;
import com.pac.util.Formatos;

/**
 * Una fila de la tabla de usuarios.
 * Prepara los textos tal como se muestran en cada columna.
 * La tabla lee estos getters por su nombre (PropertyValueFactory en usuarios.fxml).
 * Nunca incluye la contraseña.
 */
public class FilaUsuario {

    private Usuario usuario;

    public FilaUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    // Columna "Nombre completo"
    public String getNombreCompleto() {
        return usuario.getNombreCompleto();
    }

    // Columna "Usuario"
    public String getNombreUsuario() {
        return usuario.getNombreUsuario();
    }

    // Columna "Estado". Si a un usuario activo le faltan datos, se marca "Revisar datos" (RNF-13)
    public String getEstado() {
        String texto = usuario.getEstado().getValorBD();
        if (necesitaRevision()) {
            texto = texto + " · Revisar datos";
        }
        return texto;
    }

    // Columna "Permisos"
    public String getPermisos() {
        return usuario.getTextoPermisos();
    }

    // Columna "Último acceso"
    public String getUltimoAcceso() {
        return Formatos.fechaHoraCorta(usuario.getUltimoAcceso());
    }

    // Un usuario activo sin nombre o sin permisos tiene datos incompletos
    public boolean necesitaRevision() {
        if (usuario.getEstado() != EstadoUsuario.ACTIVO) {
            return false;
        }
        if (usuario.getNombreCompleto() == null || usuario.getNombreCompleto().isBlank()) {
            return true;
        }
        return usuario.getPermisos().isEmpty();
    }
}
