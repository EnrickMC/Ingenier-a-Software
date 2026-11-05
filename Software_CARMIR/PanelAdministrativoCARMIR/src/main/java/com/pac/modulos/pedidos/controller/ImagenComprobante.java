package com.pac.modulos.pedidos.controller;

import com.pac.modulos.pedidos.model.ArchivoComprobante;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * RF-50 · Paso 3: convierte el archivo del comprobante en una imagen que el panel puede mostrar.
 * - JPG y PNG: JavaFX los lee directo.
 * - WEBP: lo lee el lector de TwelveMonkeys (ImageIO), porque JavaFX no conoce ese formato.
 * - PDF: PDFBox dibuja la primera página.
 */
public final class ImagenComprobante {

    // Tamaño máximo de la imagen en memoria (las fotos de celular son muy grandes)
    private static final double LADO_MAXIMO = 2000;
    // Resolución con la que se dibuja la página del PDF (96 = tamaño real en pantalla)
    private static final float PUNTOS_POR_PULGADA_PDF = 130f;

    // Clase de apoyo: no se crean objetos de ella
    private ImagenComprobante() {
    }

    // Devuelve la imagen del comprobante, o null si no se pudo leer
    public static Image crear(ArchivoComprobante archivo) {
        if (archivo == null || archivo.getContenido() == null) {
            return null;
        }
        try {
            if (archivo.esPdf()) {
                return primeraPaginaPdf(archivo.getContenido());
            }
            if (ArchivoComprobante.TIPO_WEBP.equals(archivo.getTipoContenido())) {
                return leerConImageIO(archivo.getContenido());
            }
            Image imagen = new Image(new ByteArrayInputStream(archivo.getContenido()), LADO_MAXIMO, LADO_MAXIMO,
                    true, true);
            if (imagen.isError()) {
                return null;
            }
            return imagen;
        } catch (IOException e) {
            return null;
        } catch (RuntimeException e) {
            // Archivo dañado o con un formato que no se reconoce
            return null;
        }
    }

    // Número de páginas del PDF (0 si no se pudo leer)
    public static int contarPaginas(ArchivoComprobante archivo) {
        if (archivo == null || !archivo.esPdf()) {
            return 0;
        }
        try {
            PDDocument documento = Loader.loadPDF(archivo.getContenido());
            int paginas = documento.getNumberOfPages();
            documento.close();
            return paginas;
        } catch (IOException e) {
            return 0;
        }
    }

    private static Image primeraPaginaPdf(byte[] contenido) throws IOException {
        PDDocument documento = Loader.loadPDF(contenido);
        try {
            if (documento.getNumberOfPages() == 0) {
                return null;
            }
            PDFRenderer dibujante = new PDFRenderer(documento);
            BufferedImage pagina = dibujante.renderImageWithDPI(0, PUNTOS_POR_PULGADA_PDF);
            return aImagenFx(pagina);
        } finally {
            documento.close();
        }
    }

    private static Image leerConImageIO(byte[] contenido) throws IOException {
        BufferedImage leida = ImageIO.read(new ByteArrayInputStream(contenido));
        if (leida == null) {
            return null;
        }
        return aImagenFx(leida);
    }

    // Pasa una imagen de Java (AWT) a una imagen de JavaFX, píxel por píxel
    private static Image aImagenFx(BufferedImage original) {
        int ancho = original.getWidth();
        int alto = original.getHeight();
        int[] pixeles = original.getRGB(0, 0, ancho, alto, null, 0, ancho);
        WritableImage resultado = new WritableImage(ancho, alto);
        resultado.getPixelWriter().setPixels(0, 0, ancho, alto, PixelFormat.getIntArgbInstance(), pixeles, 0, ancho);
        return resultado;
    }
}
