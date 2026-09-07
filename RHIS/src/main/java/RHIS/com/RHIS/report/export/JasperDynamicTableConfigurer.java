package RHIS.com.RHIS.report.export;

import RHIS.com.RHIS.report.snapshot.ReportSnapshotMetadata;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.design.JRDesignBand;
import net.sf.jasperreports.engine.design.JRDesignExpression;
import net.sf.jasperreports.engine.design.JRDesignField;
import net.sf.jasperreports.engine.design.JRDesignSection;
import net.sf.jasperreports.engine.design.JRDesignStaticText;
import net.sf.jasperreports.engine.design.JRDesignTextField;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.type.PositionTypeEnum;
import net.sf.jasperreports.engine.type.HorizontalTextAlignEnum;
import net.sf.jasperreports.engine.type.SplitTypeEnum;
import net.sf.jasperreports.engine.type.StretchTypeEnum;
import net.sf.jasperreports.engine.type.TextAdjustEnum;
import net.sf.jasperreports.engine.type.WhenNoDataTypeEnum;

final class JasperDynamicTableConfigurer {

    private static final String HEADER_STYLE = "ColumnHeader";
    private static final String CELL_STYLE = "DataCell";
    private static final int HEADER_HEIGHT = 37;
    // Ten complete rows fit in the 474pt available after the header and footer.
    private static final int DETAIL_HEIGHT = 47;

    private JasperDynamicTableConfigurer() {
    }

    static void configure(JasperDesign design, ReportSnapshotMetadata metadata) throws JRException {
        design.setWhenNoDataType(WhenNoDataTypeEnum.ALL_SECTIONS_NO_DETAIL);
        int columnCount = Math.max(1, metadata.columns().size());
        int tableWidth = design.getColumnWidth();
        int baseWidth = tableWidth / columnCount;
        int remainder = tableWidth % columnCount;
        int x = 0;

        JRDesignBand columnHeader = new JRDesignBand();
        columnHeader.setHeight(HEADER_HEIGHT);
        JRDesignBand detail = new JRDesignBand();
        detail.setHeight(DETAIL_HEIGHT);
        detail.setSplitType(SplitTypeEnum.PREVENT);

        for (int index = 0; index < metadata.columns().size(); index++) {
            int width = baseWidth + (index < remainder ? 1 : 0);
            String fieldName = fieldName(index);

            JRDesignField field = new JRDesignField();
            field.setName(fieldName);
            field.setValueClass(String.class);
            design.addField(field);

            JRDesignStaticText header = new JRDesignStaticText(design);
            header.setX(x);
            header.setY(0);
            header.setWidth(width);
            header.setHeight(HEADER_HEIGHT);
            header.setStyleNameReference(HEADER_STYLE);
            header.setText(metadata.columns().get(index).displayName());
            HorizontalTextAlignEnum alignment = switch (metadata.columns().get(index).dataType()) {
                case INTEGER, DECIMAL, DATE, TIME, DATE_TIME, OFFSET_DATE_TIME -> HorizontalTextAlignEnum.CENTER;
                default -> HorizontalTextAlignEnum.LEFT;
            };
            header.setHorizontalTextAlign(alignment);
            columnHeader.addElement(header);

            JRDesignTextField cell = new JRDesignTextField(design);
            cell.setX(x);
            cell.setY(0);
            cell.setWidth(width);
            cell.setHeight(DETAIL_HEIGHT);
            cell.setStyleNameReference(CELL_STYLE);
            cell.setHorizontalTextAlign(alignment);
            cell.setBlankWhenNull(true);
            cell.setPositionType(PositionTypeEnum.FLOAT);
            cell.setStretchType(StretchTypeEnum.CONTAINER_HEIGHT);
            cell.setTextAdjust(TextAdjustEnum.SCALE_FONT);
            cell.setExpression(new JRDesignExpression("$F{" + fieldName + "}"));
            cell.getLineBox().setPadding(6);
            cell.getLineBox().setTopPadding(3);
            cell.getLineBox().setBottomPadding(3);
            detail.addElement(cell);

            x += width;
        }

        design.setColumnHeader(columnHeader);
        JRDesignSection detailSection = (JRDesignSection) design.getDetailSection();
        while (!detailSection.getBandsList().isEmpty()) {
            detailSection.removeBand(0);
        }
        detailSection.addBand(detail);
    }

    static String fieldName(int index) {
        return "column_" + index;
    }
}
