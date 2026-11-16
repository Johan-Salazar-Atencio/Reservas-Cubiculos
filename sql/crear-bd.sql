-- Solo para producción (PostgreSQL). En desarrollo se usa H2 en memoria y no hace falta nada.
-- La base lleva el nombre del proyecto para no chocar con las de otros proyectos.
CREATE DATABASE bd_reservas_cubiculos;

-- Las tablas las crea la propia aplicación al arrancar (spring.jpa.hibernate.ddl-auto=update):
-- usuarios, cubiculos, reservas, reserva_integrantes y registro_admin.
-- La lista de estudiantes (tabla usuarios, rol ESTUDIANTE) se carga aparte con los códigos y correos reales.
