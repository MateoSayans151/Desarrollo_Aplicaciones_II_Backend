package ar.edu.universidad.gestionacademica.Entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Entity
@Table(name = "horarios_curso")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HorarioCurso {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "curso_id", nullable = false)
    private Curso curso;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "aula_id", nullable = false)
    private Aula aula;

    @NotNull @Enumerated(EnumType.STRING) @Column(nullable = false)
    private DayOfWeek diaSemana;

    @NotNull @Column(nullable = false)
    private LocalTime horaInicio;

    @NotNull @Column(nullable = false)
    private LocalTime horaFin;
}
