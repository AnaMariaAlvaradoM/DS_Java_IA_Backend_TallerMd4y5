package co.sillon;

record Actor(long id, long salon, String rol, String email) {
    boolean dueno() { return "DUENO".equals(rol); }
}
