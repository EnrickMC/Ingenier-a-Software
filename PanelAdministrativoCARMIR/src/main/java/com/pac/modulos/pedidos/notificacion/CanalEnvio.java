package com.pac.modulos.pedidos.notificacion;

/**
 * PATRÓN STRATEGY.
 * Cada forma de enviar un aviso (WhatsApp, correo o simulado) implementa esta interfaz;
 * el servicio de notificaciones no necesita saber cuál está usando.
 */
public interface CanalEnvio {

    // Nombre del canal tal como se guarda en Notificacion.Canal ("WhatsApp" o "Correo")
    String getNombre();

    // Envía el mensaje; si falla lanza ErrorEnvio con el motivo
    void enviar(String destino, String asunto, String mensaje) throws ErrorEnvio;
}
