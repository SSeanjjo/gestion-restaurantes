package co.edu.uniquindio.comanda.caja.dian;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;

/**
 * Cálculo del CUFE (Código Único de Factura Electrónica) según el esquema del Anexo Técnico
 * de la DIAN: SHA-384 de la concatenación de número, fecha, hora, valores e impuestos, NIT del
 * emisor, documento del adquiriente, clave técnica y tipo de ambiente.
 *
 * Versión simplificada con fines académicos (un solo impuesto, consumidor final).
 */
public final class CalculadoraCufe {

    /** Documento de "consumidor final" usado en ventas de mostrador. */
    public static final String CONSUMIDOR_FINAL = "222222222222";
    /** Código de tributo del impuesto nacional al consumo (INC). */
    public static final String CODIGO_INC = "04";

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    private CalculadoraCufe() {
    }

    public static String calcular(String numero, LocalDateTime emision, BigDecimal base, BigDecimal impuesto,
            BigDecimal total, String nitEmisor, String claveTecnica, int ambiente) {
        String cadena = numero
                + emision.format(FECHA)
                + emision.format(HORA) + "-05:00"
                + dosDecimales(base)
                + CODIGO_INC + dosDecimales(impuesto)
                + dosDecimales(total)
                + nitEmisor
                + CONSUMIDOR_FINAL
                + claveTecnica
                + ambiente;
        return sha384(cadena);
    }

    private static String dosDecimales(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String sha384(String texto) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-384").digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-384 no disponible", e);
        }
    }
}
