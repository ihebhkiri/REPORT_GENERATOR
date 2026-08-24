package RHIS.com.RHIS.dataset.controller.dto;

public record JoinInfoProjection(
        String tableSource,
        String ColonneSource,
        String TableCible,
        String ColonneCible,
        String TypeRelation

) {
}
