package ar.com.leo.super_master_backend.dominio.common.util;

/**
 * Utilidad para formatear y describir valores de cuotas.
 *
 * Convención de valores:
 * - cuotas = -1 → Transferencia (descuento)
 * - cuotas = 0  → Contado
 * - cuotas > 0  → X cuotas (ej: 3 cuotas, 6 cuotas)
 */
public final class CuotasUtil {

    public static final int TRANSFERENCIA = -1;
    public static final int CONTADO = 0;

    private CuotasUtil() {
        // Utility class
    }

    /**
     * Convierte el valor de cuotas a una descripción legible.
     *
     * @param cuotas Valor de cuotas (puede ser null)
     * @return Descripción: "transferencia", "contado", o "X cuotas"
     */
    public static String describir(Integer cuotas) {
        if (cuotas == null) {
            return "contado";
        }
        return switch (cuotas) {
            case TRANSFERENCIA -> "transferencia";
            case CONTADO -> "contado";
            default -> cuotas + " cuotas";
        };
    }

    /**
     * Convierte el valor de cuotas a una descripción con preposición.
     *
     * @param cuotas Valor de cuotas (puede ser null)
     * @return Descripción con preposición: "por transferencia", "de contado", o "en X cuotas"
     */
    public static String describirConPreposicion(Integer cuotas) {
        if (cuotas == null) {
            return "de contado";
        }
        return switch (cuotas) {
            case TRANSFERENCIA -> "por transferencia";
            case CONTADO -> "de contado";
            default -> "en " + cuotas + " cuotas";
        };
    }

    /**
     * Verifica si el valor representa transferencia.
     */
    public static boolean esTransferencia(Integer cuotas) {
        return cuotas != null && cuotas == TRANSFERENCIA;
    }

    /**
     * Verifica si el valor representa contado.
     */
    public static boolean esContado(Integer cuotas) {
        return cuotas == null || cuotas == CONTADO;
    }

    /**
     * Verifica si el valor representa cuotas (mayor a 0).
     */
    public static boolean esCuotas(Integer cuotas) {
        return cuotas != null && cuotas > 0;
    }
}
