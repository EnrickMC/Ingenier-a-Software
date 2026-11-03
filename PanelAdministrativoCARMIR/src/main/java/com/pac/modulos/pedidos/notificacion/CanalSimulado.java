package com.pac.modulos.pedidos.notificacion;

/**
 * Canal de prueba: no envía nada, solo escribe el mensaje en la consola.
 * Se usa con notificaciones.modo=simulado.
 */
public class CanalSimulado implements CanalEnvio {

    private String nombre;

    public CanalSimulado(String nombre) {
        this.nombre = nombre;
    }

    @Override
    public String getNombre() {
        return nombre;
    }

    @Override
    public void enviar(String destino, String asunto, String mensaje) {
        System.out.println("[Notificación simulada · " + nombre + " a " + destino + "] " + asunto);
        System.out.println(mensaje);
    }
}
