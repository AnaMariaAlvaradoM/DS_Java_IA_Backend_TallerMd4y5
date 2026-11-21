package com.devsenior.sillon.cita;

public enum EstadoCita {
    PENDIENTE {
        @Override
        public boolean puedeTransicionarA(EstadoCita destino) {
            return destino == CONFIRMADA || destino == CANCELADA;
        }

        @Override
        public boolean ocupaAgenda() {
            return true;
        }
    },
    CONFIRMADA {
        @Override
        public boolean puedeTransicionarA(EstadoCita destino) {
            return destino == COMPLETADA || destino == CANCELADA || destino == NO_ASISTIO;
        }

        @Override
        public boolean ocupaAgenda() {
            return true;
        }
    },
    COMPLETADA {
        @Override
        public boolean puedeTransicionarA(EstadoCita destino) {
            return false;
        }

        @Override
        public boolean ocupaAgenda() {
            return false;
        }
    },
    CANCELADA {
        @Override
        public boolean puedeTransicionarA(EstadoCita destino) {
            return false;
        }

        @Override
        public boolean ocupaAgenda() {
            return false;
        }
    },
    NO_ASISTIO {
        @Override
        public boolean puedeTransicionarA(EstadoCita destino) {
            return false;
        }

        @Override
        public boolean ocupaAgenda() {
            return false;
        }
    };

    public abstract boolean puedeTransicionarA(EstadoCita destino);
    public abstract boolean ocupaAgenda();
}
