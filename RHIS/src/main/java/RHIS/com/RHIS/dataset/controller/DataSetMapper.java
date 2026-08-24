package RHIS.com.RHIS.dataset.controller;

import RHIS.com.RHIS.dataset.controller.dto.DataSetFieldResponse;
import RHIS.com.RHIS.dataset.controller.dto.DataSetResponse;
import RHIS.com.RHIS.dataset.entity.DataSetEntity;
import RHIS.com.RHIS.dataset.entity.DataSetField;
import org.springframework.stereotype.Component;

@Component
public class DataSetMapper {

    public DataSetResponse entityToResponse(DataSetEntity dataSetEntity) {

              return new DataSetResponse(
                        dataSetEntity.getId(),
                        dataSetEntity.getDisplayName(),
                        dataSetEntity.getSourceName()
                      );

    }
    public DataSetFieldResponse toFieldResponse (DataSetField dataSetField) {
        return new DataSetFieldResponse(
                dataSetField.getId(),
                dataSetField.getDisplayName(),
                dataSetField.getSourceName(),
                dataSetField.getDataType(),
                dataSetField.isNullable(),
                dataSetField.getDataType().isSupported(),
                dataSetField.getDataType().supportedOperators()
        );
    }
}
