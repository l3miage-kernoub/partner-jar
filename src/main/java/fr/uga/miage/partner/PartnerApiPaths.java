package fr.uga.miage.partner;

public final class PartnerApiPaths {

    public static final String BASE = "/api/partner";
    public static final String PRODUCTS = BASE + "/products";
    public static final String ORDERS = BASE + "/orders";
    public static final String ORDER = ORDERS + "/{id}";
    public static final String ORDER_CANCEL = ORDER + "/cancel";
    public static final String CONTRACT_RESOURCE = "/contrat-partenaires.yaml";

    private PartnerApiPaths() {
    }
}
