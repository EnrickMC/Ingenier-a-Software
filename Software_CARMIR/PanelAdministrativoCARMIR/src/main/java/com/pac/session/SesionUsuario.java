package com.pac.session;

import com.pac.modulos.usuarios.model.Usuario;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * PATRÓN SINGLETON + PATRÓN OBSERVER (sujeto).
 * Guarda en memoria quién tiene la sesión abierta (la base de datos no guarda sesiones)
 * y avisa a los observadores cuando la sesión empieza o termina.
 */
public class SesionUsuario {

    // La única instancia de la clase
    private static SesionUsuario instancia;

    private List<SesionObserver> observadores;
    private Usuario usuario;
    private TipoAcceso tipoAcceso;
    private LocalDateTime horaInicio;

    // Constructor privado
    private SesionUsuario() {
        observadores = new ArrayList<SesionObserver>();
    }

    // Devuelve la única instancia; la crea la primera vez que se pide
    public static synchronized SesionUsuario getInstancia() {
        if (instancia == null) {
            instancia = new SesionUsuario();
        }
        return instancia;
    }

    // ----- Manejo de observadores -----

    public void agregarObservador(SesionObserver observador) {
        if (!observadores.contains(observador)) {
            observadores.add(observador);
        }
    }

    public void quitarObservador(SesionObserver observador) {
        observadores.remove(observador);
    }

    // ----- Inicio y cierre de sesión -----

    // Guarda los datos del usuario y avisa a los observadores
    public void iniciar(Usuario usuarioQueEntra, TipoAcceso tipo) {
        usuario = usuarioQueEntra;
        tipoAcceso = tipo;
        horaInicio = LocalDateTime.now();

        // Se copia la lista para que un observador pueda quitarse sin causar errores
        List<SesionObserver> copia = new ArrayList<SesionObserver>(observadores);
        for (SesionObserver observador : copia) {
            observador.sesionIniciada(usuario, tipoAcceso);
        }
    }

    // Borra los datos de la sesión y avisa a los observadores
    public void cerrar() {
        usuario = null;
        tipoAcceso = null;
        horaInicio = null;

        List<SesionObserver> copia = new ArrayList<SesionObserver>(observadores);
        for (SesionObserver observador : copia) {
            observador.sesionCerrada();
        }
    }

    // ----- Consultas -----

    public boolean estaActiva() {
        return usuario != null;
    }

    // Devuelve el usuario de la sesión, o null si nadie ha entrado
    public Usuario getUsuario() {
        return usuario;
    }

    public TipoAcceso getTipoAcceso() {
        return tipoAcceso;
    }

    public LocalDateTime getHoraInicio() {
        return horaInicio;
    }

    public boolean esAdministradora() {
        return tipoAcceso == TipoAcceso.ADMINISTRADORA;
    }

    // La Administradora puede entrar a todo; el Personal solo a lo que tenga permitido
    public boolean tienePermiso(String nombrePermiso) {
        if (!estaActiva()) {
            return false;
        }
        if (nombrePermiso == null) {
            return true;
        }
        if (esAdministradora()) {
            return true;
        }
        return usuario.tienePermiso(nombrePermiso);
    }
}
