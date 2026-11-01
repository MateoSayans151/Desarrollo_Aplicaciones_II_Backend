package ar.edu.universidad.gestionacademica.BusinessLogic;

import ar.edu.universidad.gestionacademica.Entidades.*;
import ar.edu.universidad.gestionacademica.Excepciones.RecursoNoEncontradoException;
import ar.edu.universidad.gestionacademica.Repositorios.*;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlanEstudioPdfServicio {
    private static final float MARGEN = 50f;
    private static final float ALTO_PAGINA = PDRectangle.A4.getHeight();
    private static final float INTERLINEA = 16f;
    private static final float ANCHO_UTIL = PDRectangle.A4.getWidth() - 2 * MARGEN;

    private final PlanEstudioRepositorio planes;
    private final AsignaturaRepositorio asignaturas;
    private final CorrelatividadRepositorio correlatividades;

    @Transactional(readOnly = true)
    public byte[] generar(Long planId) {
        PlanEstudio plan = planes.findById(planId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Plan no encontrado"));
        List<Asignatura> materias = asignaturas.findByPlanIdOrderByNombreAsc(planId);

        try (PDDocument documento = new PDDocument()) {
            PDType1Font fuenteTitulo = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fuenteTexto = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font fuenteDetalle = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

            EscritorPdf escritor = new EscritorPdf(documento);
            escritor.linea(fuenteTitulo, 16, "Plan de estudios");
            escritor.linea(fuenteTexto, 12, plan.getCarrera().getNombre() + " (" + plan.getCarrera().getCodigo() + ")");
            escritor.linea(fuenteTexto, 12, plan.getCodigo() + " - " + plan.getNombre());
            escritor.linea(fuenteTexto, 12, "Vigente desde " + plan.getVigenciaDesde());
            escritor.espacio();

            if (materias.isEmpty()) {
                escritor.linea(fuenteTexto, 11, "El plan no tiene asignaturas cargadas.");
            }
            for (Asignatura materia : materias) {
                escritor.linea(fuenteTitulo, 12, materia.getAnio() + " - "
                        + materia.getCodigo() + " - " + materia.getNombre()
                        + " (" + materia.getCargaHoraria() + " hs)");
                List<Correlatividad> correlativasMateria = correlatividades.findByAsignaturaId(materia.getId());
                if (correlativasMateria.isEmpty()) {
                    escritor.linea(fuenteDetalle, 10, "   Sin correlatividades");
                } else {
                    for (Correlatividad correlatividad : correlativasMateria) {
                        Asignatura requerida = correlatividad.getCorrelativa();
                        escritor.linea(fuenteDetalle, 10, "   " + correlatividad.getTipo() + ": "
                                + requerida.getCodigo() + " - " + requerida.getNombre());
                    }
                }
            }
            escritor.cerrar();

            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            documento.save(salida);
            return salida.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo generar el PDF del plan de estudios", ex);
        }
    }

    private static final class EscritorPdf {
        private final PDDocument documento;
        private PDPageContentStream contenido;
        private float y;

        EscritorPdf(PDDocument documento) throws IOException {
            this.documento = documento;
            nuevaPagina();
        }

        void linea(PDType1Font fuente, float tamanio, String texto) throws IOException {
            for (String renglon : renglones(fuente, tamanio, imprimible(fuente, texto))) {
                if (y < MARGEN + INTERLINEA) nuevaPagina();
                contenido.beginText();
                contenido.setFont(fuente, tamanio);
                contenido.newLineAtOffset(MARGEN, y);
                contenido.showText(renglon);
                contenido.endText();
                y -= INTERLINEA;
            }
        }

        /** Las fuentes estándar sólo codifican WinAnsi: emojis u otros alfabetos harían fallar el PDF. */
        private String imprimible(PDType1Font fuente, String texto) throws IOException {
            StringBuilder resultado = new StringBuilder();
            for (int indice = 0; indice < texto.length(); ) {
                int caracter = texto.codePointAt(indice);
                indice += Character.charCount(caracter);
                String actual = Character.isWhitespace(caracter) ? " " : Character.toString(caracter);
                try {
                    fuente.encode(actual);
                    resultado.append(actual);
                } catch (IllegalArgumentException ex) {
                    resultado.append('?');
                }
            }
            return resultado.toString();
        }

        private List<String> renglones(PDType1Font fuente, float tamanio, String texto) throws IOException {
            List<String> resultado = new ArrayList<>();
            StringBuilder renglon = new StringBuilder();
            for (char caracter : texto.toCharArray()) {
                renglon.append(caracter);
                if (fuente.getStringWidth(renglon.toString()) / 1000 * tamanio > ANCHO_UTIL) {
                    int corte = Math.max(renglon.lastIndexOf(" "), 1);
                    resultado.add(renglon.substring(0, corte));
                    renglon.delete(0, corte);
                }
            }
            resultado.add(renglon.toString());
            return resultado;
        }

        void espacio() {
            y -= INTERLINEA / 2;
        }

        private void nuevaPagina() throws IOException {
            if (contenido != null) contenido.close();
            PDPage pagina = new PDPage(PDRectangle.A4);
            documento.addPage(pagina);
            contenido = new PDPageContentStream(documento, pagina);
            y = ALTO_PAGINA - MARGEN;
        }

        void cerrar() throws IOException {
            if (contenido != null) contenido.close();
        }
    }
}
