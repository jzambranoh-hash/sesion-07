package com.nttdata.ms_tickets.validacion;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Verificaciones automaticas del taller: 20 puntos. (Resuelto: no modificar)
 *
 * Trabaja por reflexion sobre el codigo GENERADO y sobre tu implementacion,
 * por lo que refleja el efecto real del contrato. Ejecutalo despues de
 * "mvn clean install" y de recargar Maven.
 */
public class Validador {

    private static final String PKG = "com.nttdata.ms_tickets";
    private static final Map<String, int[]> RESUMEN = new LinkedHashMap<>();
    private static int numero = 0;

    @FunctionalInterface
    interface Prueba { boolean ejecutar() throws Exception; }

    public static void main(String[] args) {
        System.out.println("HelpDesk - Validacion automatica del taller Open API");
        System.out.println("===================================================");

        // =================== CONTRATO: endpoints (4) ===================
        verificar("Contrato: endpoints", "El contrato genera las 3 operaciones en TicketsApiDelegate",
                () -> {
                    Class<?> api = clase(PKG + ".api.TicketsApiDelegate");
                    if (api == null) return false;
                    List<String> m = Arrays.stream(api.getMethods()).map(Method::getName).toList();
                    return m.contains("listarTickets") && m.contains("obtenerTicket") && m.contains("crearTicket");
                });

        verificar("Contrato: endpoints", "listarTickets recibe el parametro de consulta estado (RN-05)",
                () -> {
                    Method m = metodo("listarTickets");
                    return m != null && m.getParameterCount() == 1 && m.getParameterTypes()[0] == String.class;
                });

        verificar("Contrato: endpoints", "obtenerTicket recibe un Long (parametro de ruta id)",
                () -> {
                    Method m = metodo("obtenerTicket");
                    return m != null && m.getParameterCount() == 1 && m.getParameterTypes()[0] == Long.class;
                });

        verificar("Contrato: endpoints", "crearTicket recibe un TicketRequest (requestBody del POST)",
                () -> {
                    Method m = metodo("crearTicket");
                    return m != null && m.getParameterCount() == 1
                            && m.getParameterTypes()[0].getName().equals(PKG + ".model.TicketRequest");
                });

        // =================== CONTRATO: modelos (3) ===================
        verificar("Contrato: modelos", "El modelo Ticket tiene id, titulo, prioridad, estado y horasEstimadas",
                () -> {
                    Class<?> c = clase(PKG + ".model.Ticket");
                    return c != null && tieneGetters(c, "getId", "getTitulo", "getPrioridad", "getEstado", "getHorasEstimadas");
                });

        verificar("Contrato: modelos", "prioridad es un enum con ALTA, MEDIA y BAJA (RN-02)",
                () -> constantes(clase(PKG + ".model.Ticket"), "getPrioridad").containsAll(List.of("ALTA", "MEDIA", "BAJA")));

        verificar("Contrato: modelos", "estado es un enum con ABIERTO, EN_PROGRESO y CERRADO",
                () -> constantes(clase(PKG + ".model.Ticket"), "getEstado").containsAll(List.of("ABIERTO", "EN_PROGRESO", "CERRADO")));

        // =================== CONTRATO: validaciones y errores (3) ===================
        verificar("Contrato: validaciones", "El modelo TicketRequest tiene titulo y prioridad",
                () -> {
                    Class<?> c = clase(PKG + ".model.TicketRequest");
                    return c != null && tieneGetters(c, "getTitulo", "getPrioridad");
                });

        verificar("Contrato: validaciones", "api.yaml declara las respuestas '404' y '400'",
                () -> {
                    String y = contrato();
                    return y.contains("'404'") && y.contains("'400'");
                });

        verificar("Contrato: validaciones", "api.yaml declara required y los limites del titulo (RN-01)",
                () -> {
                    String y = contrato().replace(" ", "");
                    return y.contains("required:") && y.contains("minLength:5") && y.contains("maxLength:100");
                });

        // =================== IMPLEMENTACION: reglas de negocio (6) ===================
        verificar("Implementacion: reglas", "TicketApiDelegateImpl es @Service e implementa la interfaz",
                () -> {
                    Class<?> impl = clase(PKG + ".TicketApiDelegateImpl");
                    Class<?> api = clase(PKG + ".api.TicketsApiDelegate");
                    if (impl == null || api == null) return false;
                    boolean anotada = Arrays.stream(impl.getAnnotations())
                            .anyMatch(a -> a.annotationType().getSimpleName().equals("Service"));
                    return anotada && api.isAssignableFrom(impl);
                });

        verificar("Implementacion: reglas", "GET /api/tickets devuelve los 3 tickets iniciales",
                () -> {
                    Object r = invocar(nuevaImpl(), "listarTickets", new Class<?>[]{String.class}, new Object[]{null});
                    return status(r) == 200 && ((List<?>) cuerpo(r)).size() == 3;
                });

        verificar("Implementacion: reglas", "GET /api/tickets?estado=CERRADO filtra por estado (RN-05)",
                () -> {
                    Object r = invocar(nuevaImpl(), "listarTickets", new Class<?>[]{String.class}, new Object[]{"CERRADO"});
                    List<?> lista = (List<?>) cuerpo(r);
                    return status(r) == 200 && lista.size() == 1
                            && String.valueOf(leer(lista.get(0), "getTitulo")).contains("Lentitud");
                });

        verificar("Implementacion: reglas", "GET /api/tickets/1 devuelve 200 con el ticket correcto",
                () -> {
                    Object r = invocar(nuevaImpl(), "obtenerTicket", new Class<?>[]{Long.class}, new Object[]{1L});
                    Object t = cuerpo(r);
                    return status(r) == 200 && String.valueOf(leer(t, "getTitulo")).contains("reporte mensual")
                            && String.valueOf(leer(t, "getPrioridad")).equals("ALTA");
                });

        verificar("Implementacion: reglas", "POST crea el ticket con estado ABIERTO y 4 horas si es ALTA (RN-03, RN-04)",
                () -> {
                    Object impl = nuevaImpl();
                    Object req = request("Falla el envio de correos", "ALTA");
                    Object r = invocar(impl, "crearTicket", new Class<?>[]{clase(PKG + ".model.TicketRequest")}, new Object[]{req});
                    Object t = cuerpo(r);
                    return status(r) == 201 && String.valueOf(leer(t, "getEstado")).equals("ABIERTO")
                            && ((Number) leer(t, "getHorasEstimadas")).intValue() == 4
                            && ((Number) leer(t, "getId")).longValue() == 4L;
                });

        verificar("Implementacion: reglas", "Las horas dependen de la prioridad: MEDIA 8 y BAJA 24 (RN-04)",
                () -> {
                    Object impl = nuevaImpl();
                    Object m = cuerpo(invocar(impl, "crearTicket", new Class<?>[]{clase(PKG + ".model.TicketRequest")},
                            new Object[]{request("Ajuste de formato en la factura", "MEDIA")}));
                    Object b = cuerpo(invocar(impl, "crearTicket", new Class<?>[]{clase(PKG + ".model.TicketRequest")},
                            new Object[]{request("Cambiar el color del boton", "BAJA")}));
                    return ((Number) leer(m, "getHorasEstimadas")).intValue() == 8
                            && ((Number) leer(b, "getHorasEstimadas")).intValue() == 24;
                });

        // =================== IMPLEMENTACION: errores (4) ===================
        verificar("Implementacion: errores", "GET /api/tickets/99 devuelve 404 (RN-06)",
                () -> status(invocar(nuevaImpl(), "obtenerTicket", new Class<?>[]{Long.class}, new Object[]{99L})) == 404);

        verificar("Implementacion: errores", "POST con titulo demasiado corto devuelve 400 (RN-01)",
                () -> {
                    Object r = invocar(nuevaImpl(), "crearTicket", new Class<?>[]{clase(PKG + ".model.TicketRequest")},
                            new Object[]{request("Bug", "ALTA")});
                    return status(r) == 400;
                });

        verificar("Implementacion: errores", "POST sin prioridad devuelve 400 (RN-02)",
                () -> {
                    Object r = invocar(nuevaImpl(), "crearTicket", new Class<?>[]{clase(PKG + ".model.TicketRequest")},
                            new Object[]{request("Falla el envio de correos", null)});
                    return status(r) == 400;
                });

        verificar("Implementacion: errores", "El ticket creado queda registrado y se puede consultar",
                () -> {
                    Object impl = nuevaImpl();
                    invocar(impl, "crearTicket", new Class<?>[]{clase(PKG + ".model.TicketRequest")},
                            new Object[]{request("Falla el envio de correos", "ALTA")});
                    Object lista = cuerpo(invocar(impl, "listarTickets", new Class<?>[]{String.class}, new Object[]{null}));
                    Object uno = cuerpo(invocar(impl, "obtenerTicket", new Class<?>[]{Long.class}, new Object[]{4L}));
                    return ((List<?>) lista).size() == 4 && uno != null
                            && String.valueOf(leer(uno, "getTitulo")).contains("correos");
                });

        // =================== Resumen ===================
        System.out.println();
        System.out.println("Resumen por criterio");
        System.out.println("--------------------");
        int total = 0;
        for (Map.Entry<String, int[]> e : RESUMEN.entrySet()) {
            System.out.printf("%-28s %d / %d%n", e.getKey(), e.getValue()[0], e.getValue()[1]);
            total += e.getValue()[0];
        }
        System.out.println("--------------------");
        System.out.printf("PUNTAJE AUTOMATICO: %d / 20%n", total);
        if (total < 20) {
            System.out.println();
            System.out.println("Sugerencia: si muchas verificaciones dicen 'aun no generado', ejecuta");
            System.out.println("mvn clean install y luego Reload All Maven Projects en el panel Maven.");
        }
    }

    // ------------------------- utilidades -------------------------

    private static void verificar(String criterio, String descripcion, Prueba prueba) {
        numero++;
        boolean ok;
        String detalle = "";
        try {
            ok = prueba.ejecutar();
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            ok = false; detalle = "  (aun no generado o no implementado)";
        } catch (Throwable e) {
            ok = false; detalle = "  (" + e.getClass().getSimpleName() + ")";
        }
        int[] r = RESUMEN.computeIfAbsent(criterio, k -> new int[2]);
        r[1]++;
        if (ok) r[0]++;
        System.out.printf("%s %2d. %s%s%n", ok ? "[OK]" : "[X] ", numero, descripcion, detalle);
    }

    private static Class<?> clase(String nombre) {
        try {
            return Class.forName(nombre);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private static Method metodo(String nombre) {
        Class<?> api = clase(PKG + ".api.TicketsApiDelegate");
        if (api == null) return null;
        return Arrays.stream(api.getMethods()).filter(m -> m.getName().equals(nombre)).findFirst().orElse(null);
    }

    private static boolean tieneGetters(Class<?> c, String... getters) {
        List<String> nombres = Arrays.stream(c.getMethods()).map(Method::getName).toList();
        return nombres.containsAll(Arrays.asList(getters));
    }

    private static List<String> constantes(Class<?> modelo, String getter) throws Exception {
        if (modelo == null) return List.of();
        Class<?> tipo = modelo.getMethod(getter).getReturnType();
        if (!tipo.isEnum()) return List.of();
        List<String> valores = new ArrayList<>();
        for (Object o : tipo.getEnumConstants()) {
            valores.add(((Enum<?>) o).name());
        }
        return valores;
    }

    /** Contrato sin las lineas comentadas: los TODO no cuentan como contrato escrito. */
    private static String contrato() throws Exception {
        return Arrays.stream(Files.readString(Path.of("src/main/resources/api.yaml")).split("\n"))
                .filter(l -> !l.trim().startsWith("#"))
                .reduce("", (a, b) -> a + "\n" + b);
    }

    private static Object nuevaImpl() throws Exception {
        Class<?> impl = clase(PKG + ".TicketApiDelegateImpl");
        if (impl == null) throw new ClassNotFoundException(PKG + ".TicketApiDelegateImpl");
        return impl.getDeclaredConstructor().newInstance();
    }

    /** Construye un TicketRequest por reflexion; prioridad null deja el campo sin asignar. */
    private static Object request(String titulo, String prioridad) throws Exception {
        Class<?> c = clase(PKG + ".model.TicketRequest");
        if (c == null) throw new ClassNotFoundException(PKG + ".model.TicketRequest");
        Object req = c.getDeclaredConstructor().newInstance();
        c.getMethod("setTitulo", String.class).invoke(req, titulo);
        if (prioridad != null) {
            Method setter = Arrays.stream(c.getMethods()).filter(m -> m.getName().equals("setPrioridad"))
                    .findFirst().orElseThrow(() -> new NoSuchMethodException("setPrioridad"));
            Class<?> tipo = setter.getParameterTypes()[0];
            Object valor = tipo.isEnum() ? Enum.valueOf(tipo.asSubclass(Enum.class), prioridad) : prioridad;
            setter.invoke(req, valor);
        }
        return req;
    }

    private static Object invocar(Object destino, String metodo, Class<?>[] tipos, Object[] args) throws Exception {
        return destino.getClass().getMethod(metodo, tipos).invoke(destino, args);
    }

    private static int status(Object responseEntity) throws Exception {
        Object code = responseEntity.getClass().getMethod("getStatusCode").invoke(responseEntity);
        return (int) code.getClass().getMethod("value").invoke(code);
    }

    private static Object cuerpo(Object responseEntity) throws Exception {
        return responseEntity.getClass().getMethod("getBody").invoke(responseEntity);
    }

    private static Object leer(Object objeto, String getter) throws Exception {
        Object v = objeto.getClass().getMethod(getter).invoke(objeto);
        return (v != null && v.getClass().isEnum()) ? ((Enum<?>) v).name() : v;
    }
}
