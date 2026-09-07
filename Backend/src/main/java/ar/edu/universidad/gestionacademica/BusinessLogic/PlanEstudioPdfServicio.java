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
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlanEstudioPdfServicio {
    private static final float MARGEN = 50f;
    private static final float ALTO_PAGINA = PDRectangle.A4.getHeight();
    private static final float INTERLINEA = 16f;

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
            if (y < MARGEN + INTERLINEA) nuevaPagina();
            contenido.beginText();
            contenido.setFont(fuente, tamanio);
            contenido.newLineAtOffset(MARGEN, y);
            contenido.showText(texto);
            contenido.endText();
            y -= INTERLINEA;
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
