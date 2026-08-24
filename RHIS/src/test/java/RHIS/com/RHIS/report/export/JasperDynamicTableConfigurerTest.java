package RHIS.com.RHIS.report.export;

import RHIS.com.RHIS.dataset.model.DataSetFieldType;
import RHIS.com.RHIS.report.snapshot.ReportSnapshotMetadata;
import net.sf.jasperreports.engine.JRBand;
import net.sf.jasperreports.engine.design.JRDesignBand;
import net.sf.jasperreports.engine.design.JRDesignSection;
import net.sf.jasperreports.engine.design.JRDesignStaticText;
import net.sf.jasperreports.engine.design.JRDesignTextField;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.type.PositionTypeEnum;
import net.sf.jasperreports.engine.type.SplitTypeEnum;
import net.sf.jasperreports.engine.type.StretchTypeEnum;
import net.sf.jasperreports.engine.type.TextAdjustEnum;
import net.sf.jasperreports.engine.type.WhenNoDataTypeEnum;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JasperDynamicTableConfigurerTest {

    @Test
    void configuresEmptyMetadataWithHeaderAndDetailBands() throws Exception {
        JasperDesign design = designWithColumnWidth(794);

        JasperDynamicTableConfigurer.configure(design, new ReportSnapshotMetadata(List.of(), 0));

        assertThat(design.getWhenNoDataType()).isEqualTo(WhenNoDataTypeEnum.ALL_SECTIONS_NO_DETAIL);
        assertThat(design.getFieldsList()).isEmpty();
        assertThat(design.getColumnHeader().getHeight()).isEqualTo(24);
        assertThat(design.getColumnHeader().getElements()).isEmpty();
        JRDesignSection detail = (JRDesignSection) design.getDetailSection();
        assertThat(detail.getBandsList()).hasSize(1);
        assertThat(detail.getBandsList().get(0).getHeight()).isEqualTo(18);
        assertThat(detail.getBandsList().get(0).getElements()).isEmpty();
        assertThat(detail.getBandsList().get(0).getSplitType()).isEqualTo(SplitTypeEnum.STRETCH);
    }

    @Test
    void createsOrderedFieldsAndDistributesAllAvailableWidth() throws Exception {
        JasperDesign design = designWithColumnWidth(10);
        ReportSnapshotMetadata metadata = new ReportSnapshotMetadata(
                List.of(
                        column("field_1", "Premier"),
                        column("field_2", "DeuxiÃ¨me"),
                        column("field_3", "TroisiÃ¨me")
                ),
                0
        );

        JasperDynamicTableConfigurer.configure(design, metadata);

        assertThat(design.getFieldsList())
                .extracting(field -> field.getName())
                .containsExactly("column_0", "column_1", "column_2");
        assertThat(design.getFieldsList())
                .extracting(field -> field.getValueClass().getName())
                .containsOnly(String.class.getName());

        JRBand header = design.getColumnHeader();
        assertThat(header.getElements())
                .extracting(element -> ((JRDesignStaticText) element).getText())
                .containsExactly("Premier", "DeuxiÃ¨me", "TroisiÃ¨me");
        assertThat(header.getElements())
                .extracting(element -> element.getX())
                .containsExactly(0, 4, 7);
        List<Integer> widths = Arrays.stream(header.getElements()).map(element -> element.getWidth()).toList();
        assertThat(widths).containsExactly(4, 3, 3);
        assertThat(widths.stream().mapToInt(Integer::intValue).sum()).isEqualTo(10);

        JRBand detail = ((JRDesignSection) design.getDetailSection()).getBandsList().get(0);
        assertThat(detail.getElements())
                .extracting(element -> ((JRDesignTextField) element).getExpression().getText())
                .containsExactly("$F{column_0}", "$F{column_1}", "$F{column_2}");
    }

    @Test
    void appliesTheExistingTableStylesAndHeights() throws Exception {
        JasperDesign design = designWithColumnWidth(100);
        JRDesignSection detailSection = (JRDesignSection) design.getDetailSection();
        JRDesignBand existingDetail = new JRDesignBand();
        existingDetail.setHeight(11);
        JRDesignBand anotherExistingDetail = new JRDesignBand();
        anotherExistingDetail.setHeight(13);
        detailSection.addBand(existingDetail);
        detailSection.addBand(anotherExistingDetail);

        JasperDynamicTableConfigurer.configure(design, new ReportSnapshotMetadata(List.of(column("field_1", "Nom")), 0));

        JRDesignStaticText header = (JRDesignStaticText) design.getColumnHeader().getElements()[0];
        assertThat(detailSection.getBandsList()).hasSize(1);
        assertThat(detailSection.getBandsList().get(0))
                .isNotSameAs(existingDetail)
                .isNotSameAs(anotherExistingDetail);
        JRDesignTextField cell = (JRDesignTextField) detailSection.getBandsList().get(0)
                .getElements()[0];
        assertThat(header.getHeight()).isEqualTo(24);
        assertThat(header.getStyleNameReference()).isEqualTo("ColumnHeader");
        assertThat(cell.getHeight()).isEqualTo(18);
        assertThat(cell.getStyleNameReference()).isEqualTo("DataCell");
        assertThat(cell.isBlankWhenNull()).isTrue();
        assertThat(cell.getPositionType()).isEqualTo(PositionTypeEnum.FLOAT);
        assertThat(cell.getStretchType()).isEqualTo(StretchTypeEnum.CONTAINER_HEIGHT);
        assertThat(cell.getTextAdjust()).isEqualTo(TextAdjustEnum.STRETCH_HEIGHT);
        assertThat(cell.getLineBox().getPadding()).isEqualTo(2);
    }

    private JasperDesign designWithColumnWidth(int columnWidth) {
        JasperDesign design = new JasperDesign();
        design.setColumnWidth(columnWidth);
        return design;
    }

    private ReportSnapshotMetadata.Column column(String key, String displayName) {
        return new ReportSnapshotMetadata.Column(key, displayName, DataSetFieldType.TEXT);
    }
}
