package com.pac.modulos.usuarios.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa un registro de la tabla Usuario.
 * Es una persona del negocio que entra al panel (Administradora o Personal).
 */
public class Usuario {

    private int idUsuario;
    private String nombreCompleto;
    private String nombreUsuario;
    private String contrasenaHash;       // La contraseña cifrada, nunca en texto plano
    private EstadoUsuario estado;
    private LocalDateTime ultimoAcceso;
    private int intentosFallidos;
    private List<Permiso> permisos;

    // Constructor vacío
    public Usuario() {
        this.estado = EstadoUsuario.ACTIVO;
        this.permisos = new ArrayList<Permiso>();
    }

    // Constructor con los datos básicos
    public Usuario(int idUsuario, String nombreCompleto, String nombreUsuario) {
        this();
        this.idUsuario = idUsuario;
        this.nombreCompleto = nombreCompleto;
        this.nombreUsuario = nombreUsuario;
    }

    // Revisa si el usuario tiene un permiso con ese nombre
    public boolean tienePermiso(String nombrePermiso) {
        for (Permiso permiso : permisos) {
            if (permiso.getNombrePermiso().equalsIgnoreCase(nombrePermiso)) {
                return true;
            }
        }
        return false;
    }

    // Agrega un permiso a la lista del usuario
    public void agregarPermiso(Permiso permiso) {
        permisos.add(permiso);
    }

    // Reemplaza todos los permisos del usuario por los de la lista nueva
    public void reemplazarPermisos(List<Permiso> permisosNuevos) {
        permisos.clear();
        for (Permiso permiso : permisosNuevos) {
            permisos.add(permiso);
        }
    }

    // Devuelve los permisos separados por coma, por ejemplo "Gestionar pedidos, Gestionar recetas"
    public String getTextoPermisos() {
        if (permisos.isEmpty()) {
            return "Sin permisos";
        }
        String texto = "";
        for (int i = 0; i < permisos.size(); i++) {
            if (i > 0) {
                texto = texto + ", ";
            }
            texto = texto + permisos.get(i).getNombrePermiso();
        }
        return texto;
    }

    // ----- Getters y setters -----

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public String getContrasenaHash() {
        return contrasenaHash;
    }

    public void setContrasenaHash(String contrasenaHash) {
        this.contrasenaHash = contrasenaHash;
    }

    public EstadoUsuario getEstado() {
        return estado;
    }

    public void setEstado(EstadoUsuario estado) {
        this.estado = estado;
    }

    public LocalDateTime getUltimoAcceso() {
        return ultimoAcceso;
    }

    public void setUltimoAcceso(LocalDateTime ultimoAcceso) {
        this.ultimoAcceso = ultimoAcceso;
    }

    public int getIntentosFallidos() {
        return intentosFallidos;
    }

    public void setIntentosFallidos(int intentosFallidos) {
        this.intentosFallidos = intentosFallidos;
    }

    public List<Permiso> getPermisos() {
        return permisos;
    }

    @Override
    public String toString() {
        return nombreCompleto + " (" + nombreUsuario + ")";
    }
}
