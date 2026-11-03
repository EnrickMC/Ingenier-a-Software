package com.pac.modulos.usuarios.model;

/**
 * Representa un registro de la tabla Permiso.
 * Es una acción del sistema que se le puede dar a un usuario, por ejemplo "Validar pagos".
 */
public class Permiso {

    private int idPermiso;
    private String nombrePermiso;

    // Constructor vacío
    public Permiso() {
    }

    // Constructor con todos los datos
    public Permiso(int idPermiso, String nombrePermiso) {
        this.idPermiso = idPermiso;
        this.nombrePermiso = nombrePermiso;
    }

    // ----- Getters y setters -----

    public int getIdPermiso() {
        return idPermiso;
    }

    public void setIdPermiso(int idPermiso) {
        this.idPermiso = idPermiso;
    }

    public String getNombrePermiso() {
        return nombrePermiso;
    }

    public void setNombrePermiso(String nombrePermiso) {
        this.nombrePermiso = nombrePermiso;
    }

    @Override
    public String toString() {
        return nombrePermiso;
    }
}
